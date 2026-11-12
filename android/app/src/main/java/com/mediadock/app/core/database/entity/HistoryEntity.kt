package com.mediadock.app.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val jobId: String,
    val sourceUrl: String,
    val title: String,
    val mediaType: String,
    val format: String,
    val localUri: String,
    val fileSizeBytes: Long = 0L,
    val downloadedAt: Long = System.currentTimeMillis()
)
