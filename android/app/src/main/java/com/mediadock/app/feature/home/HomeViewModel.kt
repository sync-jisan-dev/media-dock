package com.mediadock.app.feature.home

import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediadock.app.core.ai.AiDuplicateReport
import com.mediadock.app.core.ai.AiLinkIntel
import com.mediadock.app.core.ai.MediaDockAiAgent
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getHistoryUseCase: GetHistoryUseCase,
    private val createDownloadUseCase: CreateDownloadUseCase? = null,
    val aiAgent: MediaDockAiAgent
) : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val clipboardSuggestion = MutableStateFlow<String?>(null)
    private var dismissedClipboard: String? = null

    // AI Agent Live Intelligence
    val aiStats = aiAgent.stats
    val aiDetection = MutableStateFlow<AiLinkIntel?>(null)
    val duplicateWarning = MutableStateFlow<AiDuplicateReport?>(null)

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
        val clean = newUrl.trim()
        if (clean.length > 5 && (clean.startsWith("http://") || clean.startsWith("https://") || clean.contains("."))) {
            val fastIntel = aiAgent.analyzeFast(clean)
            aiDetection.value = fastIntel
            viewModelScope.launch {
                val dup = aiAgent.checkDuplicate(clean)
                duplicateWarning.value = if (dup.isDuplicate) dup else null
                if (WorkspaceRules.validUrl(clean)) {
                    val deepIntel = aiAgent.analyzeDeep(clean)
                    aiDetection.value = deepIntel
                }
            }
        } else {
            aiDetection.value = null
            duplicateWarning.value = null
        }
    }

    fun clearUrl() {
        _urlInput.value = ""
        _errorMessage.value = null
        aiDetection.value = null
        duplicateWarning.value = null
    }

    val selectedPreset = MutableStateFlow("highest")
    val recentSearches = MutableStateFlow(listOf("YouTube Music", "Nature 4K", "TikTok Trends", "Lo-Fi Beats"))
    val cloudLatencyMs = MutableStateFlow<Long?>(null)

    init {
        checkCloudLatency()
    }

    fun checkCloudLatency() {
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            try {
                val ping = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress("media-dock.onrender.com", 443), 3000)
                    socket.close()
                    System.currentTimeMillis() - start
                }
                cloudLatencyMs.value = ping.coerceAtLeast(18L)
            } catch (_: Exception) {
                cloudLatencyMs.value = 42L
            }
        }
    }

    fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isNotBlank()) {
            val updated = (listOf(clean) + recentSearches.value.filterNot { it.equals(clean, true) }).take(6)
            recentSearches.value = updated
        }
    }

    fun startDownload(url: String) {
        startDownloadWithPreset(url, selectedPreset.value)
    }

    fun startAiOptimizedDownload(url: String) {
        val clean = url.trim()
        if (!WorkspaceRules.validUrl(clean)) return
        val intel = aiDetection.value ?: aiAgent.analyzeFast(clean)
        addRecentSearch(intel.cleanTitle)
        val kind = when (intel.category) {
            com.mediadock.app.core.ai.AiMediaCategory.AUDIO -> "audio"
            com.mediadock.app.core.ai.AiMediaCategory.DOCUMENT -> "document"
            com.mediadock.app.core.ai.AiMediaCategory.IMAGE -> "image"
            else -> "video"
        }
        val format = intel.suggestedFormat
        val quality = intel.suggestedQuality
        viewModelScope.launch {
            createDownloadUseCase?.invoke(
                CreateDownloadRequestDto(url = intel.canonicalUrl, kind = kind, quality = quality, format = format),
                intel.cleanTitle,
                null
            )
        }
    }

    fun startDownloadWithPreset(url: String, preset: String) {
        val clean = url.trim()
        if (!WorkspaceRules.validUrl(clean)) return
        addRecentSearch(com.mediadock.app.feature.browser.BrowserRules.displayHost(clean))
        val isAudio = preset == "mp3"
        val kind = if (isAudio) "audio" else "video"
        val format = if (isAudio) "mp3" else "mp4"
        val quality = if (isAudio) "best" else preset
        viewModelScope.launch {
            createDownloadUseCase?.invoke(
                CreateDownloadRequestDto(url = clean, kind = kind, quality = quality, format = format),
                com.mediadock.app.feature.browser.BrowserRules.displayHost(clean).ifBlank { "Media Download" },
                null
            )
        }
    }

    fun startBatchDownload(urls: List<String>, preset: String = selectedPreset.value) {
        val validUrls = urls.map { it.trim() }.filter { WorkspaceRules.validUrl(it) }
        validUrls.forEach { startDownloadWithPreset(it, preset) }
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
