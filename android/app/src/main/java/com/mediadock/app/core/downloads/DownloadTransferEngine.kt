package com.mediadock.app.core.downloads

import android.content.Context
import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.database.dao.DownloadDao
import com.mediadock.app.core.database.dao.HistoryDao
import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.core.datastore.PreferencesDataStore
import com.mediadock.app.core.network.MediaDockApi
import com.mediadock.app.core.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadTransferEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MediaDockApi,
    private val downloadDao: DownloadDao,
    private val historyDao: HistoryDao,
    private val storageManager: StorageManager,
    private val preferencesDataStore: PreferencesDataStore
) {
    private val engineScope = CoroutineScope(Dispatchers.IO + Job())
    private val activeTransfers = ConcurrentHashMap<String, Job>()
    private val pauseRequests = ConcurrentHashMap<String, Boolean>()

    fun startFileTransfer(
        jobId: String,
        fileUrl: String,
        title: String,
        mediaType: String,
        format: String,
        totalBytes: Long
    ) {
        if (activeTransfers.containsKey(jobId)) return

        val transferJob = engineScope.launch {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            var targetUri: android.net.Uri? = null

            try {
                ForegroundTransferService.startService(context, title)
                downloadDao.updateStatus(jobId, "DOWNLOADING")

                val prefs = preferencesDataStore.preferencesFlow.first()
                val fullUrl = if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                    fileUrl
                } else {
                    prefs.serverUrl.removeSuffix("/") + "/" + fileUrl.removePrefix("/")
                }

                val ext = if (format.isNotBlank()) format else "mp4"
                val filename = "${FormattingUtils.sanitizeFilename(title)}.$ext"
                val mimeType = when (mediaType.lowercase()) {
                    "video" -> "video/$ext"
                    "audio" -> "audio/$ext"
                    "image" -> "image/$ext"
                    "gif" -> "image/gif"
                    else -> "application/octet-stream"
                }

                val (uri, out) = storageManager.createMediaOutputStream(
                    filename = filename,
                    mediaType = mediaType,
                    mimeType = mimeType,
                    customDirUri = prefs.customDownloadDirectoryUri
                )
                targetUri = uri
                outputStream = out

                val response = api.streamFile(fullUrl)
                if (!response.isSuccessful || response.body() == null) {
                    throw IllegalStateException("Failed to download file: HTTP ${response.code()}")
                }

                val body: ResponseBody = response.body()!!
                val contentLength = if (body.contentLength() > 0) body.contentLength() else totalBytes
                inputStream = body.byteStream()

                val buffer = ByteArray(16 * 1024) // 16 KB streaming chunk
                var bytesRead: Int
                var totalRead = 0L
                var lastProgressUpdate = System.currentTimeMillis()
                var bytesSinceLastUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (pauseRequests.remove(jobId) == true) {
                        downloadDao.updateStatus(jobId, "PAUSED")
                        return@launch
                    }

                    outputStream.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    bytesSinceLastUpdate += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastProgressUpdate
                    if (timeDiff >= 500) { // Update progress twice a second
                        val speedBytesPerSec = if (timeDiff > 0) (bytesSinceLastUpdate * 1000) / timeDiff else 0L
                        val progressPercent = if (contentLength > 0) {
                            ((totalRead.toFloat() / contentLength.toFloat()) * 100f).coerceIn(0f, 100f)
                        } else 0f
                        val remainingBytes = if (contentLength > totalRead) contentLength - totalRead else 0L
                        val etaSec = if (speedBytesPerSec > 0) remainingBytes / speedBytesPerSec else 0L

                        downloadDao.updateProgress(
                            jobId = jobId,
                            progress = progressPercent,
                            speed = speedBytesPerSec,
                            eta = etaSec,
                            downloadedBytes = totalRead
                        )

                        ForegroundTransferService.updateProgress(
                            context,
                            title,
                            progressPercent.toInt(),
                            FormattingUtils.formatSpeed(speedBytesPerSec)
                        )

                        lastProgressUpdate = now
                        bytesSinceLastUpdate = 0L
                    }
                }

                outputStream.flush()
                storageManager.finishMediaStoreEntry(targetUri)

                // Mark Completed in Database
                downloadDao.updateStatus(
                    jobId = jobId,
                    status = "COMPLETED",
                    completedAt = System.currentTimeMillis(),
                    localUri = targetUri.toString()
                )
                downloadDao.updateProgress(jobId, 100f, 0L, 0L, totalRead)

                // Add to History
                historyDao.insert(
                    HistoryEntity(
                        jobId = jobId,
                        sourceUrl = fullUrl,
                        title = title,
                        mediaType = mediaType,
                        format = ext,
                        localUri = targetUri.toString(),
                        fileSizeBytes = totalRead
                    )
                )

            } catch (e: Exception) {
                if (e is CancellationException) {
                    downloadDao.updateStatus(jobId, "CANCELLED")
                } else {
                    downloadDao.updateStatus(jobId, "FAILED", error = e.localizedMessage ?: "Download error")
                }
            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { outputStream?.close() } catch (_: Exception) {}
                activeTransfers.remove(jobId)

                if (activeTransfers.isEmpty()) {
                    ForegroundTransferService.stopService(context)
                }
            }
        }

        activeTransfers[jobId] = transferJob
    }

    fun pauseTransfer(jobId: String) {
        pauseRequests[jobId] = true
    }

    fun cancelTransfer(jobId: String) {
        activeTransfers[jobId]?.cancel()
        activeTransfers.remove(jobId)
        pauseRequests.remove(jobId)
    }

    fun isTransferring(jobId: String): Boolean = activeTransfers.containsKey(jobId)
}
