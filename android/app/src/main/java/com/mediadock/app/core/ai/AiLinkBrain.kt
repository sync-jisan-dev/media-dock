package com.mediadock.app.core.ai

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Superfast on-device AI Brain for link detection, categorization, and metadata inference.
 * Operates entirely in memory with sub-millisecond latency (<1ms), making link detection instantaneous.
 */
object AiLinkBrain {

    private val TRACKING_PARAMS = setOf(
        "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
        "fbclid", "igshid", "si", "ref", "ref_src", "s", "t", "feature", "app",
        "gclid", "_ga", "mc_cid", "mc_eid"
    )

    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "avi", "mov", "flv", "ts", "m4v", "3gp", "ogv")
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "flac", "wav", "aac", "ogg", "opus", "wma", "alac")
    private val DOCUMENT_EXTENSIONS = setOf("pdf", "epub", "docx", "xlsx", "pptx", "txt")
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "svg", "bmp", "avif", "heic")
    private val STREAM_EXTENSIONS = setOf("m3u8", "mpd", "ism")

    private val URL_REGEX = """(https?://[^\s<>"'{}|\\^`]+)""".toRegex(RegexOption.IGNORE_CASE)

    /**
     * Extracts all valid HTTP/HTTPS URLs from any messy text block or multi-line clipboard paste.
     */
    fun extractAllUrls(rawText: String): List<String> {
        if (rawText.isBlank()) return emptyList()
        return URL_REGEX.findAll(rawText)
            .map { it.value.trim().trimEnd('.', ',', ';', ')', ']', '>', '}') }
            .filter { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
            .distinct()
            .take(100)
            .toList()
    }

    /**
     * Strips promotional and tracking parameters to produce clean canonical URLs.
     */
    fun canonicalize(url: String): String {
        return try {
            val uri = URI(url.trim())
            val query = uri.rawQuery ?: return url.trim()
            val cleanQuery = query.split('&')
                .map { it.split('=', limit = 2) }
                .filterNot { it.firstOrNull()?.lowercase() in TRACKING_PARAMS }
                .joinToString("&") { if (it.size > 1) "${it[0]}=${it[1]}" else it[0] }

            val portStr = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
            val queryStr = if (cleanQuery.isNotBlank()) "?$cleanQuery" else ""
            val fragStr = if (!uri.rawFragment.isNullOrBlank()) "#${uri.rawFragment}" else ""

            "${uri.scheme}://${uri.host}$portStr${uri.rawPath ?: ""}$queryStr$fragStr"
        } catch (_: Exception) {
            url.trim()
        }
    }

    /**
     * Instant on-device heuristic analysis of any media or document URL.
     */
    fun analyzeFast(rawInput: String): AiLinkIntel {
        val canonical = canonicalize(rawInput)
        val lower = canonical.lowercase()
        val extension = extractExtension(canonical)

        // 1. Check Platform
        val platform = detectPlatform(lower)

        // 2. Determine Category & Format
        val (category, format, quality) = when {
            extension in DOCUMENT_EXTENSIONS -> Triple(AiMediaCategory.DOCUMENT, extension, "Document")
            extension in VIDEO_EXTENSIONS -> Triple(AiMediaCategory.VIDEO, extension, "1080p HD")
            extension in AUDIO_EXTENSIONS -> Triple(AiMediaCategory.AUDIO, extension, "320 kbps HQ")
            extension in IMAGE_EXTENSIONS -> Triple(AiMediaCategory.IMAGE, extension, "Original")
            extension in STREAM_EXTENSIONS -> Triple(AiMediaCategory.STREAM, extension, "Auto Stream")
            platform == AiPlatform.YOUTUBE -> Triple(AiMediaCategory.VIDEO, "mp4", "1080p / 4K")
            platform == AiPlatform.INSTAGRAM -> Triple(AiMediaCategory.VIDEO, "mp4", "Original HD")
            platform == AiPlatform.TIKTOK -> Triple(AiMediaCategory.VIDEO, "mp4", "No Watermark")
            platform == AiPlatform.TWITTER_X -> Triple(AiMediaCategory.VIDEO, "mp4", "Original")
            platform == AiPlatform.FACEBOOK -> Triple(AiMediaCategory.VIDEO, "mp4", "HD")
            platform == AiPlatform.SOUNDCLOUD -> Triple(AiMediaCategory.AUDIO, "mp3", "320 kbps")
            platform == AiPlatform.REDDIT -> Triple(AiMediaCategory.VIDEO, "mp4", "Original")
            platform == AiPlatform.PINTEREST -> Triple(AiMediaCategory.IMAGE, "jpg", "High Res")
            platform == AiPlatform.GOOGLE_DRIVE || platform == AiPlatform.DROPBOX ->
                Triple(AiMediaCategory.DOCUMENT, extension.ifBlank { "file" }, "Cloud File")
            platform == AiPlatform.DIRECT_STORAGE -> Triple(AiMediaCategory.VIDEO, extension.ifBlank { "mp4" }, "Direct File")
            else -> Triple(AiMediaCategory.GENERIC_WEB, extension.ifBlank { "mp4" }, "Web Stream")
        }

        // 3. Generate Clean Title
        val cleanTitle = inferCleanTitle(canonical, platform, category, format)

        // 4. Determine if direct download
        val isDirect = extension in VIDEO_EXTENSIONS ||
                extension in AUDIO_EXTENSIONS ||
                extension in DOCUMENT_EXTENSIONS ||
                extension in IMAGE_EXTENSIONS ||
                platform == AiPlatform.DIRECT_STORAGE

        // 5. Build AI Badge & Summary
        val summaryBadge = buildSummaryBadge(category, platform, format, quality, isDirect)
        val detailsNote = buildDetailsNote(category, platform, isDirect)

        val tags = mutableListOf<String>()
        tags.add(platform.displayName)
        tags.add(format.uppercase())
        if (isDirect) tags.add("Direct Download")
        if (category == AiMediaCategory.DOCUMENT) tags.add("PDF & Docs")

        return AiLinkIntel(
            originalUrl = rawInput.trim(),
            canonicalUrl = canonical,
            cleanTitle = cleanTitle,
            category = category,
            platform = platform,
            suggestedFormat = format,
            suggestedQuality = quality,
            isDirectDownload = isDirect,
            isResumable = isDirect || platform == AiPlatform.DIRECT_STORAGE,
            estimatedBytes = null,
            confidence = if (isDirect || platform != AiPlatform.GENERIC_WEB) 0.98f else 0.75f,
            summaryBadge = summaryBadge,
            detailsNote = detailsNote,
            tags = tags
        )
    }

    private fun detectPlatform(lowerUrl: String): AiPlatform {
        return when {
            lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be") -> AiPlatform.YOUTUBE
            lowerUrl.contains("instagram.com") || lowerUrl.contains("instagr.am") -> AiPlatform.INSTAGRAM
            lowerUrl.contains("tiktok.com") || lowerUrl.contains("douyin.com") -> AiPlatform.TIKTOK
            lowerUrl.contains("twitter.com") || lowerUrl.contains("x.com") || lowerUrl.contains("t.co") -> AiPlatform.TWITTER_X
            lowerUrl.contains("facebook.com") || lowerUrl.contains("fb.watch") || lowerUrl.contains("fb.com") -> AiPlatform.FACEBOOK
            lowerUrl.contains("reddit.com") || lowerUrl.contains("v.redd.it") || lowerUrl.contains("redd.it") -> AiPlatform.REDDIT
            lowerUrl.contains("pinterest.com") || lowerUrl.contains("pin.it") -> AiPlatform.PINTEREST
            lowerUrl.contains("soundcloud.com") -> AiPlatform.SOUNDCLOUD
            lowerUrl.contains("twitch.tv") -> AiPlatform.TWITCH
            lowerUrl.contains("vimeo.com") -> AiPlatform.VIMEO
            lowerUrl.contains("drive.google.com") -> AiPlatform.GOOGLE_DRIVE
            lowerUrl.contains("dropbox.com") -> AiPlatform.DROPBOX
            lowerUrl.contains("commondatastorage.googleapis.com") ||
                    lowerUrl.contains("storage.googleapis.com") ||
                    lowerUrl.contains("amazonaws.com") ||
                    lowerUrl.contains("r2.cloudflarestorage.com") -> AiPlatform.DIRECT_STORAGE
            else -> AiPlatform.GENERIC_WEB
        }
    }

    private fun extractExtension(url: String): String {
        return try {
            val uri = URI(url)
            val path = uri.path ?: ""
            val lastSlash = path.lastIndexOf('/')
            val filename = if (lastSlash != -1) path.substring(lastSlash + 1) else path
            val dot = filename.lastIndexOf('.')
            if (dot != -1 && dot < filename.length - 1) {
                filename.substring(dot + 1).lowercase().filter { it.isLetterOrDigit() }
            } else ""
        } catch (_: Exception) {
            val dot = url.lastIndexOf('.')
            if (dot != -1 && dot < url.length - 1) {
                url.substring(dot + 1).take(5).lowercase().filter { it.isLetterOrDigit() }
            } else ""
        }
    }

    private fun inferCleanTitle(url: String, platform: AiPlatform, category: AiMediaCategory, format: String): String {
        return try {
            val uri = URI(url)
            val path = uri.path ?: ""
            val lastSegment = path.split('/').lastOrNull { it.isNotBlank() } ?: ""
            val decoded = URLDecoder.decode(lastSegment, StandardCharsets.UTF_8.toString())

            // Strip extension
            val withoutExt = if (decoded.contains('.')) decoded.substringBeforeLast('.') else decoded

            // Clean underscores, hyphens, and numeric hashes
            var cleaned = withoutExt
                .replace('_', ' ')
                .replace('-', ' ')
                .replace(Regex("""\s+"""), " ")
                .trim()

            // If it's a long random hash (e.g. "a9f82bc7190e381b"), use platform name
            if (cleaned.length > 20 && cleaned.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                return "${platform.displayName} $format".trim()
            }

            if (cleaned.length < 3) {
                return when (category) {
                    AiMediaCategory.DOCUMENT -> "Document ($format)"
                    AiMediaCategory.AUDIO -> "${platform.displayName} Audio"
                    AiMediaCategory.IMAGE -> "${platform.displayName} Image"
                    else -> "${platform.displayName} Media"
                }
            }

            // Capitalize words
            cleaned.split(' ').joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
        } catch (_: Exception) {
            "${platform.displayName} Download"
        }
    }

    private fun buildSummaryBadge(
        category: AiMediaCategory,
        platform: AiPlatform,
        format: String,
        quality: String,
        isDirect: Boolean
    ): String {
        val prefix = "⚡ AI:"
        return when {
            category == AiMediaCategory.DOCUMENT -> "$prefix ${format.uppercase()} Document • Ready"
            isDirect -> "$prefix Direct ${format.uppercase()} • Resumable"
            platform == AiPlatform.YOUTUBE -> "$prefix YouTube Video • $quality"
            platform == AiPlatform.TIKTOK -> "$prefix TikTok • No Watermark"
            platform == AiPlatform.INSTAGRAM -> "$prefix Instagram • Original HD"
            platform == AiPlatform.TWITTER_X -> "$prefix X / Twitter Media"
            category == AiMediaCategory.AUDIO -> "$prefix Audio Track • ${format.uppercase()}"
            category == AiMediaCategory.IMAGE -> "$prefix Photo • High Resolution"
            else -> "$prefix ${category.label} • Ready"
        }
    }

    private fun buildDetailsNote(
        category: AiMediaCategory,
        platform: AiPlatform,
        isDirect: Boolean
    ): String {
        return when {
            category == AiMediaCategory.DOCUMENT -> "Direct document detected. MediaDock will download and save it to your Documents folder."
            isDirect -> "Direct media stream detected. Resumable high-speed downloading enabled."
            platform == AiPlatform.YOUTUBE -> "Full 1080p/4K video and high-bitrate audio extraction supported."
            platform == AiPlatform.TIKTOK -> "Clean HD stream without platform watermark supported."
            platform == AiPlatform.INSTAGRAM -> "Reels, carousels, and stories supported with maximum original fidelity."
            else -> "MediaDock AI Core is ready to inspect, stream, and download this content."
        }
    }
}
