package com.mediadock.app.domain.model

data class MediaDetails(
    val id: String,
    val platform: String,
    val url: String,
    val title: String,
    val description: String? = null,
    val thumbnail: String? = null,
    val durationSeconds: Long? = null,
    val uploader: String? = null,
    val uploaderUrl: String? = null,
    val viewCount: Long? = null,
    val videoQualities: List<String> = emptyList(),
    val audioQualities: List<String> = emptyList(),
    val videoFormats: List<String> = emptyList(),
    val audioFormats: List<String> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList()
)

data class SubtitleTrack(
    val lang: String,
    val name: String,
    val auto: Boolean = false
)

data class ExtractedImage(
    val id: String,
    val url: String,
    val title: String? = null,
    val alt: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val format: String = "jpg",
    val source: String = "img"
)

enum class DownloadJobStatus {
    PENDING,
    QUEUED,
    PROCESSING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class ActiveDownload(
    val jobId: String,
    val sourceUrl: String,
    val title: String,
    val mediaType: String,
    val format: String,
    val quality: String,
    val thumbnail: String?,
    val localUri: String?,
    val fileSizeBytes: Long,
    val downloadedBytes: Long,
    val progress: Float,
    val speed: Long,
    val eta: Long,
    val status: DownloadJobStatus,
    val error: String?,
    val createdAt: Long
)
