package com.mediadock.app

import com.mediadock.app.core.common.Resource
import com.mediadock.app.domain.model.*
import com.mediadock.app.domain.usecase.*
import com.mediadock.app.feature.downloads.DownloadsViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val get = mockk<GetActiveDownloadsUseCase>()
    private val manage = mockk<ManageDownloadUseCase>(relaxed = true)
    private val create = mockk<CreateDownloadUseCase>()
    private val image = mockk<DownloadImageUseCase>()
    private fun job(id: String, status: DownloadJobStatus, type: String = "video") = ActiveDownload(id, "https://example.com/$id", id, type, "mp4", "360p", null, null, 0, 0, 0f, 0, 0, status, null, 0)
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { Dispatchers.resetMain() }
    @Test fun pauseAllOnlyPausesPhoneTransfers() = runTest(dispatcher) {
        every { get() } returns flowOf(listOf(job("one", DownloadJobStatus.DOWNLOADING), job("two", DownloadJobStatus.PROCESSING), job("three", DownloadJobStatus.PAUSED)))
        val vm = DownloadsViewModel(get, manage, create, image)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.activeDownloads.collect() }
        runCurrent(); vm.pauseAll(); runCurrent()
        coVerify(exactly = 1) { manage.pause("one") }; coVerify(exactly = 0) { manage.pause("two") }; coVerify(exactly = 0) { manage.pause("three") }
    }
    @Test fun retryFailureKeepsOldRecordAndShowsTheError() = runTest(dispatcher) {
        every { get() } returns flowOf(emptyList()); coEvery { create(any(), any(), any()) } returns Resource.Error("Provider unavailable")
        val vm = DownloadsViewModel(get, manage, create, image); vm.retryDownload(job("failed", DownloadJobStatus.FAILED)); runCurrent()
        assertEquals("Provider unavailable", vm.actionError.value); coVerify(exactly = 0) { manage.delete(any()) }; assertFalse(vm.busy.value)
    }
    @Test fun successfulRetryReplacesFailureOnlyAfterEnqueueSucceeds() = runTest(dispatcher) {
        every { get() } returns flowOf(emptyList()); coEvery { create(any(), any(), any()) } returns Resource.Success("new-job")
        val vm = DownloadsViewModel(get, manage, create, image); vm.retryDownload(job("failed", DownloadJobStatus.FAILED)); runCurrent()
        coVerifyOrder { create(any(), "failed", null); manage.delete("failed") }
    }
    @Test fun retryImageUsesImageEndpointAndBlocksDuplicateTaps() = runTest(dispatcher) {
        every { get() } returns flowOf(emptyList()); val ready = CompletableDeferred<Unit>()
        coEvery { image(any(), any(), any()) } coAnswers { ready.await(); Resource.Success("new-image") }
        val vm = DownloadsViewModel(get, manage, create, image); val item = job("image", DownloadJobStatus.FAILED, "image")
        vm.retryDownload(item); vm.retryDownload(item); runCurrent(); coVerify(exactly = 1) { image(any(), any(), any()) }
        ready.complete(Unit); runCurrent(); coVerify(exactly = 0) { create(any(), any(), any()) }; assertFalse(vm.busy.value)
    }
}
