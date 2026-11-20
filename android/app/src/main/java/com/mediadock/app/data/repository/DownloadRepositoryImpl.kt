package com.mediadock.app.data.repository

import android.content.Context
import android.net.Uri
import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.common.Resource
import com.mediadock.app.core.database.dao.DownloadDao
import com.mediadock.app.core.database.dao.HistoryDao
import com.mediadock.app.core.database.entity.DownloadEntity
import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.core.datastore.PreferencesDataStore
import com.mediadock.app.core.datastore.WorkspaceRules
import com.mediadock.app.core.storage.StorageManager
import com.mediadock.app.feature.browser.BrowserRules
import com.mediadock.app.core.downloads.DownloadTransferEngine
import com.mediadock.app.core.downloads.ForegroundTransferService
import com.mediadock.app.core.network.MediaDockApi
import com.mediadock.app.core.network.SseEventManager
import com.mediadock.app.data.remote.dto.CreateBatchRequestDto
import com.mediadock.app.data.remote.dto.CreateDownloadRequestDto
import com.mediadock.app.data.remote.dto.DownloadImageRequestDto
import com.mediadock.app.domain.model.ActiveDownload
import com.mediadock.app.domain.model.DownloadJobStatus
import com.mediadock.app.domain.repository.DownloadRepository
import com.mediadock.app.domain.repository.MediaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MediaDockApi,
    private val downloadDao: DownloadDao,
    private val historyDao: HistoryDao,
    private val mediaRepository: MediaRepository,
    private val sseEventManager: SseEventManager,
    private val transferEngine: DownloadTransferEngine,
    private val preferencesDataStore: PreferencesDataStore
) : DownloadRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())
    private val storageManager: StorageManager by lazy { StorageManager(context) }

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

    private fun isDirectMediaUrl(url: String): Boolean {
        val clean = url.trim().lowercase().substringBefore('?').substringBefore('#')
        return clean.endsWith(".mp4") || clean.endsWith(".webm") || clean.endsWith(".m4a") ||
               clean.endsWith(".mp3") || clean.endsWith(".wav") || clean.endsWith(".mov") ||
               clean.endsWith(".mkv") || clean.endsWith(".flv") || clean.endsWith(".aac") ||
               clean.endsWith(".ogg") || clean.endsWith(".opus") || clean.endsWith(".m4v") ||
               clean.endsWith(".pdf") || clean.endsWith(".doc") || clean.endsWith(".docx") ||
               clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".png") ||
               clean.endsWith(".gif") || clean.endsWith(".webp")
    }

    override suspend fun enqueueDownload(
        request: CreateDownloadRequestDto,
        title: String,
        thumbnail: String?
    ): Resource<String> = withContext(Dispatchers.IO) {
        val localJobId = "job_" + System.currentTimeMillis() + "_" + (1000..9999).random()
        val cleanUrl = request.url.trim().lowercase().substringBefore('?').substringBefore('#')
        val safeTitle = title.ifBlank {
            BrowserRules.displayHost(request.url).ifBlank {
                if (cleanUrl.endsWith(".pdf")) "Document Download.pdf" else "Media Download"
            }
        }
        val kind = when {
            request.kind.isNotBlank() && request.kind != "video" -> request.kind
            cleanUrl.endsWith(".pdf") || cleanUrl.endsWith(".doc") || cleanUrl.endsWith(".docx") -> "document"
            cleanUrl.endsWith(".jpg") || cleanUrl.endsWith(".jpeg") || cleanUrl.endsWith(".png") ||
            cleanUrl.endsWith(".gif") || cleanUrl.endsWith(".webp") -> "image"
            cleanUrl.endsWith(".mp3") || cleanUrl.endsWith(".m4a") || cleanUrl.endsWith(".wav") ||
            cleanUrl.endsWith(".aac") || cleanUrl.endsWith(".ogg") || cleanUrl.endsWith(".opus") -> "audio"
            else -> request.kind.ifBlank { "video" }
        }
        val format = when {
            cleanUrl.endsWith(".pdf") -> "pdf"
            cleanUrl.endsWith(".doc") -> "doc"
            cleanUrl.endsWith(".docx") -> "docx"
            cleanUrl.endsWith(".jpg") || cleanUrl.endsWith(".jpeg") -> "jpg"
            cleanUrl.endsWith(".png") -> "png"
            cleanUrl.endsWith(".gif") -> "gif"
            cleanUrl.endsWith(".webp") -> "webp"
            else -> request.format ?: "mp4"
        }
        val quality = request.quality ?: "highest"

        val initialSpeed = 2_800_000L
        val initialBytes = 2_450_000L
        val estimatedTotalBytes = 24_500_000L
        val initialProgress = 10f
        val initialEta = 18L

        // Immediately persist the active download entity so it shows in the Queue right away with active progress
        val entity = DownloadEntity(
            jobId = localJobId,
            sourceUrl = request.url,
            mediaTitle = safeTitle,
            mediaType = kind,
            selectedFormat = format,
            selectedQuality = quality,
            thumbnailUrl = thumbnail,
            status = "PROCESSING",
            progress = initialProgress,
            downloadSpeed = initialSpeed,
            etaSeconds = initialEta,
            fileSizeBytes = estimatedTotalBytes,
            downloadedBytes = initialBytes,
            createdAt = System.currentTimeMillis()
        )
        downloadDao.insertOrUpdate(entity)

        // Immediately show download notification in Android system notification bar
        ForegroundTransferService.startService(context, localJobId, safeTitle)
        ForegroundTransferService.updateProgress(
            context = context,
            jobId = localJobId,
            title = safeTitle,
            progress = initialProgress.toInt(),
            statusText = "Downloading",
            speed = FormattingUtils.formatSpeed(initialSpeed)
        )

        // Launch background orchestration with simulated progressive feedback
        repositoryScope.launch {
            runDownloadPipeline(localJobId, request, safeTitle, thumbnail, kind, format, quality)
        }

        Resource.Success(localJobId)
    }

    private suspend fun runDownloadPipeline(
        localJobId: String,
        request: CreateDownloadRequestDto,
        initialTitle: String,
        initialThumbnail: String?,
        kind: String,
        format: String,
        quality: String
    ) {
        var currentTitle = initialTitle
        var currentThumbnail = initialThumbnail
        var simulatedProgress = 10f
        var simulatedSpeed = 2_800_000L
        val estimatedTotal = 24_500_000L
        var simulatedBytes = 2_450_000L
        var transferStarted = false
        var isCancelledOrDone = false

        // 1. Direct media URL shortcut (e.g., .mp4, .webm, .mp3, .pdf, .jpg links from CDNs)
        if (isDirectMediaUrl(request.url)) {
            val cleanUrl = request.url.trim().lowercase().substringBefore('?').substringBefore('#')
            val directKind = when {
                cleanUrl.endsWith(".pdf") || cleanUrl.endsWith(".doc") || cleanUrl.endsWith(".docx") -> "document"
                cleanUrl.endsWith(".jpg") || cleanUrl.endsWith(".jpeg") || cleanUrl.endsWith(".png") ||
                cleanUrl.endsWith(".gif") || cleanUrl.endsWith(".webp") -> "image"
                cleanUrl.endsWith(".mp3") || cleanUrl.endsWith(".m4a") || cleanUrl.endsWith(".wav") ||
                cleanUrl.endsWith(".aac") || cleanUrl.endsWith(".ogg") || cleanUrl.endsWith(".opus") -> "audio"
                else -> kind
            }
            val directFormat = when {
                cleanUrl.endsWith(".pdf") -> "pdf"
                cleanUrl.endsWith(".doc") -> "doc"
                cleanUrl.endsWith(".docx") -> "docx"
                cleanUrl.endsWith(".jpg") || cleanUrl.endsWith(".jpeg") -> "jpg"
                cleanUrl.endsWith(".png") -> "png"
                cleanUrl.endsWith(".gif") -> "gif"
                cleanUrl.endsWith(".webp") -> "webp"
                else -> format
            }
            downloadDao.updateStatus(localJobId, "DOWNLOADING")
            transferEngine.startFileTransfer(
                jobId = localJobId,
                fileUrl = request.url,
                title = currentTitle,
                mediaType = directKind,
                format = directFormat,
                totalBytes = 0L
            )
            return
        }

        // 2. Progressive feedback ticker while the server wakes up and extracts media streams
        val tickerJob = repositoryScope.launch {
            while (simulatedProgress < 94f && !transferStarted && !isCancelledOrDone) {
                delay(600)
                val dbEntity = downloadDao.getDownloadSync(localJobId)
                if (dbEntity == null || dbEntity.status in listOf("PAUSED", "CANCELLED", "COMPLETED")) {
                    isCancelledOrDone = true
                    break
                }
                if (dbEntity.status == "DOWNLOADING" && dbEntity.downloadedBytes > simulatedBytes && dbEntity.downloadSpeed > 0L) {
                    transferStarted = true
                    break
                }
                val increment = when {
                    simulatedProgress < 35f -> 2.4f
                    simulatedProgress < 65f -> 1.8f
                    simulatedProgress < 85f -> 1.1f
                    else -> 0.4f
                }
                simulatedProgress = (simulatedProgress + increment).coerceAtMost(94f)
                val jitter = ((System.currentTimeMillis() % 7) - 3) * 110_000L
                simulatedSpeed = (2_800_000L + jitter).coerceIn(1_900_000L, 4_100_000L)
                simulatedBytes = ((simulatedProgress / 100f) * estimatedTotal).toLong()
                val eta = if (simulatedSpeed > 0) ((estimatedTotal - simulatedBytes) / simulatedSpeed).coerceAtLeast(3L) else 10L

                downloadDao.updateProgress(localJobId, simulatedProgress, simulatedSpeed, eta, simulatedBytes)
                ForegroundTransferService.updateProgress(
                    context = context,
                    jobId = localJobId,
                    title = currentTitle,
                    progress = simulatedProgress.toInt(),
                    statusText = "Downloading",
                    speed = FormattingUtils.formatSpeed(simulatedSpeed)
                )
            }
        }

        try {
            var sessionReady = false
            for (attempt in 1..4) {
                sessionReady = mediaRepository.ensureSession()
                if (sessionReady) break
                delay(2500)
            }

            if (!sessionReady) {
                downloadDao.updateStatus(localJobId, "PROCESSING", error = "Waking cloud engine… retrying")
                delay(5000)
                sessionReady = mediaRepository.ensureSession()
            }

            if (!sessionReady) {
                tickerJob.cancel()
                val err = "Could not reach media server. Check connection or tap retry."
                downloadDao.updateStatus(localJobId, "FAILED", error = err)
                ForegroundTransferService.failDownload(context, localJobId, currentTitle, err)
                return
            }

            val response = api.createDownload(request)
            if (response.isSuccessful && response.body() != null) {
                val remoteJob = response.body()!!
                currentTitle = remoteJob.title ?: currentTitle
                currentThumbnail = remoteJob.thumbnail ?: currentThumbnail

                val existing = downloadDao.getDownloadSync(localJobId)
                if (existing != null) {
                    downloadDao.insertOrUpdate(
                        existing.copy(
                            mediaTitle = currentTitle,
                            thumbnailUrl = currentThumbnail ?: existing.thumbnailUrl,
                            fileSizeBytes = remoteJob.fileSizeBytes ?: existing.fileSizeBytes
                        )
                    )
                }

                if (!remoteJob.fileUrl.isNullOrBlank()) {
                    tickerJob.cancel()
                    transferStarted = true
                    downloadDao.updateFileUrl(localJobId, remoteJob.fileUrl)
                    transferEngine.startFileTransfer(
                        jobId = localJobId,
                        fileUrl = remoteJob.fileUrl,
                        title = currentTitle,
                        mediaType = kind,
                        format = format,
                        totalBytes = remoteJob.fileSizeBytes ?: 0L
                    )
                } else {
                    trackJobViaSse(
                        localJobId = localJobId,
                        remoteJobId = remoteJob.id,
                        title = currentTitle,
                        mediaType = kind,
                        format = format,
                        simulatedProgressProvider = { simulatedProgress },
                        onTransferInitiated = {
                            tickerJob.cancel()
                            transferStarted = true
                        }
                    )
                }
            } else {
                tickerJob.cancel()
                val rawErr = response.errorBody()?.string()
                val err = parseError(rawErr, "Download failed (HTTP ${response.code()})")
                downloadDao.updateStatus(localJobId, "FAILED", error = err)
                ForegroundTransferService.failDownload(context, localJobId, currentTitle, err)
            }
        } catch (e: Exception) {
            tickerJob.cancel()
            val err = e.localizedMessage ?: "Failed to process download"
            downloadDao.updateStatus(localJobId, "FAILED", error = err)
            ForegroundTransferService.failDownload(context, localJobId, currentTitle, err)
        }
    }

    override suspend fun enqueueBatch(
        urls: List<String>,
        kind: String,
        quality: String?,
        format: String?
    ): Resource<List<String>> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<String>()
        for (url in urls) {
            val req = CreateDownloadRequestDto(
                url = url,
                kind = kind,
                quality = quality,
                format = format
            )
            when (val res = enqueueDownload(req, BrowserRules.displayHost(url), null)) {
                is Resource.Success -> ids.add(res.data)
                else -> Unit
            }
        }
        if (ids.isNotEmpty()) Resource.Success(ids) else Resource.Error("Could not enqueue batch items")
    }

    override suspend fun enqueueImageDownload(
        url: String,
        title: String?,
        format: String?
    ): Resource<String> = withContext(Dispatchers.IO) {
        val localJobId = "img_" + System.currentTimeMillis() + "_" + (1000..9999).random()
        val safeTitle = title?.ifBlank { null } ?: "Image Download"
        val fmt = format ?: "jpg"

        val entity = DownloadEntity(
            jobId = localJobId,
            sourceUrl = url,
            mediaTitle = safeTitle,
            mediaType = "image",
            selectedFormat = fmt,
            selectedQuality = "original",
            thumbnailUrl = url,
            status = "PROCESSING",
            progress = 10f,
            createdAt = System.currentTimeMillis()
        )
        downloadDao.insertOrUpdate(entity)
        ForegroundTransferService.startService(context, localJobId, safeTitle)

        repositoryScope.launch {
            try {
                if (!mediaRepository.ensureSession()) {
                    downloadDao.updateStatus(localJobId, "FAILED", error = "Server unavailable")
                    ForegroundTransferService.failDownload(context, localJobId, safeTitle, "Server unavailable")
                    return@launch
                }
                val response = api.downloadImage(DownloadImageRequestDto(url, safeTitle, fmt))
                if (response.isSuccessful && response.body() != null) {
                    val job = response.body()!!
                    val finalTitle = title ?: job.title ?: safeTitle
                    job.fileUrl?.let { fileUrl ->
                        downloadDao.updateFileUrl(localJobId, fileUrl)
                        transferEngine.startFileTransfer(
                            jobId = localJobId,
                            fileUrl = fileUrl,
                            title = finalTitle,
                            mediaType = "image",
                            format = fmt,
                            totalBytes = job.fileSizeBytes ?: 0L
                        )
                    }
                } else {
                    val err = parseError(response.errorBody()?.string(), "Failed to download image")
                    downloadDao.updateStatus(localJobId, "FAILED", error = err)
                    ForegroundTransferService.failDownload(context, localJobId, safeTitle, err)
                }
            } catch (e: Exception) {
                val err = e.localizedMessage ?: "Image download error"
                downloadDao.updateStatus(localJobId, "FAILED", error = err)
                ForegroundTransferService.failDownload(context, localJobId, safeTitle, err)
            }
        }

        Resource.Success(localJobId)
    }

    private fun trackJobViaSse(
        localJobId: String,
        remoteJobId: String,
        title: String,
        mediaType: String,
        format: String,
        simulatedProgressProvider: () -> Float,
        onTransferInitiated: () -> Unit
    ) {
        repositoryScope.launch {
            var initiated = false
            try {
                val prefs = preferencesDataStore.preferencesFlow.first()
                val eventsUrl = prefs.serverUrl.removeSuffix("/") + "/app/v1/downloads/$remoteJobId/events"

                sseEventManager.streamJobEvents(eventsUrl).collect { event ->
                    when (event.type) {
                        "progress" -> {
                            event.progress?.let { p ->
                                val effectiveProgress = maxOf(simulatedProgressProvider(), p)
                                downloadDao.updateProgress(
                                    jobId = localJobId,
                                    progress = effectiveProgress,
                                    speed = event.speed ?: 0L,
                                    eta = event.eta ?: 0L,
                                    downloadedBytes = 0L
                                )
                                ForegroundTransferService.updateProgress(
                                    context = context,
                                    jobId = localJobId,
                                    title = title,
                                    progress = effectiveProgress.toInt(),
                                    statusText = "Processing media stream..."
                                )
                            }
                        }
                        "complete" -> {
                            val fileUrl = event.job?.fileUrl
                            val sizeBytes = event.job?.fileSizeBytes ?: 0L
                            if (!fileUrl.isNullOrBlank()) {
                                initiated = true
                                onTransferInitiated()
                                downloadDao.updateFileUrl(localJobId, fileUrl)
                                transferEngine.startFileTransfer(
                                    jobId = localJobId,
                                    fileUrl = fileUrl,
                                    title = title,
                                    mediaType = mediaType,
                                    format = format,
                                    totalBytes = sizeBytes
                                )
                            }
                        }
                        "error" -> {
                            initiated = true
                            onTransferInitiated()
                            val err = event.message ?: "Processing error"
                            downloadDao.updateStatus(localJobId, "FAILED", error = err)
                            ForegroundTransferService.failDownload(context, localJobId, title, err)
                        }
                    }
                }
            } catch (_: Exception) {}

            // Fallback polling if SSE disconnects or finishes before completion
            if (!initiated) {
                for (pollAttempt in 1..40) {
                    delay(1500)
                    try {
                        val poll = api.getDownload(remoteJobId)
                        if (poll.isSuccessful && poll.body() != null) {
                            val job = poll.body()!!
                            if (job.status == "COMPLETED" && !job.fileUrl.isNullOrBlank()) {
                                onTransferInitiated()
                                downloadDao.updateFileUrl(localJobId, job.fileUrl)
                                transferEngine.startFileTransfer(
                                    jobId = localJobId,
                                    fileUrl = job.fileUrl,
                                    title = title,
                                    mediaType = mediaType,
                                    format = format,
                                    totalBytes = job.fileSizeBytes ?: 0L
                                )
                                break
                            } else if (job.status == "FAILED") {
                                onTransferInitiated()
                                val err = job.error ?: "Download processing failed"
                                downloadDao.updateStatus(localJobId, "FAILED", error = err)
                                ForegroundTransferService.failDownload(context, localJobId, title, err)
                                break
                            }
                        }
                    } catch (_: Exception) {}
                }
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
        } else if (download != null) {
            downloadDao.updateStatus(jobId, "FAILED", error = "The file link is not ready. Retry this download.")
        }
    }

    override suspend fun cancelDownload(jobId: String) = withContext(Dispatchers.IO) {
        transferEngine.cancelTransfer(jobId)
        try {
            api.cancelDownload(jobId)
        } catch (_: Exception) {}
        downloadDao.updateStatus(jobId, "CANCELLED")
        ForegroundTransferService.stopService(context)
    }

    override suspend fun deleteDownload(jobId: String) = withContext(Dispatchers.IO) {
        val entity = downloadDao.getDownloadSync(jobId)
        if (entity?.localUri != null) {
            runCatching { storageManager.deleteMediaFile(Uri.parse(entity.localUri)) }
        }
        transferEngine.cancelTransfer(jobId)
        downloadDao.delete(jobId)
        ForegroundTransferService.stopService(context)
    }

    override fun getHistory(): Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    override fun searchHistory(query: String): Flow<List<HistoryEntity>> = historyDao.searchHistory(query)
    override suspend fun deleteHistory(id: Long) = withContext(Dispatchers.IO) {
        val entity = historyDao.getById(id)
        if (entity != null) {
            runCatching { storageManager.deleteMediaFile(Uri.parse(entity.localUri)) }
        }
        historyDao.delete(id)
    }
    override suspend fun clearHistory() = historyDao.clearAll()
    override suspend fun restoreHistory(items: List<HistoryEntity>) {
        items.forEach { historyDao.insert(it) }
    }

    private fun parseError(raw: String?, defaultMessage: String): String {
        if (raw.isNullOrBlank()) return defaultMessage
        return try {
            val json = JSONObject(raw)
            val message = json.optString("message")
            if (message.isNotBlank() && !message.startsWith("{") && message.length < 180) {
                message
            } else {
                defaultMessage
            }
        } catch (_: Exception) {
            if (raw.length < 180 && !raw.startsWith("{")) raw else defaultMessage
        }
    }
}
