package com.mediadock.app

import android.content.Context
import com.mediadock.app.domain.usecase.GetHistoryUseCase
import com.mediadock.app.feature.home.HomeViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ClipboardSuggestionTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { Dispatchers.resetMain() }
    private fun model(): HomeViewModel {
        val history = mockk<GetHistoryUseCase> { every { this@mockk.invoke() } returns flowOf(emptyList()) }
        return HomeViewModel(mockk<Context>(), history)
    }
    @Test fun suggestsOnlyWebLinksWithoutChangingInput() {
        val model = model(); model.suggestClipboard("Watch https://example.com/video?token=a+b%2Fc")
        assertEquals("https://example.com/video?token=a+b%2Fc", model.clipboardSuggestion.value)
        assertEquals("", model.urlInput.value)
        model.suggestClipboard("A private note without a link")
        assertNull(model.clipboardSuggestion.value)
    }
    @Test fun dismissedLinkStaysDismissedUntilClipboardChanges() {
        val model = model(); model.suggestClipboard("https://example.com/one"); model.dismissClipboardSuggestion()
        model.suggestClipboard("https://example.com/one"); assertNull(model.clipboardSuggestion.value)
        model.suggestClipboard("https://example.com/two"); assertEquals("https://example.com/two", model.clipboardSuggestion.value)
    }
}
