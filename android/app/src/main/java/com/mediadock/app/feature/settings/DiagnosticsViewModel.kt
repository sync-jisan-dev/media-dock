package com.mediadock.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediadock.app.core.network.MediaDockApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class ServerCheck(val serverUrl: String, val result: String? = null)

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(private val api: MediaDockApi) : ViewModel() {
    private val _connection = MutableStateFlow<ServerCheck?>(null)
    val connection = _connection.asStateFlow()
    private var checkJob: Job? = null

    fun check(serverUrl: String) {
        if (checkJob?.isActive == true && _connection.value?.serverUrl == serverUrl) return
        checkJob?.cancel()
        _connection.value = ServerCheck(serverUrl)
        checkJob = viewModelScope.launch {
            val started = System.nanoTime()
            val result = try {
                val response = withTimeout(20_000) { api.checkLiveness() }
                when {
                    !response.isSuccessful -> "Server returned HTTP ${response.code()}."
                    response.body()?.get("status") == "ok" -> "Connected · ${(System.nanoTime() - started) / 1_000_000} ms"
                    else -> "The server returned an unexpected health response."
                }
            } catch (_: TimeoutCancellationException) {
                "No response in 20 seconds. The server may still be waking up."
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                "Could not reach the server. Check the address and try again."
            }
            _connection.value = ServerCheck(serverUrl, result)
        }
    }
}
