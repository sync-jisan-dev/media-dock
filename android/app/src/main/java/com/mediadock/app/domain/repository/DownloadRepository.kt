package com.mediadock.app.domain.repository

import com.mediadock.app.core.common.Resource
import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.data.remote.dto.CreateDownloadRequestDto
import com.mediadock.app.domain.model.ActiveDownload
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    suspend fun enqueueDownload(
        request: CreateDownloadRequestDto,
        title: String,
        thumbnail: String?
    ): Resource<String>

    suspend fun enqueueBatch(
        urls: List<String>,
        kind: String,
        quality: String?,
        format: String?
    ): Resource<List<String>>

    suspend fun enqueueImageDownload(
        url: String,
        title: String?,
        format: String?
    ): Resource<String>

    fun getActiveDownloads(): Flow<List<ActiveDownload>>
    fun getDownload(jobId: String): Flow<ActiveDownload?>
    fun getActiveDownloadsCount(): Flow<Int>

    suspend fun pauseDownload(jobId: String)
    suspend fun resumeDownload(jobId: String)
    suspend fun cancelDownload(jobId: String)
    suspend fun deleteDownload(jobId: String)

    fun getHistory(): Flow<List<HistoryEntity>>
    fun searchHistory(query: String): Flow<List<HistoryEntity>>
    suspend fun deleteHistory(id: Long)
    suspend fun clearHistory()
}
