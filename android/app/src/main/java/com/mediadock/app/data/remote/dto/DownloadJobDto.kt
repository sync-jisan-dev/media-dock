package com.mediadock.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateDownloadRequestDto(
    val url: String,
    val kind: String, // "video" | "audio"
    val quality: String? = null,
    val format: String? = null,
    val embedThumbnail: Boolean? = null,
    val embedMetadata: Boolean? = null,
    val embedChapters: Boolean? = null,
    val embedSubtitles: Boolean? = null,
    val writeSubtitles: Boolean? = null,
    val writeThumbnail: Boolean? = null,
    val videoCodec: String? = null,
    val subtitleLang: String? = null,
    val clipStart: Long? = null,
    val clipEnd: Long? = null
)

@Serializable
data class CreateBatchRequestDto(
    val urls: List<String>,
    val kind: String,
    val quality: String? = null,
    val format: String? = null,
    val embedThumbnail: Boolean? = null
)

@Serializable
data class DownloadJobResponseDto(
    val id: String,
    val platform: String,
    val url: String,
    val kind: String,
    val status: String,
    val title: String? = null,
    val thumbnail: String? = null,
    val quality: String? = null,
    val format: String? = null,
    val filename: String? = null,
    val fileUrl: String? = null,
    val fileSizeBytes: Long? = null,
    val error: String? = null,
    val errorCode: String? = null,
    val progress: Float = 0f,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String? = null,
    val expiresAt: String? = null
)
