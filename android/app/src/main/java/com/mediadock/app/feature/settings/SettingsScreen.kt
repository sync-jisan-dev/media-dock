package com.mediadock.app.feature.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.mediadock.app.BuildConfig
import com.mediadock.app.core.common.MediaActions
import com.mediadock.app.core.datastore.ThemeMode
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.CpdColor
import com.mediadock.app.core.network.ApiConstants
import com.mediadock.app.feature.browser.BrowserRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsPanel { THEME, VIDEO, AUDIO, FOLDER, SERVER, ADVANCED, TRANSFERS, CACHE, RESET }
private val videoQualities = listOf("360p", "720p", "1080p", "highest")
private val audioFormats = listOf("mp3", "m4a", "opus", "flac")

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun videoQualityLabel(quality: String) = when (quality) {
    "highest" -> "Best available"
    else -> quality
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onSavedLinks: () -> Unit = {},
    diagnostics: DiagnosticsViewModel = hiltViewModel()
) {
    val prefs by viewModel.preferences.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var panel by rememberSaveable { mutableStateOf<SettingsPanel?>(null) }

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            val saved = runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                viewModel.setCustomDownloadDir(uri.toString())
            }
            if (saved.isFailure) MediaActions.message(context, "This folder cannot be used. Choose another folder.")
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, top = 16.dp, end = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
        item {
            SettingsGroup("Appearance") {
                SettingsRow(
                    title = "Theme",
                    icon = HugeIcons.DarkMode,
                    value = prefs.themeMode.label(),
                    onClick = { panel = SettingsPanel.THEME }
                )
            }
        }
        item {
            SettingsGroup("Downloads") {
                SettingsRow("Video quality", HugeIcons.VideoLibrary, videoQualityLabel(prefs.defaultVideoQuality)) {
                    panel = SettingsPanel.VIDEO
                }
                SettingsDivider()
                SettingsRow("Audio format", HugeIcons.Headphones, prefs.defaultAudioFormat.uppercase()) {
                    panel = SettingsPanel.AUDIO
                }
                SettingsDivider()
                SettingsRow(
                    title = "Wi-Fi only",
                    icon = HugeIcons.Wifi,
                    modifier = Modifier.toggleable(
                        value = prefs.wifiOnlyDownloading,
                        role = Role.Switch,
                        onValueChange = viewModel::setWifiOnly
                    ),
                    trailing = { Switch(checked = prefs.wifiOnlyDownloading, onCheckedChange = null) }
                )
                SettingsDivider()
                SettingsRow(
                    title = "Download folder",
                    icon = HugeIcons.Folder,
                    value = if (prefs.customDownloadDirectoryUri == null) "Default" else "Custom"
                ) { panel = SettingsPanel.FOLDER }
            }
        }
        item {
            SettingsGroup("App") {
                SettingsRow("Saved links", HugeIcons.Collections, onClick = onSavedLinks)
                SettingsDivider()
                SettingsRow(
                    title = "Server connection",
                    icon = HugeIcons.Cloud,
                    value = if (prefs.serverUrl.trimEnd('/') == ApiConstants.DEFAULT_BASE_URL.trimEnd('/')) {
                        "MediaDock cloud"
                    } else {
                        BrowserRules.displayHost(prefs.serverUrl)
                    }
                ) { panel = SettingsPanel.SERVER }
                SettingsDivider()
                SettingsRow("Advanced", HugeIcons.Settings) { panel = SettingsPanel.ADVANCED }
            }
        }
        item {
            Text(
                "MediaDock ${BuildConfig.VERSION_NAME}",
                modifier = Modifier.padding(start = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }

    val closePanel = { panel = null }
    when (panel) {
        SettingsPanel.THEME -> SettingsChoiceDialog("Theme", ThemeMode.entries, prefs.themeMode, ThemeMode::label, closePanel) {
            viewModel.setThemeMode(it)
            closePanel()
        }
        SettingsPanel.VIDEO -> SettingsChoiceDialog("Video quality", videoQualities, prefs.defaultVideoQuality, ::videoQualityLabel, closePanel) {
            viewModel.setDefaultVideoQuality(it)
            closePanel()
        }
        SettingsPanel.AUDIO -> SettingsChoiceDialog("Audio format", audioFormats, prefs.defaultAudioFormat, { it.uppercase() }, closePanel) {
            viewModel.setDefaultAudioFormat(it)
            closePanel()
        }
        SettingsPanel.FOLDER -> AlertDialog(
            onDismissRequest = closePanel,
            title = { Text("Download folder") },
            text = { Text(if (prefs.customDownloadDirectoryUri == null) "Downloads are saved to the default media folders." else "Downloads are saved to your custom folder.") },
            confirmButton = { TextButton(onClick = { closePanel(); pickFolder.launch(null) }) { Text("Choose folder") } },
            dismissButton = {
                if (prefs.customDownloadDirectoryUri != null) {
                    TextButton(onClick = { viewModel.setCustomDownloadDir(null); closePanel() }) { Text("Use default") }
                } else {
                    TextButton(onClick = closePanel) { Text("Cancel") }
                }
            }
        )
        SettingsPanel.SERVER -> ServerConnectionDialog(
            serverUrl = prefs.serverUrl,
            diagnostics = diagnostics,
            onSave = viewModel::setServerUrl,
            onDismiss = closePanel
        )
        SettingsPanel.ADVANCED -> AlertDialog(
            onDismissRequest = closePanel,
            title = { Text("Advanced") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    SettingsRow("Simultaneous downloads", HugeIcons.Download, prefs.maxConcurrentDownloads.toString()) {
                        panel = SettingsPanel.TRANSFERS
                    }
                    SettingsDivider()
                    SettingsRow("Clear preview cache", HugeIcons.Storage) { panel = SettingsPanel.CACHE }
                    SettingsDivider()
                    SettingsRow("Reset preferences", HugeIcons.Refresh) { panel = SettingsPanel.RESET }
                }
            },
            confirmButton = { TextButton(onClick = closePanel) { Text("Done") } }
        )
        SettingsPanel.TRANSFERS -> SettingsChoiceDialog(
            "Simultaneous downloads",
            (1..5).toList(),
            prefs.maxConcurrentDownloads,
            Int::toString,
            onDismiss = { panel = SettingsPanel.ADVANCED }
        ) {
            viewModel.setMaxConcurrent(it)
            panel = SettingsPanel.ADVANCED
        }
        SettingsPanel.CACHE -> AlertDialog(
            onDismissRequest = { panel = SettingsPanel.ADVANCED },
            title = { Text("Clear preview cache?") },
            text = { Text("Removes temporary thumbnails. Your downloaded files stay saved.") },
            confirmButton = {
                TextButton(onClick = {
                    panel = SettingsPanel.ADVANCED
                    scope.launch {
                        val loader = context.imageLoader
                        val cleared = runCatching {
                            loader.memoryCache?.clear()
                            withContext(Dispatchers.IO) { loader.diskCache?.clear() }
                        }
                        MediaActions.message(context, if (cleared.isSuccess) "Preview cache cleared" else "Could not clear the preview cache")
                    }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { panel = SettingsPanel.ADVANCED }) { Text("Cancel") } }
        )
        SettingsPanel.RESET -> AlertDialog(
            onDismissRequest = { panel = SettingsPanel.ADVANCED },
            title = { Text("Reset preferences?") },
            text = { Text("Restores the default settings. Your downloads and saved links stay saved.") },
            confirmButton = { TextButton(onClick = { viewModel.resetPreferences(); closePanel() }) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { panel = SettingsPanel.ADVANCED }) { Text("Cancel") } }
        )
        null -> Unit
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(start = 58.dp, end = 14.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SettingsRow(title: String, icon: ImageVector, value: String? = null, onClick: () -> Unit) {
    SettingsRow(title, icon, Modifier.clickable(onClick = onClick), value) {
        Icon(
            HugeIcons.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier,
    value: String? = null,
    trailing: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val accent = if (CpdColor.isDark) Color(0xFFFE5222) else Color(0xFFB43A17)
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).clip(shape).background(accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = accent)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (!value.isNullOrBlank()) {
                Text(
                    value,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing()
    }
}

@Composable
private fun <T> SettingsChoiceDialog(
    title: String,
    choices: List<T>,
    selected: T,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                choices.forEach { choice ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                            selected = choice == selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(choice) }
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = choice == selected, onClick = null)
                        Text(label(choice), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
