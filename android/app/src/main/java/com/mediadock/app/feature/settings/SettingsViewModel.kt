package com.mediadock.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediadock.app.core.datastore.AppPreferences
import com.mediadock.app.core.datastore.ThemeMode
import com.mediadock.app.domain.repository.SettingsRepository
import com.mediadock.app.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val getHistoryUseCase: GetHistoryUseCase
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = settingsRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setServerUrl(url: String) {
        viewModelScope.launch {
            settingsRepository.setServerUrl(url)
        }
    }

    fun setDefaultVideoQuality(quality: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultVideoQuality(quality)
        }
    }

    fun setDefaultAudioFormat(format: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultAudioFormat(format)
        }
    }

    fun setMaxConcurrent(count: Int) {
        viewModelScope.launch {
            settingsRepository.setMaxConcurrentDownloads(count)
        }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWifiOnly(wifiOnly)
        }
    }

    fun setCustomDownloadDir(uri: String?) {
        viewModelScope.launch {
            settingsRepository.setCustomDownloadDirectoryUri(uri)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            getHistoryUseCase.clearAll()
        }
    }
}
