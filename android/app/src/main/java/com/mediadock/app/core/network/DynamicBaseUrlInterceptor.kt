package com.mediadock.app.core.network

import com.mediadock.app.core.datastore.PreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.Request
import retrofit2.Invocation
import retrofit2.http.Streaming
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (request.isDirectFileRequest()) return chain.proceed(request)

        val configuredUrlStr = try {
            runBlocking {
                preferencesDataStore.preferencesFlow.first().serverUrl
            }
        } catch (_: Exception) {
            ApiConstants.DEFAULT_BASE_URL
        }

        val targetHttpUrl = configuredUrlStr.toHttpUrlOrNull()
        if (targetHttpUrl != null) {
            val newHttpUrl = request.url.newBuilder()
                .scheme(targetHttpUrl.scheme)
                .host(targetHttpUrl.host)
                .port(targetHttpUrl.port)
                .build()
            request = request.newBuilder().url(newHttpUrl).build()
        }

        return chain.proceed(request)
    }
}

internal fun Request.isDirectFileRequest(): Boolean =
    tag(Invocation::class.java)?.method()?.isAnnotationPresent(Streaming::class.java) == true
