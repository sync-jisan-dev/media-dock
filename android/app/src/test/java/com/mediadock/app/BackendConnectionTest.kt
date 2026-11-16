package com.mediadock.app

import com.mediadock.app.core.datastore.AppPreferences
import com.mediadock.app.core.datastore.PreferencesDataStore
import com.mediadock.app.core.network.BackendConnection
import com.mediadock.app.core.network.MediaDockApi
import io.mockk.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class BackendConnectionTest {
    private val api = mockk<MediaDockApi>()
    private val preferences = mockk<PreferencesDataStore> {
        every { preferencesFlow } returns flowOf(AppPreferences())
    }

    @Test fun wakesServerBeforeReportingReady() = runTest {
        coEvery { api.checkLiveness() } returnsMany listOf(
            Response.error(503, "Waking up".toResponseBody()),
            Response.success(mapOf("status" to "ok"))
        )
        assertTrue(BackendConnection(api, preferences).awaitReady())
        coVerify(exactly = 2) { api.checkLiveness() }
    }

    @Test fun sharesSuccessfulProbeAcrossConcurrentCallers() = runTest {
        coEvery { api.checkLiveness() } returns Response.success(mapOf("status" to "ok"))
        val connection = BackendConnection(api, preferences)
        val first = async { connection.awaitReady() }
        val second = async { connection.awaitReady() }
        assertTrue(first.await())
        assertTrue(second.await())
        coVerify(exactly = 1) { api.checkLiveness() }
    }

    @Test fun reportsQuotaSuspensionWithoutRepeatedRequests() = runTest {
        coEvery { api.checkLiveness() } returns Response.error(402, "Quota reached".toResponseBody())
        assertFalse(BackendConnection(api, preferences).awaitReady())
        coVerify(exactly = 1) { api.checkLiveness() }
    }
}
