package com.mediadock.app.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediadock.app.core.common.Resource
import com.mediadock.app.data.remote.dto.CreateDownloadRequestDto
import com.mediadock.app.domain.model.ActiveDownload
import com.mediadock.app.domain.model.DownloadJobStatus
import com.mediadock.app.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel class DownloadsViewModel @Inject constructor(
    getActiveDownloadsUseCase: GetActiveDownloadsUseCase,
    private val manageDownloadUseCase: ManageDownloadUseCase,
    private val createDownloadUseCase: CreateDownloadUseCase,
    private val imageDownloadUseCase: DownloadImageUseCase
) : ViewModel() {
    val activeDownloads = getActiveDownloadsUseCase().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val actionError = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val speedLimit = MutableStateFlow("Unlimited")
    val nightModeScheduled = MutableStateFlow(false)

    fun setSpeedLimit(limit: String) { speedLimit.value = limit }
    fun toggleNightMode() { nightModeScheduled.value = !nightModeScheduled.value }
    fun exportQueueUrls(): String = activeDownloads.value.joinToString("\n") { it.sourceUrl }

    private fun action(block: suspend () -> Unit) = viewModelScope.launch {
        try { actionError.value = null; block() } catch (e: CancellationException) { throw e } catch (_: Exception) { actionError.value = "Could not complete this action. Please try again." }
    }
    fun pauseDownload(id: String) = action { manageDownloadUseCase.pause(id) }
    fun resumeDownload(id: String) = action { manageDownloadUseCase.resume(id) }
    fun cancelDownload(id: String) = action { manageDownloadUseCase.cancel(id) }
    fun deleteDownload(id: String) = action { manageDownloadUseCase.delete(id) }
    fun startDownload(url: String, title: String = "") = viewModelScope.launch {
        val clean = url.trim()
        val safeTitle = title.ifBlank { com.mediadock.app.feature.browser.BrowserRules.displayHost(clean).ifBlank { "Media Download" } }
        createDownloadUseCase(CreateDownloadRequestDto(clean, "video"), safeTitle, null)
    }
    private suspend fun retry(item: ActiveDownload) {
        if (item.mediaType == "gif") { actionError.value = "Open the source again to choose the GIF clip range."; return }
        val result = if (item.mediaType == "image") imageDownloadUseCase(item.sourceUrl, item.title, item.format)
            else createDownloadUseCase(CreateDownloadRequestDto(item.sourceUrl, item.mediaType, item.quality, item.format), item.title, item.thumbnail)
        if (result is Resource.Success) manageDownloadUseCase.delete(item.jobId)
        else if (result is Resource.Error) actionError.value = result.message
    }
    fun retryDownload(item: ActiveDownload) = bulk { retry(item) }
    fun pauseAll() = bulk { activeDownloads.value.filter { it.status == DownloadJobStatus.DOWNLOADING }.forEach { manageDownloadUseCase.pause(it.jobId) } }
    fun resumeAll() = bulk { activeDownloads.value.filter { it.status == DownloadJobStatus.PAUSED }.forEach { manageDownloadUseCase.resume(it.jobId) } }
    fun cancelAll() = bulk { activeDownloads.value.forEach { manageDownloadUseCase.cancel(it.jobId) } }
    fun retryFailed() = bulk { activeDownloads.value.filter { it.status == DownloadJobStatus.FAILED }.forEach { retry(it) } }
    fun clearCompleted() = bulk { activeDownloads.value.filter { it.status == DownloadJobStatus.COMPLETED }.forEach { manageDownloadUseCase.delete(it.jobId) } }
    private fun bulk(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        action { try { block() } finally { busy.value = false } }
    }
}
