package com.mediadock.app.core.network

import com.mediadock.app.data.remote.dto.JobEventDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SseEventManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) {
    fun streamJobEvents(eventsUrl: String): Flow<JobEventDto> = callbackFlow {
        val request = Request.Builder()
            .url(eventsUrl)
            .header("Accept", "text/event-stream")
            .build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val event = json.decodeFromString<JobEventDto>(data)
                    trySend(event)
                    if (event.type == "complete" || event.type == "error") {
                        close()
                    }
                } catch (e: Exception) {
                    // Ignore malformed ping or unparsable frame
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(t)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val factory = EventSources.createFactory(okHttpClient)
        val eventSource = factory.newEventSource(request, listener)

        awaitClose {
            eventSource.cancel()
        }
    }
}
