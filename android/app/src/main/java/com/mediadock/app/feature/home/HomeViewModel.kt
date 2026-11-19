package com.mediadock.app.feature.home

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.datastore.WorkspaceRules
import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.data.remote.dto.CreateDownloadRequestDto
import com.mediadock.app.domain.usecase.CreateDownloadUseCase
import com.mediadock.app.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val createDownloadUseCase: CreateDownloadUseCase? = null
) : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val clipboardSuggestion = MutableStateFlow<String?>(null)
    private var dismissedClipboard: String? = null

    /** Called only while Home has window focus. No network request is made here. */
    fun refreshClipboardSuggestion() {
        val text = runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
        }.getOrNull()
        suggestClipboard(text)
    }

    fun suggestClipboard(text: String?) {
        val candidate = text?.let { WorkspaceRules.extractLinks(it).firstOrNull() }
        if (candidate != dismissedClipboard) dismissedClipboard = null
        clipboardSuggestion.value = candidate?.takeUnless { it == dismissedClipboard }
    }

    fun dismissClipboardSuggestion() {
        dismissedClipboard = clipboardSuggestion.value
        clipboardSuggestion.value = null
    }

    val recentDownloads: StateFlow<List<HistoryEntity>> = getHistoryUseCase()
        .map { list -> list.take(5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onUrlChange(newUrl: String) {
        _urlInput.value = newUrl
        _errorMessage.value = null
    }

    fun clearUrl() {
        _urlInput.value = ""
        _errorMessage.value = null
    }

    fun startDownload(url: String) {
        val clean = url.trim()
        if (!WorkspaceRules.validUrl(clean)) return
        viewModelScope.launch {
            createDownloadUseCase?.invoke(
                CreateDownloadRequestDto(clean, "video"),
                com.mediadock.app.feature.browser.BrowserRules.displayHost(clean).ifBlank { "Media Download" },
                null
            )
        }
    }

    fun pasteFromClipboard() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pasted = clip.getItemAt(0).coerceToText(context).toString().trim()
                if (FormattingUtils.isValidUrl(pasted)) {
                    _urlInput.value = pasted
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Clipboard does not contain a valid media link."
                }
            } else {
                _errorMessage.value = "Clipboard is empty."
            }
        } catch (_: Exception) {
            _errorMessage.value = "Could not access clipboard."
        }
    }

    fun handleSharedUrl(sharedText: String?) {
        if (!sharedText.isNullOrBlank()) {
            val urlRegex = """(https?://\S+)""".toRegex()
            val match = urlRegex.find(sharedText)
            val extracted = match?.value ?: sharedText.trim()
            if (FormattingUtils.isValidUrl(extracted)) {
                _urlInput.value = extracted
                _errorMessage.value = null
            }
        }
    }

    fun validateUrl(): Boolean {
        val current = _urlInput.value.trim()
        if (!FormattingUtils.isValidUrl(current)) {
            _errorMessage.value = "Please enter a valid HTTP or HTTPS media link."
            return false
        }
        return true
    }
}
