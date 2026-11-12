package com.mediadock.app.domain.model

data class SupportedPlatform(
    val id: String,
    val name: String,
    val domains: List<String>,
    val supportedTypes: List<String>, // "Video", "Audio", "Images", "GIF"
    val description: String,
    val maxQuality: String
)

object PlatformRegistry {
    val platforms = listOf(
        SupportedPlatform(
            id = "youtube",
            name = "YouTube",
            domains = listOf("youtube.com", "youtu.be"),
            supportedTypes = listOf("Video", "Audio", "GIF", "Subtitles"),
            description = "Videos, Shorts, Playlists, Channels, Audio extraction",
            maxQuality = "4K Ultra HD (2160p)"
        ),
        SupportedPlatform(
            id = "instagram",
            name = "Instagram",
            domains = listOf("instagram.com"),
            supportedTypes = listOf("Video", "Audio", "Images"),
            description = "Reels, Posts, IGTV, Carousels",
            maxQuality = "1080p Full HD"
        ),
        SupportedPlatform(
            id = "tiktok",
            name = "TikTok",
            domains = listOf("tiktok.com", "vm.tiktok.com", "vt.tiktok.com"),
            supportedTypes = listOf("Video", "Audio"),
            description = "Watermark-free video, Original sounds",
            maxQuality = "1080p Full HD"
        ),
        SupportedPlatform(
            id = "twitter",
            name = "X / Twitter",
            domains = listOf("twitter.com", "x.com"),
            supportedTypes = listOf("Video", "GIF", "Images"),
            description = "Public video clips, High-res images, Animated GIFs",
            maxQuality = "1080p Full HD"
        ),
        SupportedPlatform(
            id = "facebook",
            name = "Facebook",
            domains = listOf("facebook.com", "fb.watch"),
            supportedTypes = listOf("Video", "Audio"),
            description = "Public videos, Reels, Watch clips",
            maxQuality = "1080p Full HD"
        ),
        SupportedPlatform(
            id = "reddit",
            name = "Reddit",
            domains = listOf("reddit.com", "redd.it"),
            supportedTypes = listOf("Video", "Audio", "Images", "GIF"),
            description = "Posts, native v.redd.it videos with merged audio",
            maxQuality = "1080p Full HD"
        ),
        SupportedPlatform(
            id = "pinterest",
            name = "Pinterest",
            domains = listOf("pinterest.com", "pin.it"),
            supportedTypes = listOf("Video", "Images"),
            description = "Idea pins, High-resolution original images",
            maxQuality = "1080p Full HD"
        ),
        SupportedPlatform(
            id = "soundcloud",
            name = "SoundCloud",
            domains = listOf("soundcloud.com"),
            supportedTypes = listOf("Audio"),
            description = "Music tracks, Playlists, Artwork embedding",
            maxQuality = "320 kbps MP3"
        ),
        SupportedPlatform(
            id = "vimeo",
            name = "Vimeo",
            domains = listOf("vimeo.com"),
            supportedTypes = listOf("Video", "Audio"),
            description = "Cinematic videos, High-bitrate streams",
            maxQuality = "4K Ultra HD"
        ),
        SupportedPlatform(
            id = "twitch",
            name = "Twitch",
            domains = listOf("twitch.tv"),
            supportedTypes = listOf("Video", "Audio"),
            description = "Clips, Highlights, VODs",
            maxQuality = "1080p60"
        ),
        SupportedPlatform(
            id = "bluesky",
            name = "Bluesky",
            domains = listOf("bsky.app"),
            supportedTypes = listOf("Video", "Images"),
            description = "Post videos and images",
            maxQuality = "1080p Full HD"
        ),
        SupportedPlatform(
            id = "threads",
            name = "Threads",
            domains = listOf("threads.net"),
            supportedTypes = listOf("Video", "Images"),
            description = "Post videos and carousels",
            maxQuality = "1080p Full HD"
        )
    )

    fun matchPlatform(url: String): SupportedPlatform? {
        val lower = url.lowercase()
        return platforms.find { platform ->
            platform.domains.any { domain -> lower.contains(domain) }
        }
    }
}
