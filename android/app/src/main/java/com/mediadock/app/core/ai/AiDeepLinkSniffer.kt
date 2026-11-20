package com.mediadock.app.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deep Network Sniffer that runs non-blocking async probes on URLs to unshorten links,
 * detect exact MIME types, extract true content lengths, verify resumable range support,
 * and fetch OpenGraph / HTML metadata.
 */
@Singleton
class AiDeepLinkSniffer @Inject constructor() {

    private val probeClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    private val BROWSER_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

    /**
     * Performs a deep async network inspection on the URL, enriching the fast heuristic intel.
     */
    suspend fun sniffDeep(url: String): AiLinkIntel = withContext(Dispatchers.IO) {
        val fastIntel = AiLinkBrain.analyzeFast(url)
        try {
            val request = Request.Builder()
                .url(fastIntel.canonicalUrl)
                .header("User-Agent", BROWSER_USER_AGENT)
                .header("Range", "bytes=0-16384") // Request first 16KB to probe headers & meta
                .build()

            probeClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    return@withContext fastIntel
                }

                val finalUrl = response.request.url.toString()
                val contentType = response.header("Content-Type")?.lowercase() ?: ""
                val acceptRanges = response.header("Accept-Ranges")?.lowercase() == "bytes" || response.code == 206
                val contentDisposition = response.header("Content-Disposition").orEmpty()
                val contentLength = extractContentLength(response)

                // Try to infer filename from Content-Disposition
                val serverFilename = extractFilenameFromDisposition(contentDisposition)

                // Detect category & format from Content-Type if present
                val (refinedCategory, refinedFormat) = inferFromContentType(contentType, fastIntel)

                // Title enrichment: either from server filename or from HTML title / OpenGraph
                val enrichedTitle = when {
                    serverFilename.isNotBlank() -> serverFilename
                    contentType.contains("text/html") -> {
                        val htmlPrefix = response.body?.string().orEmpty()
                        extractHtmlTitle(htmlPrefix).ifBlank { fastIntel.cleanTitle }
                    }
                    else -> fastIntel.cleanTitle
                }

                val isDirect = refinedCategory == AiMediaCategory.DOCUMENT ||
                        refinedCategory == AiMediaCategory.VIDEO ||
                        refinedCategory == AiMediaCategory.AUDIO ||
                        refinedCategory == AiMediaCategory.IMAGE ||
                        fastIntel.isDirectDownload

                val sizeStr = if (contentLength != null && contentLength > 0) formatBytes(contentLength) else null
                val badgeText = "⚡ AI: ${refinedFormat.uppercase()} • ${sizeStr ?: refinedCategory.label} • ${if (acceptRanges) "Resumable" else "Direct"}"

                fastIntel.copy(
                    canonicalUrl = finalUrl,
                    cleanTitle = enrichedTitle,
                    category = refinedCategory,
                    suggestedFormat = refinedFormat,
                    isDirectDownload = isDirect,
                    isResumable = acceptRanges,
                    estimatedBytes = contentLength,
                    confidence = 0.99f,
                    summaryBadge = badgeText
                )
            }
        } catch (_: Exception) {
            fastIntel
        }
    }

    private fun extractContentLength(response: Response): Long? {
        val contentRange = response.header("Content-Range")
        if (!contentRange.isNullOrBlank()) {
            val total = contentRange.substringAfterLast('/', "").toLongOrNull()
            if (total != null && total > 0) return total
        }
        val cl = response.header("Content-Length")?.toLongOrNull()
        return if (cl != null && cl > 0) cl else null
    }

    private fun extractFilenameFromDisposition(disposition: String): String {
        if (disposition.isBlank()) return ""
        val match = Regex("""filename\*?=['"]?(?:UTF-8'')?([^"';]+)['"]?""", RegexOption.IGNORE_CASE).find(disposition)
        val raw = match?.groupValues?.get(1)?.trim() ?: ""
        return try {
            URLDecoder.decode(raw, StandardCharsets.UTF_8.toString()).substringBeforeLast('.')
        } catch (_: Exception) {
            raw.substringBeforeLast('.')
        }
    }

    private fun inferFromContentType(contentType: String, fallback: AiLinkIntel): Pair<AiMediaCategory, String> {
        return when {
            contentType.contains("application/pdf") -> Pair(AiMediaCategory.DOCUMENT, "pdf")
            contentType.contains("application/epub") -> Pair(AiMediaCategory.DOCUMENT, "epub")
            contentType.contains("video/mp4") -> Pair(AiMediaCategory.VIDEO, "mp4")
            contentType.contains("video/webm") -> Pair(AiMediaCategory.VIDEO, "webm")
            contentType.contains("video/") -> Pair(AiMediaCategory.VIDEO, "mp4")
            contentType.contains("audio/mpeg") || contentType.contains("audio/mp3") -> Pair(AiMediaCategory.AUDIO, "mp3")
            contentType.contains("audio/mp4") || contentType.contains("audio/m4a") -> Pair(AiMediaCategory.AUDIO, "m4a")
            contentType.contains("audio/flac") -> Pair(AiMediaCategory.AUDIO, "flac")
            contentType.contains("audio/wav") -> Pair(AiMediaCategory.AUDIO, "wav")
            contentType.contains("audio/") -> Pair(AiMediaCategory.AUDIO, "mp3")
            contentType.contains("image/jpeg") -> Pair(AiMediaCategory.IMAGE, "jpg")
            contentType.contains("image/png") -> Pair(AiMediaCategory.IMAGE, "png")
            contentType.contains("image/webp") -> Pair(AiMediaCategory.IMAGE, "webp")
            contentType.contains("image/gif") -> Pair(AiMediaCategory.IMAGE, "gif")
            contentType.contains("image/") -> Pair(AiMediaCategory.IMAGE, "jpg")
            contentType.contains("application/vnd.apple.mpegurl") || contentType.contains("application/x-mpegurl") ->
                Pair(AiMediaCategory.STREAM, "m3u8")
            else -> Pair(fallback.category, fallback.suggestedFormat)
        }
    }

    private fun extractHtmlTitle(html: String): String {
        // Try OpenGraph title first: <meta property="og:title" content="...">
        val ogMatch = Regex("""<meta[^>]+property=["']og:title["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html)
            ?: Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:title["']""", RegexOption.IGNORE_CASE).find(html)
        val ogTitle = ogMatch?.groupValues?.get(1)?.trim()
        if (!ogTitle.isNullOrBlank()) return cleanTitle(ogTitle)

        // Fallback to <title>...</title>
        val titleMatch = Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE).find(html)
        val title = titleMatch?.groupValues?.get(1)?.trim()
        return if (!title.isNullOrBlank()) cleanTitle(title) else ""
    }

    private fun cleanTitle(raw: String): String {
        return raw.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }
}
