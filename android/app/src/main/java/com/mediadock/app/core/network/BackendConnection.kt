package com.mediadock.app.core.network

import com.mediadock.app.core.datastore.PreferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** Wakes a sleeping backend with read-only probes before submitting work. */
@Singleton
class BackendConnection @Inject constructor(
    private val api: MediaDockApi,
    private val preferences: PreferencesDataStore
) {
    private val mutex = Mutex()
    private var readyOrigin = ""
    private var readyAtNanos = 0L

    suspend fun awaitReady(): Boolean = mutex.withLock {
        val origin = preferences.preferencesFlow.first().serverUrl
        if (origin == readyOrigin && System.nanoTime() - readyAtNanos < 30_000_000_000L) return@withLock true
        val ready = withTimeoutOrNull(120_000L) {
            repeat(30) {
                try {
                    val response = api.checkLiveness()
                    if (response.isSuccessful && response.body()?.get("status") == "ok") return@withTimeoutOrNull true
                    if (response.code() in 400..499) return@withTimeoutOrNull false
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // A waking host may temporarily return HTML or reset a connection.
                }
                delay(2_000L)
            }
            false
        } ?: false
        if (ready) {
            readyOrigin = origin
            readyAtNanos = System.nanoTime()
        }
        ready
    }
}
