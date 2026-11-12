package com.mediadock.app.data.repository

import com.mediadock.app.core.datastore.AppPreferences
import com.mediadock.app.core.datastore.PreferencesDataStore
import com.mediadock.app.core.datastore.ThemeMode
import com.mediadock.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore
) : SettingsRepository {

    override val preferences: Flow<AppPreferences> = preferencesDataStore.preferencesFlow

    override suspend fun setThemeMode(mode: ThemeMode) {
        preferencesDataStore.setThemeMode(mode)
    }

    override suspend fun setServerUrl(url: String) {
        preferencesDataStore.setServerUrl(url)
    }

    override suspend fun setDefaultVideoQuality(quality: String) {
        preferencesDataStore.setDefaultVideoQuality(quality)
    }

    override suspend fun setDefaultAudioFormat(format: String) {
        preferencesDataStore.setDefaultAudioFormat(format)
    }

    override suspend fun setMaxConcurrentDownloads(count: Int) {
        preferencesDataStore.setMaxConcurrentDownloads(count)
    }

    override suspend fun setWifiOnly(wifiOnly: Boolean) {
        preferencesDataStore.setWifiOnly(wifiOnly)
    }

    override suspend fun setCustomDownloadDirectoryUri(uri: String?) {
        preferencesDataStore.setCustomDownloadDirectoryUri(uri)
    }
}
