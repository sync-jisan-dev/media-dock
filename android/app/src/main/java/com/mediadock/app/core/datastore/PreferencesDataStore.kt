package com.mediadock.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mediadock.app.core.network.ApiConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mediadock_preferences")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val serverUrl: String = ApiConstants.DEFAULT_BASE_URL,
    val defaultVideoQuality: String = "1080p",
    val defaultAudioFormat: String = "mp3",
    val maxConcurrentDownloads: Int = 2,
    val wifiOnlyDownloading: Boolean = false,
    val customDownloadDirectoryUri: String? = null
)

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SERVER_URL = stringPreferencesKey("server_url")
        val DEFAULT_VIDEO_QUALITY = stringPreferencesKey("default_video_quality")
        val DEFAULT_AUDIO_FORMAT = stringPreferencesKey("default_audio_format")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent_downloads")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only_downloading")
        val CUSTOM_DIR_URI = stringPreferencesKey("custom_download_dir_uri")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        val themeStr = prefs[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }

        AppPreferences(
            themeMode = themeMode,
            serverUrl = prefs[PreferencesKeys.SERVER_URL]
                ?.takeUnless { it.trimEnd('/') == ApiConstants.LEGACY_TUNNEL_URL.trimEnd('/') }
                ?: ApiConstants.DEFAULT_BASE_URL,
            defaultVideoQuality = prefs[PreferencesKeys.DEFAULT_VIDEO_QUALITY] ?: "1080p",
            defaultAudioFormat = prefs[PreferencesKeys.DEFAULT_AUDIO_FORMAT] ?: "mp3",
            maxConcurrentDownloads = prefs[PreferencesKeys.MAX_CONCURRENT] ?: 2,
            wifiOnlyDownloading = prefs[PreferencesKeys.WIFI_ONLY] ?: false,
            customDownloadDirectoryUri = prefs[PreferencesKeys.CUSTOM_DIR_URI]
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[PreferencesKeys.THEME_MODE] = mode.name }
    }

    suspend fun setServerUrl(url: String) {
        val formatted = if (!url.endsWith("/")) "$url/" else url
        context.dataStore.edit { it[PreferencesKeys.SERVER_URL] = formatted }
    }

    suspend fun setDefaultVideoQuality(quality: String) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_VIDEO_QUALITY] = quality }
    }

    suspend fun setDefaultAudioFormat(format: String) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_AUDIO_FORMAT] = format }
    }

    suspend fun setMaxConcurrentDownloads(count: Int) {
        context.dataStore.edit { it[PreferencesKeys.MAX_CONCURRENT] = count.coerceIn(1, 5) }
    }

    suspend fun setWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.WIFI_ONLY] = wifiOnly }
    }

    suspend fun setCustomDownloadDirectoryUri(uri: String?) {
        context.dataStore.edit {
            if (uri != null) it[PreferencesKeys.CUSTOM_DIR_URI] = uri
            else it.remove(PreferencesKeys.CUSTOM_DIR_URI)
        }
    }
}
