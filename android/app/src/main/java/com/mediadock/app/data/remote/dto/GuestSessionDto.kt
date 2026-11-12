package com.mediadock.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GuestSessionResponseDto(
    val sessionId: String,
    val token: String,
    val createdAt: Long,
    val expiresAt: Long,
    val quota: Int,
    val quotaRemaining: Int
)
