package com.mediadock.app

import androidx.lifecycle.SavedStateHandle
import com.mediadock.app.core.common.Resource
import com.mediadock.app.domain.model.MediaDetails
import com.mediadock.app.domain.usecase.*
import com.mediadock.app.feature.media.MediaInspectViewModel
import com.mediadock.app.core.datastore.PreferencesDataStore
import com.mediadock.app.core.datastore.AppPreferences
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
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
    private val preferences = mockk<PreferencesDataStore> { every { preferencesFlow } returns flowOf(AppPreferences()) }
    private val url = "https://example.com/video?token=a+b%2Fc"
    private val details = MediaDetails("1", "generic", url, "Video")

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    private fun viewModel() = MediaInspectViewModel(SavedStateHandle(mapOf("url" to url)), inspect, images, create, downloadImage, preferences)

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

    @Test fun appliesPreferredQualityAndAudioFormatOnlyWhenSupported() = runTest(dispatcher) {
        every { preferences.preferencesFlow } returns flowOf(AppPreferences(defaultVideoQuality = "720p", defaultAudioFormat = "m4a"))
        coEvery { inspect(url) } returns Resource.Success(details.copy(videoQualities = listOf("1080p", "720p", "360p"), audioFormats = listOf("mp3", "m4a")))
        coEvery { images(url) } returns Resource.Success(emptyList())
        val model = viewModel(); advanceUntilIdle()
        assertEquals("720p", model.selectedVideoQuality.value)
        assertEquals("m4a", model.selectedAudioFormat.value)
    }

    @Test fun unsupportedPreferencesFallBackToAvailableFormats() = runTest(dispatcher) {
        every { preferences.preferencesFlow } returns flowOf(AppPreferences(defaultVideoQuality = "1080p", defaultAudioFormat = "flac"))
        coEvery { inspect(url) } returns Resource.Success(details.copy(videoQualities = listOf("360p"), audioFormats = listOf("mp3")))
        coEvery { images(url) } returns Resource.Success(emptyList())
        val model = viewModel(); advanceUntilIdle()
        assertEquals("360p", model.selectedVideoQuality.value)
        assertEquals("mp3", model.selectedAudioFormat.value)
    }

    @Test fun quickDownloadQueuesTheAvailableDefaultOnlyOnce() = runTest(dispatcher) {
        coEvery { inspect(url) } returns Resource.Success(details.copy(videoQualities = listOf("360p"), videoFormats = listOf("mp4")))
        coEvery { images(url) } returns Resource.Success(emptyList())
        coEvery { create(any(), any(), any()) } returns Resource.Error("Fixture failure")
        val handle = SavedStateHandle(mapOf("url" to url, "quick" to true))
        val model = MediaInspectViewModel(handle, inspect, images, create, downloadImage, preferences)
        advanceUntilIdle(); model.retry(); advanceUntilIdle()
        coVerify(exactly = 1) { create(match { it.url == url && it.quality == "360p" && it.format == "mp4" }, any(), any()) }
        assertEquals(true, handle.get<Boolean>("quickStarted"))
        assertEquals("Fixture failure", model.downloadError.value)
        coVerify(exactly = 0) { images(any()) }
    }

    @Test fun restoredQuickRequestDoesNotEnqueueAgain() = runTest(dispatcher) {
        coEvery { inspect(url) } returns Resource.Success(details.copy(videoFormats = listOf("mp4")))
        coEvery { images(url) } returns Resource.Success(emptyList())
        MediaInspectViewModel(SavedStateHandle(mapOf("url" to url, "quick" to true, "quickStarted" to true)), inspect, images, create, downloadImage, preferences)
        advanceUntilIdle()
        coVerify(exactly = 0) { create(any(), any(), any()) }
    }

    @Test fun queueInspectionPublishesLoadingAndReplacesAnOlderLookup() = runTest(dispatcher) {
        val nextUrl = "https://example.com/second-video"
        val next = details.copy(url = nextUrl, title = "Second video")
        val oldLookup = CompletableDeferred<Resource<MediaDetails>>()
        coEvery { inspect(url) } coAnswers { oldLookup.await() }
        coEvery { inspect(nextUrl) } returns Resource.Success(next)
        coEvery { images(any()) } returns Resource.Success(emptyList())
        val model = MediaInspectViewModel(SavedStateHandle(), inspect, images, create, downloadImage, preferences)

        model.startInspection(url)
        assertEquals(url, model.sourceUrl.value)
        assertEquals(Resource.Loading, model.mediaDetailsState.value)
        runCurrent()
        model.startInspection(nextUrl)
        oldLookup.complete(Resource.Success(details))
        advanceUntilIdle()
        assertEquals(nextUrl, model.sourceUrl.value)
        assertEquals(Resource.Success(next), model.mediaDetailsState.value)
    }

    @Test fun successfullyQueuedMediaClearsTheInspectionCard() = runTest(dispatcher) {
        coEvery { inspect(url) } returns Resource.Success(details)
        coEvery { images(url) } returns Resource.Success(emptyList())
        coEvery { create(any(), any(), any()) } returns Resource.Success("job-id")
        val model = viewModel()
        advanceUntilIdle()
        model.downloadVideo(details)
        advanceUntilIdle()
        assertEquals("", model.sourceUrl.value)
        assertEquals(Resource.Idle, model.mediaDetailsState.value)
    }
}
