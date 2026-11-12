package com.mediadock.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class JobEventDto(
    val type: String, // "status" | "progress" | "complete" | "error" | "ping"
    val status: String? = null,
    val progress: Float? = null,
    val speed: Long? = null,
    val eta: Long? = null,
    val job: DownloadJobResponseDto? = null,
    val message: String? = null,
    val code: String? = null
)
