package com.mediadock.app.data.repository

import com.mediadock.app.core.common.Resource
import com.mediadock.app.core.database.dao.DownloadDao
import com.mediadock.app.core.database.dao.HistoryDao
import com.mediadock.app.core.database.entity.DownloadEntity
import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.core.datastore.PreferencesDataStore
import com.mediadock.app.core.downloads.DownloadTransferEngine
import com.mediadock.app.core.network.MediaDockApi
import com.mediadock.app.core.network.SseEventManager
import com.mediadock.app.data.remote.dto.CreateBatchRequestDto
import com.mediadock.app.data.remote.dto.CreateDownloadRequestDto
import com.mediadock.app.data.remote.dto.DownloadImageRequestDto
import com.mediadock.app.domain.model.ActiveDownload
import com.mediadock.app.domain.model.DownloadJobStatus
import com.mediadock.app.domain.repository.DownloadRepository
import com.mediadock.app.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val api: MediaDockApi,
    private val downloadDao: DownloadDao,
    private val historyDao: HistoryDao,
    private val mediaRepository: MediaRepository,
    private val sseEventManager: SseEventManager,
    private val transferEngine: DownloadTransferEngine,
    private val preferencesDataStore: PreferencesDataStore
) : DownloadRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())

    private fun mapToDomain(entity: DownloadEntity): ActiveDownload {
        val status = try {
            DownloadJobStatus.valueOf(entity.status)
        } catch (_: Exception) {
            DownloadJobStatus.QUEUED
        }
        return ActiveDownload(
            jobId = entity.jobId,
            sourceUrl = entity.sourceUrl,
            title = entity.mediaTitle,
            mediaType = entity.mediaType,
            format = entity.selectedFormat,
            quality = entity.selectedQuality,
            thumbnail = entity.thumbnailUrl,
            localUri = entity.localUri,
            fileSizeBytes = entity.fileSizeBytes,
            downloadedBytes = entity.downloadedBytes,
            progress = entity.progress,
            speed = entity.downloadSpeed,
            eta = entity.etaSeconds,
            status = status,
            error = entity.errorMessage,
            createdAt = entity.createdAt
        )
    }

    override suspend fun enqueueDownload(
        request: CreateDownloadRequestDto,
        title: String,
        thumbnail: String?
    ): Resource<String> = withContext(Dispatchers.IO) {
        mediaRepository.ensureSession()
        try {
            val response = api.createDownload(request)
            if (response.isSuccessful && response.body() != null) {
                val job = response.body()!!
                val entity = DownloadEntity(
                    jobId = job.id,
                    sourceUrl = request.url,
                    mediaTitle = title.ifBlank { job.title ?: "Media Download" },
                    mediaType = request.kind,
                    selectedFormat = request.format ?: "mp4",
                    selectedQuality = request.quality ?: "highest",
                    thumbnailUrl = thumbnail ?: job.thumbnail,
                    status = "QUEUED",
                    createdAt = System.currentTimeMillis()
                )
                downloadDao.insertOrUpdate(entity)

                // Launch SSE tracking in background
                trackJobViaSse(job.id, entity.mediaTitle, request.kind, entity.selectedFormat)

                Resource.Success(job.id)
            } else {
                val err = response.errorBody()?.string() ?: "Failed to queue download: HTTP ${response.code()}"
                Resource.Error(err)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to start download", e)
        }
    }

    override suspend fun enqueueBatch(
        urls: List<String>,
        kind: String,
        quality: String?,
        format: String?
    ): Resource<List<String>> = withContext(Dispatchers.IO) {
        mediaRepository.ensureSession()
        try {
            val batchDto = CreateBatchRequestDto(
                urls = urls,
                kind = kind,
                quality = quality,
                format = format
            )
            val response = api.createBatch(batchDto)
            if (response.isSuccessful && response.body() != null) {
                val jobs = response.body()!!
                val ids = mutableListOf<String>()
                for (job in jobs) {
                    val entity = DownloadEntity(
                        jobId = job.id,
                        sourceUrl = job.url,
                        mediaTitle = job.title ?: "Batch item",
                        mediaType = kind,
                        selectedFormat = format ?: "mp4",
                        selectedQuality = quality ?: "highest",
                        thumbnailUrl = job.thumbnail,
                        status = "QUEUED",
                        createdAt = System.currentTimeMillis()
                    )
                    downloadDao.insertOrUpdate(entity)
                    trackJobViaSse(job.id, entity.mediaTitle, kind, entity.selectedFormat)
                    ids.add(job.id)
                }
                Resource.Success(ids)
            } else {
                val err = response.errorBody()?.string() ?: "Batch request failed: HTTP ${response.code()}"
                Resource.Error(err)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Network error during batch request", e)
        }
    }

    override suspend fun enqueueImageDownload(
        url: String,
        title: String?,
        format: String?
    ): Resource<String> = withContext(Dispatchers.IO) {
        mediaRepository.ensureSession()
        try {
            val response = api.downloadImage(DownloadImageRequestDto(url, title, format))
            if (response.isSuccessful && response.body() != null) {
                val job = response.body()!!
                val safeTitle = title ?: job.title ?: "Image Download"
                val fmt = format ?: job.format ?: "jpg"

                val entity = DownloadEntity(
                    jobId = job.id,
                    sourceUrl = url,
                    mediaTitle = safeTitle,
                    mediaType = "image",
                    selectedFormat = fmt,
                    selectedQuality = "original",
                    thumbnailUrl = url,
                    fileSizeBytes = job.fileSizeBytes ?: 0L,
                    status = "DOWNLOADING",
                    remoteFileUrl = job.fileUrl,
                    createdAt = System.currentTimeMillis()
                )
                downloadDao.insertOrUpdate(entity)

                // Start immediate streaming to storage
                job.fileUrl?.let { fileUrl ->
                    transferEngine.startFileTransfer(
                        jobId = job.id,
                        fileUrl = fileUrl,
                        title = safeTitle,
                        mediaType = "image",
                        format = fmt,
                        totalBytes = job.fileSizeBytes ?: 0L
                    )
                }

                Resource.Success(job.id)
            } else {
                val err = response.errorBody()?.string() ?: "Image download failed"
                Resource.Error(err)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to download image", e)
        }
    }

    private fun trackJobViaSse(jobId: String, title: String, mediaType: String, format: String) {
        repositoryScope.launch {
            try {
                val prefs = preferencesDataStore.preferencesFlow.first()
                val eventsUrl = prefs.serverUrl.removeSuffix("/") + "/app/v1/downloads/$jobId/events"

                downloadDao.updateStatus(jobId, "PROCESSING")

                sseEventManager.streamJobEvents(eventsUrl).collect { event ->
                    when (event.type) {
                        "progress" -> {
                            event.progress?.let { p ->
                                downloadDao.updateProgress(
                                    jobId = jobId,
                                    progress = p,
                                    speed = event.speed ?: 0L,
                                    eta = event.eta ?: 0L,
                                    downloadedBytes = 0L
                                )
                            }
                        }
                        "complete" -> {
                            val fileUrl = event.job?.fileUrl
                            val sizeBytes = event.job?.fileSizeBytes ?: 0L
                            if (!fileUrl.isNullOrBlank()) {
                                downloadDao.updateFileUrl(jobId, fileUrl)
                                transferEngine.startFileTransfer(
                                    jobId = jobId,
                                    fileUrl = fileUrl,
                                    title = title,
                                    mediaType = mediaType,
                                    format = format,
                                    totalBytes = sizeBytes
                                )
                            }
                        }
                        "error" -> {
                            downloadDao.updateStatus(jobId, "FAILED", error = event.message ?: "Processing error")
                        }
                    }
                }
            } catch (e: Exception) {
                // If SSE is interrupted, poll once as fallback
                try {
                    val poll = api.getDownload(jobId)
                    if (poll.isSuccessful && poll.body() != null) {
                        val job = poll.body()!!
                        if (job.status == "COMPLETED" && !job.fileUrl.isNullOrBlank()) {
                            downloadDao.updateFileUrl(jobId, job.fileUrl)
                            transferEngine.startFileTransfer(
                                jobId = jobId,
                                fileUrl = job.fileUrl,
                                title = title,
                                mediaType = mediaType,
                                format = format,
                                totalBytes = job.fileSizeBytes ?: 0L
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun getActiveDownloads(): Flow<List<ActiveDownload>> =
        downloadDao.getActiveDownloads().map { list -> list.map { mapToDomain(it) } }

    override fun getDownload(jobId: String): Flow<ActiveDownload?> =
        downloadDao.getDownload(jobId).map { it?.let { mapToDomain(it) } }

    override fun getActiveDownloadsCount(): Flow<Int> =
        downloadDao.getActiveDownloadsCount()

    override suspend fun pauseDownload(jobId: String) = withContext(Dispatchers.IO) {
        transferEngine.pauseTransfer(jobId)
        downloadDao.updateStatus(jobId, "PAUSED")
    }

    override suspend fun resumeDownload(jobId: String) = withContext(Dispatchers.IO) {
        val download = downloadDao.getDownloadSync(jobId)
        if (download != null && !download.remoteFileUrl.isNullOrBlank()) {
            transferEngine.startFileTransfer(
                jobId = download.jobId,
                fileUrl = download.remoteFileUrl,
                title = download.mediaTitle,
                mediaType = download.mediaType,
                format = download.selectedFormat,
                totalBytes = download.fileSizeBytes
            )
        }
    }

    override suspend fun cancelDownload(jobId: String) = withContext(Dispatchers.IO) {
        transferEngine.cancelTransfer(jobId)
        try {
            api.cancelDownload(jobId)
        } catch (_: Exception) {}
        downloadDao.updateStatus(jobId, "CANCELLED")
    }

    override suspend fun deleteDownload(jobId: String) = withContext(Dispatchers.IO) {
        transferEngine.cancelTransfer(jobId)
        downloadDao.delete(jobId)
    }

    override fun getHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    override fun searchHistory(query: String): Flow<List<HistoryEntity>> = historyDao.searchHistory(query)
    override suspend fun deleteHistory(id: Long) = historyDao.delete(id)
    override suspend fun clearHistory() = historyDao.clearAll()
}
