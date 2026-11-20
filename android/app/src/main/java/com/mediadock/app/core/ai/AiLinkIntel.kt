package com.mediadock.app.core.ai

enum class AiMediaCategory(val label: String, val badgeColorHex: Long) {
    VIDEO("Video", 0xFF3B82F6),
    AUDIO("Audio", 0xFF10B981),
    DOCUMENT("Document / PDF", 0xFFF59E0B),
    IMAGE("Image / Gallery", 0xFF8B5CF6),
    STREAM("Live / HLS Stream", 0xFFEC4899),
    SOCIAL_PLATFORM("Platform Media", 0xFFFE5223),
    GENERIC_WEB("Webpage", 0xFF6B7280)
}

enum class AiPlatform(val displayName: String, val brandTag: String) {
    YOUTUBE("YouTube", "youtube"),
    INSTAGRAM("Instagram", "instagram"),
    TIKTOK("TikTok", "tiktok"),
    TWITTER_X("X / Twitter", "twitter"),
    FACEBOOK("Facebook", "facebook"),
    REDDIT("Reddit", "reddit"),
    PINTEREST("Pinterest", "pinterest"),
    SOUNDCLOUD("SoundCloud", "soundcloud"),
    TWITCH("Twitch", "twitch"),
    VIMEO("Vimeo", "vimeo"),
    GOOGLE_DRIVE("Google Drive", "gdrive"),
    DROPBOX("Dropbox", "dropbox"),
    DIRECT_STORAGE("Direct Storage", "storage"),
    GENERIC_WEB("Web Page", "web")
}

data class AiLinkIntel(
    val originalUrl: String,
    val canonicalUrl: String,
    val cleanTitle: String,
    val category: AiMediaCategory,
    val platform: AiPlatform,
    val suggestedFormat: String,
    val suggestedQuality: String,
    val isDirectDownload: Boolean,
    val isResumable: Boolean,
    val estimatedBytes: Long? = null,
    val confidence: Float = 1.0f,
    val summaryBadge: String = "",
    val detailsNote: String = "",
    val tags: List<String> = emptyList()
)

data class AiCrawledMediaItem(
    val url: String,
    val title: String,
    val category: AiMediaCategory,
    val format: String,
    val estimatedSize: Long? = null,
    val previewUrl: String? = null
)

data class AiPageCrawlResult(
    val pageUrl: String,
    val pageTitle: String,
    val videos: List<AiCrawledMediaItem> = emptyList(),
    val audios: List<AiCrawledMediaItem> = emptyList(),
    val documents: List<AiCrawledMediaItem> = emptyList(),
    val images: List<AiCrawledMediaItem> = emptyList()
) {
    val totalCount: Int get() = videos.size + audios.size + documents.size + images.size
}

data class AiAgentStats(
    val linksAnalyzed: Int = 0,
    val directDetected: Int = 0,
    val selfHealedCount: Int = 0,
    val duplicatesPrevented: Int = 0,
    val agentActive: Boolean = true
)
