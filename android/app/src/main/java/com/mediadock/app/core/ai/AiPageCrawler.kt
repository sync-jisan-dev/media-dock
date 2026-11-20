package com.mediadock.app.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Webpage Crawler that extracts all embedded media, direct streams, PDF documents,
 * and high-resolution images from any visited webpage.
 */
@Singleton
class AiPageCrawler @Inject constructor() {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val BROWSER_USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"

    suspend fun crawlPage(pageUrl: String): AiPageCrawlResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(pageUrl)
                .header("User-Agent", BROWSER_USER_AGENT)
                .build()

            val html = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext AiPageCrawlResult(pageUrl, "Unavailable")
                response.body?.string().orEmpty()
            }

            val baseUri = try { URI(pageUrl) } catch (_: Exception) { null }
            val pageTitle = extractTitle(html).ifBlank { baseUri?.host ?: "Webpage Media" }

            val videos = mutableListOf<AiCrawledMediaItem>()
            val audios = mutableListOf<AiCrawledMediaItem>()
            val documents = mutableListOf<AiCrawledMediaItem>()
            val images = mutableListOf<AiCrawledMediaItem>()

            // 1. Scan for Video tags & sources
            val videoRegex = Regex("""<(?:video|source)[^>]+src=["']([^"']+\.(?:mp4|webm|mkv|mov|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE)
            videoRegex.findAll(html).forEach { match ->
                val rawSrc = match.groupValues[1]
                val absUrl = resolveUrl(baseUri, rawSrc) ?: return@forEach
                val intel = AiLinkBrain.analyzeFast(absUrl)
                videos.add(
                    AiCrawledMediaItem(
                        url = absUrl,
                        title = intel.cleanTitle,
                        category = intel.category,
                        format = intel.suggestedFormat
                    )
                )
            }

            // 2. Scan for Audio tags & sources
            val audioRegex = Regex("""<(?:audio|source)[^>]+src=["']([^"']+\.(?:mp3|m4a|wav|ogg|flac|opus)[^"']*)["']""", RegexOption.IGNORE_CASE)
            audioRegex.findAll(html).forEach { match ->
                val rawSrc = match.groupValues[1]
                val absUrl = resolveUrl(baseUri, rawSrc) ?: return@forEach
                val intel = AiLinkBrain.analyzeFast(absUrl)
                audios.add(
                    AiCrawledMediaItem(
                        url = absUrl,
                        title = intel.cleanTitle,
                        category = AiMediaCategory.AUDIO,
                        format = intel.suggestedFormat
                    )
                )
            }

            // 3. Scan for Documents & PDFs in <a> links
            val docRegex = Regex("""<a[^>]+href=["']([^"']+\.(?:pdf|epub|docx|xlsx)[^"']*)["'][^>]*>(.*?)</a>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            docRegex.findAll(html).forEach { match ->
                val rawHref = match.groupValues[1]
                val linkText = cleanText(match.groupValues[2])
                val absUrl = resolveUrl(baseUri, rawHref) ?: return@forEach
                val intel = AiLinkBrain.analyzeFast(absUrl)
                val title = linkText.takeIf { it.isNotBlank() && it.length > 2 } ?: intel.cleanTitle
                documents.add(
                    AiCrawledMediaItem(
                        url = absUrl,
                        title = title,
                        category = AiMediaCategory.DOCUMENT,
                        format = intel.suggestedFormat
                    )
                )
            }

            // 4. Scan for Images: <img> src, data-src, and OpenGraph og:image
            val imgRegex = Regex("""<img[^>]+(?:src|data-src)=["']([^"']+\.(?:jpg|jpeg|png|webp|avif)[^"']*)["'][^>]*(?:alt=["']([^"']*)["'])?""", RegexOption.IGNORE_CASE)
            imgRegex.findAll(html).forEach { match ->
                val rawSrc = match.groupValues[1]
                val alt = match.groupValues[2].trim()
                val absUrl = resolveUrl(baseUri, rawSrc) ?: return@forEach
                // Filter out tiny icons or tracking pixels
                if (!absUrl.contains("favicon", ignoreCase = true) && !absUrl.contains("1x1", ignoreCase = true)) {
                    val intel = AiLinkBrain.analyzeFast(absUrl)
                    images.add(
                        AiCrawledMediaItem(
                            url = absUrl,
                            title = alt.ifBlank { intel.cleanTitle },
                            category = AiMediaCategory.IMAGE,
                            format = intel.suggestedFormat,
                            previewUrl = absUrl
                        )
                    )
                }
            }

            // OpenGraph Video fallback
            val ogVideoRegex = Regex("""<meta[^>]+property=["']og:video(?::secure_url)?["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            ogVideoRegex.findAll(html).forEach { match ->
                val absUrl = resolveUrl(baseUri, match.groupValues[1]) ?: return@forEach
                val intel = AiLinkBrain.analyzeFast(absUrl)
                videos.add(
                    AiCrawledMediaItem(
                        url = absUrl,
                        title = pageTitle,
                        category = AiMediaCategory.VIDEO,
                        format = intel.suggestedFormat
                    )
                )
            }

            AiPageCrawlResult(
                pageUrl = pageUrl,
                pageTitle = pageTitle,
                videos = videos.distinctBy { it.url }.take(25),
                audios = audios.distinctBy { it.url }.take(25),
                documents = documents.distinctBy { it.url }.take(25),
                images = images.distinctBy { it.url }.take(50)
            )
        } catch (_: Exception) {
            AiPageCrawlResult(pageUrl, "Crawl Completed")
        }
    }

    private fun resolveUrl(baseUri: URI?, relative: String): String? {
        if (baseUri == null) return if (relative.startsWith("http")) relative else null
        return try {
            baseUri.resolve(relative.trim()).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTitle(html: String): String {
        val titleMatch = Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE).find(html)
        return titleMatch?.groupValues?.get(1)?.trim()?.replace("&amp;", "&") ?: ""
    }

    private fun cleanText(htmlSnippet: String): String {
        return htmlSnippet.replace(Regex("""<[^>]*>"""), "").trim()
    }
}
