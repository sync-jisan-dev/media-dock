package com.mediadock.app.core.network

import com.mediadock.app.core.security.SecureTokenManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: SecureTokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        // Skip auth for guest session creation, health check, or direct file streaming
        if (path.contains("sessions/guest") || path.contains("health") || path.contains("/files/")) {
            return chain.proceed(original)
        }

        val token = tokenManager.getGuestToken()
        val builder = original.newBuilder()

        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
            builder.header("X-Guest-Token", token)
        }

        return chain.proceed(builder.build())
    }
}
