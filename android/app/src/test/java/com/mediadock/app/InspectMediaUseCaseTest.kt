package com.mediadock.app

import com.mediadock.app.core.common.Resource
import com.mediadock.app.domain.model.MediaDetails
import com.mediadock.app.domain.repository.MediaRepository
import com.mediadock.app.domain.usecase.InspectMediaUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InspectMediaUseCaseTest {

    private lateinit var repository: MediaRepository
    private lateinit var useCase: InspectMediaUseCase

    @Before
    fun setup() {
        repository = mockk()
        useCase = InspectMediaUseCase(repository)
    }

    @Test
    fun testRejectsInvalidUrlImmediately() = runTest {
        val result = useCase("invalid-url")
        assertTrue(result is Resource.Error)
        assertEquals("Please enter a valid HTTP or HTTPS media link.", (result as Resource.Error).message)
        coVerify(exactly = 0) { repository.inspectMedia(any()) }
    }

    @Test
    fun testDelegatesValidUrlToRepository() = runTest {
        val mockDetails = MediaDetails(
            id = "123",
            platform = "youtube",
            url = "https://youtube.com/watch?v=123",
            title = "Test Video"
        )
        coEvery { repository.inspectMedia("https://youtube.com/watch?v=123") } returns Resource.Success(mockDetails)

        val result = useCase("https://youtube.com/watch?v=123")
        assertTrue(result is Resource.Success)
        assertEquals("Test Video", (result as Resource.Success).data.title)
        coVerify(exactly = 1) { repository.inspectMedia("https://youtube.com/watch?v=123") }
    }
}
