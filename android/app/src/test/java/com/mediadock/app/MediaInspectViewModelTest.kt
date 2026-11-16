package com.mediadock.app

import androidx.lifecycle.SavedStateHandle
import com.mediadock.app.core.common.Resource
import com.mediadock.app.domain.model.MediaDetails
import com.mediadock.app.domain.usecase.*
import com.mediadock.app.feature.media.MediaInspectViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaInspectViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val inspect = mockk<InspectMediaUseCase>()
    private val images = mockk<ExtractImagesUseCase>()
    private val create = mockk<CreateDownloadUseCase>()
    private val downloadImage = mockk<DownloadImageUseCase>()
    private val url = "https://example.com/video?token=a+b%2Fc"
    private val details = MediaDetails("1", "generic", url, "Video")

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    private fun viewModel() = MediaInspectViewModel(SavedStateHandle(mapOf("url" to url)), inspect, images, create, downloadImage)

    @Test fun preservesSignedUrlAndPublishesVideoBeforeGalleryFinishes() = runTest(dispatcher) {
        val galleryReady = CompletableDeferred<Unit>()
        coEvery { inspect(url) } returns Resource.Success(details)
        coEvery { images(url) } coAnswers { galleryReady.await(); Resource.Success(emptyList()) }
        val model = viewModel()
        runCurrent()
        assertTrue(model.mediaDetailsState.value is Resource.Success)
        assertTrue(model.extractedImagesState.value is Resource.Loading)
        coVerify { inspect(url) }
        galleryReady.complete(Unit)
        advanceUntilIdle()
        assertTrue(model.extractedImagesState.value is Resource.Success)
    }

    @Test fun rejectsDuplicateSubmissionAndDisplaysBackendFailure() = runTest(dispatcher) {
        coEvery { inspect(url) } returns Resource.Success(details)
        coEvery { images(url) } returns Resource.Success(emptyList())
        val requestReady = CompletableDeferred<Unit>()
        coEvery { create(any(), any(), any()) } coAnswers { requestReady.await(); Resource.Error("Server unavailable") }
        val model = viewModel()
        advanceUntilIdle()
        model.downloadVideo(details)
        model.downloadVideo(details)
        runCurrent()
        assertTrue(model.isSubmitting.value)
        coVerify(exactly = 1) { create(any(), any(), any()) }
        requestReady.complete(Unit)
        advanceUntilIdle()
        assertFalse(model.isSubmitting.value)
        assertEquals("Server unavailable", model.downloadError.value)
    }
}
