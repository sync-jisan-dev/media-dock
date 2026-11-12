package com.mediadock.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val jobId: String,
    val sourceUrl: String,
    val mediaTitle: String,
    val mediaType: String, // "video" | "audio" | "image" | "gif"
    val selectedFormat: String,
    val selectedQuality: String,
    val thumbnailUrl: String? = null,
    val localUri: String? = null,
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val progress: Float = 0f,
    val downloadSpeed: Long = 0L,
    val etaSeconds: Long = 0L,
    val status: String = "QUEUED",
    val errorMessage: String? = null,
    val remoteFileUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
