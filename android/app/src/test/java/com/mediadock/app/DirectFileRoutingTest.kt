package com.mediadock.app

import com.mediadock.app.core.datastore.PreferencesDataStore
import com.mediadock.app.core.network.AuthInterceptor
import com.mediadock.app.core.network.DynamicBaseUrlInterceptor
import com.mediadock.app.core.network.MediaDockApi
import com.mediadock.app.core.security.SecureTokenManager
import io.mockk.*
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertSame
import org.junit.Test
import retrofit2.Invocation

class DirectFileRoutingTest {
    private fun directRequest(): Request {
        val method = MediaDockApi::class.java.methods.single { it.name == "streamFile" }
        return Request.Builder()
            .url("https://storage.example/file.mp4?signature=a%2Bb")
            .tag(Invocation::class.java, Invocation.of(method, emptyList<Any>()))
            .build()
    }

    @Test fun preservesStorageHostAndSignedQuery() {
        val preferences = mockk<PreferencesDataStore>()
        val request = directRequest()
        val response = mockk<Response>()
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns request
            every { proceed(request) } returns response
        }
        assertSame(response, DynamicBaseUrlInterceptor(preferences).intercept(chain))
        verify { chain.proceed(request) }
        verify { preferences wasNot Called }
    }

    @Test fun keepsAppSessionTokenAwayFromStorageServers() {
        val tokens = mockk<SecureTokenManager>()
        val request = directRequest()
        val response = mockk<Response>()
        val chain = mockk<Interceptor.Chain> {
            every { request() } returns request
            every { proceed(request) } returns response
        }
        assertSame(response, AuthInterceptor(tokens).intercept(chain))
        verify { chain.proceed(request) }
        verify { tokens wasNot Called }
    }
}
