package com.mediadock.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExtractedImageItemDto(
    val id: String,
    val url: String,
    val title: String? = null,
    val alt: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val format: String? = null,
    val source: String = "img"
)

@Serializable
data class ExtractImagesResponseDto(
    val url: String,
    val siteTitle: String,
    val totalFound: Int,
    val images: List<ExtractedImageItemDto> = emptyList()
)

@Serializable
data class DownloadImageRequestDto(
    val url: String,
    val title: String? = null,
    val format: String? = null
)
