package com.mediadock.app.feature.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediadock.app.domain.model.ActiveDownload
import com.mediadock.app.domain.usecase.GetActiveDownloadsUseCase
import com.mediadock.app.domain.usecase.ManageDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    getActiveDownloadsUseCase: GetActiveDownloadsUseCase,
    private val manageDownloadUseCase: ManageDownloadUseCase
) : ViewModel() {

    val activeDownloads: StateFlow<List<ActiveDownload>> = getActiveDownloadsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun pauseDownload(jobId: String) {
        viewModelScope.launch {
            manageDownloadUseCase.pause(jobId)
        }
    }

    fun resumeDownload(jobId: String) {
        viewModelScope.launch {
            manageDownloadUseCase.resume(jobId)
        }
    }

    fun cancelDownload(jobId: String) {
        viewModelScope.launch {
            manageDownloadUseCase.cancel(jobId)
        }
    }

    fun deleteDownload(jobId: String) {
        viewModelScope.launch {
            manageDownloadUseCase.delete(jobId)
        }
    }
}
