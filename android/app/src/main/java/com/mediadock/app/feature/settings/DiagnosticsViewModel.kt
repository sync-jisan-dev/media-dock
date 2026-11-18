package com.mediadock.app.feature.settings

import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediadock.app.core.network.MediaDockApi
import com.mediadock.app.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel class DiagnosticsViewModel @Inject constructor(private val api: MediaDockApi, history: GetHistoryUseCase) : ViewModel() {
    val library = history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val checking = MutableStateFlow(false)
    val connection = MutableStateFlow("Not checked yet")
    val storage = MutableStateFlow(0L to 0L)
    init { refreshStorage() }
    fun refreshStorage() = viewModelScope.launch(Dispatchers.IO) {
        val stats = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        storage.value = stats.availableBytes to stats.totalBytes
    }
    fun check() {
        if (checking.value) return
        checking.value = true
        viewModelScope.launch {
            val started = System.nanoTime()
            connection.value = try {
                val response = withTimeout(20000) { api.checkLiveness() }
                if (response.isSuccessful && response.body()?.get("status") == "ok") "Connected · ${(System.nanoTime() - started) / 1000000} ms"
                else "Server responded with HTTP ${response.code()}. Try again shortly."
            } catch (_: TimeoutCancellationException) { "No response in 20 seconds. Free hosting may be waking up." }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { "Could not reach the server. Check your connection and server URL." }
            finally { checking.value = false }
        }
    }
}
