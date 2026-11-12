package com.mediadock.app.domain.usecase

import com.mediadock.app.core.common.Resource
import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.data.remote.dto.CreateDownloadRequestDto
import com.mediadock.app.domain.model.ActiveDownload
import com.mediadock.app.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CreateDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(
        request: CreateDownloadRequestDto,
        title: String,
        thumbnail: String?
    ): Resource<String> {
        return repository.enqueueDownload(request, title, thumbnail)
    }
}

class CreateBatchDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(
        urls: List<String>,
        kind: String,
        quality: String?,
        format: String?
    ): Resource<List<String>> {
        return repository.enqueueBatch(urls, kind, quality, format)
    }
}

class DownloadImageUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(
        url: String,
        title: String?,
        format: String?
    ): Resource<String> {
        return repository.enqueueImageDownload(url, title, format)
    }
}

class GetActiveDownloadsUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Flow<List<ActiveDownload>> = repository.getActiveDownloads()
    fun getCount(): Flow<Int> = repository.getActiveDownloadsCount()
    fun getDownload(jobId: String): Flow<ActiveDownload?> = repository.getDownload(jobId)
}

class ManageDownloadUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    suspend fun pause(jobId: String) = repository.pauseDownload(jobId)
    suspend fun resume(jobId: String) = repository.resumeDownload(jobId)
    suspend fun cancel(jobId: String) = repository.cancelDownload(jobId)
    suspend fun delete(jobId: String) = repository.deleteDownload(jobId)
}

class GetHistoryUseCase @Inject constructor(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Flow<List<HistoryEntity>> = repository.getHistory()
    fun search(query: String): Flow<List<HistoryEntity>> = repository.searchHistory(query)
    suspend fun delete(id: Long) = repository.deleteHistory(id)
    suspend fun clearAll() = repository.clearHistory()
}
