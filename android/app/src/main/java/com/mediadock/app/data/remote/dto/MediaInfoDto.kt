package com.mediadock.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InspectMediaRequestDto(
    val url: String
)

@Serializable
data class MediaQualitiesDto(
    val video: List<String> = emptyList(),
    val audio: List<String> = emptyList()
)

@Serializable
data class MediaFormatsDto(
    val video: List<String> = emptyList(),
    val audio: List<String> = emptyList()
)

@Serializable
data class SubtitleTrackDto(
    val lang: String,
    val name: String,
    val auto: Boolean = false
)

@Serializable
data class MediaInfoResponseDto(
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
    val likeCount: Long? = null,
    val commentCount: Long? = null,
    val uploadedAt: String? = null,
    val isLive: Boolean = false,
    val qualities: MediaQualitiesDto? = null,
    val formats: MediaFormatsDto? = null,
    val subtitles: List<SubtitleTrackDto> = emptyList()
)
