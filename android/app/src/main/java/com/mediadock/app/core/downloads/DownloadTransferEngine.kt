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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.webkit.MimeTypeMap
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
    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeTransfers = ConcurrentHashMap<String, Job>()
    private val slots = TransferSlots()
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

        val transferJob = engineScope.launch(start = CoroutineStart.LAZY) {
            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null
            var targetUri: android.net.Uri? = null
            var slotHeld = false
            var complete = false
            var responseBody: ResponseBody? = null

            try {
                ForegroundTransferService.startService(context, title)
                downloadDao.updateStatus(jobId, "QUEUED")
                var prefs = preferencesDataStore.preferencesFlow.first()
                while (!slots.tryAcquire(prefs.maxConcurrentDownloads)) {
                    delay(250)
                    prefs = preferencesDataStore.preferencesFlow.first()
                }
                slotHeld = true
                if (prefs.wifiOnlyDownloading && !isWifi()) {
                    downloadDao.updateStatus(jobId, "PAUSED", error = "Waiting for Wi-Fi. Resume when connected.")
                    return@launch
                }
                downloadDao.updateStatus(jobId, "DOWNLOADING")
                downloadDao.updateProgress(jobId, 0f, 0L, 0L, 0L)
                val fullUrl = if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
                    fileUrl
                } else {
                    prefs.serverUrl.removeSuffix("/") + "/" + fileUrl.removePrefix("/")
                }

                val ext = if (format.isNotBlank()) format else "mp4"
                val filename = "${FormattingUtils.sanitizeFilename(title)}.$ext"
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
                    ?: when (mediaType.lowercase()) { "video" -> "video/mp4"; "audio" -> "audio/mpeg"; "image", "gif" -> "image/$ext"; else -> "application/octet-stream" }


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
                responseBody = body
                val contentLength = if (body.contentLength() > 0) body.contentLength() else totalBytes
                inputStream = body.byteStream()

                val buffer = ByteArray(16 * 1024) // 16 KB streaming chunk
                var bytesRead: Int
                var totalRead = 0L
                var lastProgressUpdate = System.currentTimeMillis()
                var bytesSinceLastUpdate = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    currentCoroutineContext().ensureActive()
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
                        if (preferencesDataStore.preferencesFlow.first().wifiOnlyDownloading && !isWifi()) {
                            downloadDao.updateStatus(jobId, "PAUSED", error = "Wi-Fi disconnected. Resume on Wi-Fi.")
                            return@launch
                        }
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

                check(totalRead > 0) { "The server returned an empty file. Retry the download." }
                check(contentLength <= 0 || totalRead == contentLength) { "The transfer ended before the complete file arrived. Retry the download." }
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
                        sourceUrl = downloadDao.getDownloadSync(jobId)?.sourceUrl ?: fullUrl,
                        title = title,
                        mediaType = mediaType,
                        format = ext,
                        localUri = targetUri.toString(),
                        fileSizeBytes = totalRead
                    )
                )
                complete = true

            } catch (e: Exception) {
                withContext(NonCancellable) {
                    if (e is CancellationException) downloadDao.updateStatus(jobId, if (pauseRequests.remove(jobId) == true) "PAUSED" else "CANCELLED")
                    else downloadDao.updateStatus(jobId, "FAILED", error = e.localizedMessage ?: "Download error")
                }
            } finally {
                try { inputStream?.close() } catch (_: Exception) {}
                try { outputStream?.close() } catch (_: Exception) {}
                try { responseBody?.close() } catch (_: Exception) {}
                if (!complete && targetUri != null) storageManager.discardIncomplete(targetUri)
                if (slotHeld) slots.release()
                activeTransfers.remove(jobId)
                pauseRequests.remove(jobId)

                if (activeTransfers.isEmpty()) {
                    ForegroundTransferService.stopService(context)
                }
            }
        }

        val existing = activeTransfers.putIfAbsent(jobId, transferJob)
        if (existing == null) transferJob.start() else transferJob.cancel()
    }

    private fun isWifi(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return manager.getNetworkCapabilities(manager.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    suspend fun pauseTransfer(jobId: String) {
        pauseRequests[jobId] = true
        activeTransfers[jobId]?.cancelAndJoin()
        pauseRequests.remove(jobId)
    }

    suspend fun cancelTransfer(jobId: String) {
        pauseRequests.remove(jobId)
        activeTransfers[jobId]?.cancelAndJoin()
    }

    fun isTransferring(jobId: String): Boolean = activeTransfers.containsKey(jobId)
}
