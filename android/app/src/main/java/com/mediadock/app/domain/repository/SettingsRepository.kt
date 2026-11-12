package com.mediadock.app.domain.repository

import com.mediadock.app.core.datastore.AppPreferences
import com.mediadock.app.core.datastore.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val preferences: Flow<AppPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setServerUrl(url: String)
    suspend fun setDefaultVideoQuality(quality: String)
    suspend fun setDefaultAudioFormat(format: String)
    suspend fun setMaxConcurrentDownloads(count: Int)
    suspend fun setWifiOnly(wifiOnly: Boolean)
    suspend fun setCustomDownloadDirectoryUri(uri: String?)
}
