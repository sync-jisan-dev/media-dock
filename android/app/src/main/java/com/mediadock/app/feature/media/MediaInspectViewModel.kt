package com.mediadock.app.feature.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediadock.app.core.common.Resource
import com.mediadock.app.data.remote.dto.CreateDownloadRequestDto
import com.mediadock.app.domain.model.ExtractedImage
import com.mediadock.app.domain.model.MediaDetails
import com.mediadock.app.domain.usecase.CreateDownloadUseCase
import com.mediadock.app.domain.usecase.DownloadImageUseCase
import com.mediadock.app.domain.usecase.ExtractImagesUseCase
import com.mediadock.app.domain.usecase.InspectMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

enum class InspectTab {
    ALL, VIDEO, AUDIO, IMAGES, GIF
}

@HiltViewModel
class MediaInspectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val inspectMediaUseCase: InspectMediaUseCase,
    private val extractImagesUseCase: ExtractImagesUseCase,
    private val createDownloadUseCase: CreateDownloadUseCase,
    private val downloadImageUseCase: DownloadImageUseCase
) : ViewModel() {

    private val rawUrl: String = savedStateHandle.get<String>("url")?.let {
        try {
            URLDecoder.decode(it, StandardCharsets.UTF_8.toString())
        } catch (_: Exception) {
            it
        }
    } ?: ""

    private val _mediaDetailsState = MutableStateFlow<Resource<MediaDetails>>(Resource.Loading)
    val mediaDetailsState: StateFlow<Resource<MediaDetails>> = _mediaDetailsState.asStateFlow()

    private val _extractedImagesState = MutableStateFlow<Resource<List<ExtractedImage>>>(Resource.Loading)
    val extractedImagesState: StateFlow<Resource<List<ExtractedImage>>> = _extractedImagesState.asStateFlow()

    private val _selectedTab = MutableStateFlow(InspectTab.ALL)
    val selectedTab: StateFlow<InspectTab> = _selectedTab.asStateFlow()

    // Video options
    val selectedVideoQuality = MutableStateFlow("highest")
    val selectedVideoFormat = MutableStateFlow("mp4")
    val embedSubtitles = MutableStateFlow(false)

    // Audio options
    val selectedAudioQuality = MutableStateFlow("best")
    val selectedAudioFormat = MutableStateFlow("mp3")
    val embedAudioThumbnail = MutableStateFlow(true)

    // GIF Creator options
    val gifStartTime = MutableStateFlow(0L)
    val gifEndTime = MutableStateFlow(10L)

    // Selected Images set for batch image download
    val selectedImageIds = MutableStateFlow<Set<String>>(emptySet())

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    init {
        if (rawUrl.isNotBlank()) {
            loadMedia(rawUrl)
        }
    }

    fun selectTab(tab: InspectTab) {
        _selectedTab.value = tab
    }

    fun toggleImageSelection(imageId: String) {
        val current = selectedImageIds.value.toMutableSet()
        if (current.contains(imageId)) {
            current.remove(imageId)
        } else {
            current.add(imageId)
        }
        selectedImageIds.value = current
    }

    fun selectAllImages(images: List<ExtractedImage>) {
        if (selectedImageIds.value.size == images.size) {
            selectedImageIds.value = emptySet()
        } else {
            selectedImageIds.value = images.map { it.id }.toSet()
        }
    }

    fun loadMedia(url: String) {
        viewModelScope.launch {
            _mediaDetailsState.value = Resource.Loading
            _extractedImagesState.value = Resource.Loading

            val inspectDeferred = async { inspectMediaUseCase(url) }
            val imagesDeferred = async { extractImagesUseCase(url) }

            val mediaRes = inspectDeferred.await()
            val imagesRes = imagesDeferred.await()

            _mediaDetailsState.value = mediaRes
            _extractedImagesState.value = imagesRes

            if (mediaRes is Resource.Success) {
                val details = mediaRes.data
                selectedVideoQuality.value = details.videoQualities.firstOrNull() ?: "highest"
                selectedVideoFormat.value = details.videoFormats.firstOrNull() ?: "mp4"
                selectedAudioQuality.value = details.audioQualities.firstOrNull() ?: "best"
                selectedAudioFormat.value = details.audioFormats.firstOrNull() ?: "mp3"
                gifEndTime.value = (details.durationSeconds ?: 10L).coerceAtMost(15L)
            }
        }
    }

    fun downloadVideo(details: MediaDetails) {
        viewModelScope.launch {
            val request = CreateDownloadRequestDto(
                url = details.url,
                kind = "video",
                quality = selectedVideoQuality.value,
                format = selectedVideoFormat.value,
                embedSubtitles = embedSubtitles.value
            )
            val res = createDownloadUseCase(request, details.title, details.thumbnail)
            if (res is Resource.Success) {
                _navigationEvent.emit("downloads")
            }
        }
    }

    fun downloadAudio(details: MediaDetails) {
        viewModelScope.launch {
            val request = CreateDownloadRequestDto(
                url = details.url,
                kind = "audio",
                quality = selectedAudioQuality.value,
                format = selectedAudioFormat.value,
                embedThumbnail = embedAudioThumbnail.value
            )
            val res = createDownloadUseCase(request, details.title, details.thumbnail)
            if (res is Resource.Success) {
                _navigationEvent.emit("downloads")
            }
        }
    }

    fun downloadGif(details: MediaDetails) {
        viewModelScope.launch {
            val request = CreateDownloadRequestDto(
                url = details.url,
                kind = "video",
                format = "gif",
                clipStart = gifStartTime.value,
                clipEnd = gifEndTime.value
            )
            val res = createDownloadUseCase(request, "${details.title} (GIF)", details.thumbnail)
            if (res is Resource.Success) {
                _navigationEvent.emit("downloads")
            }
        }
    }

    fun downloadSelectedImages(images: List<ExtractedImage>) {
        viewModelScope.launch {
            val toDownload = images.filter { selectedImageIds.value.contains(it.id) }
            for (img in toDownload) {
                downloadImageUseCase(img.url, img.title, img.format)
            }
            _navigationEvent.emit("downloads")
        }
    }
}
