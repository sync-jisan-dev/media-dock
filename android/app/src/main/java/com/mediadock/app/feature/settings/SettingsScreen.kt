package com.mediadock.app.feature.settings

import com.mediadock.app.core.designsystem.components.BrandButton
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import com.mediadock.app.core.designsystem.theme.*
import com.mediadock.app.BuildConfig
import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.common.MediaActions
import com.mediadock.app.core.datastore.ThemeMode
import com.mediadock.app.core.datastore.WorkspaceRules
import com.mediadock.app.core.designsystem.components.*
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.network.ApiConstants

@Composable fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier, onSavedLinks: () -> Unit = {}, diagnostics: DiagnosticsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsState()
    val library by diagnostics.library.collectAsState()
    val storage by diagnostics.storage.collectAsState()
    val checking by diagnostics.checking.collectAsState()
    val connection by diagnostics.connection.collectAsState()
    val context = LocalContext.current
    var serverDialog by remember { mutableStateOf(false) }
    var server by remember { mutableStateOf("") }
    var reset by remember { mutableStateOf(false) }
    var concurrency by remember(prefs.maxConcurrentDownloads) { mutableStateOf(prefs.maxConcurrentDownloads.toFloat()) }
    val directory = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) try {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            viewModel.setCustomDownloadDir(uri.toString())
        } catch (_: Exception) { MediaActions.message(context, "This folder cannot be used. Choose another folder.") }
    }
    if (serverDialog) AlertDialog(onDismissRequest = { serverDialog = false }, title = { Text("Backend connection") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Use the HTTPS address of your cloud backend.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(server, { server = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Server URL") }, isError = server.isNotBlank() && !WorkspaceRules.validUrl(server))
        }
    }, confirmButton = { TextButton(onClick = { viewModel.setServerUrl(server.trim()); serverDialog = false }, enabled = WorkspaceRules.validUrl(server)) { Text("Save") } }, dismissButton = { TextButton(onClick = { serverDialog = false }) { Text("Cancel") } })
    if (reset) AlertDialog(onDismissRequest = { reset = false }, title = { Text("Reset preferences?") }, text = { Text("Restores theme, download defaults, folder and cloud server. Your files, collections and saved links stay saved.") }, confirmButton = { TextButton(onClick = { viewModel.resetPreferences(); reset = false }) { Text("Reset") } }, dismissButton = { TextButton(onClick = { reset = false }) { Text("Cancel") } })
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { SectionHeading("Make it yours", "A quieter space for your media.") }
        item { SettingsCard("Appearance", HugeIcons.DarkMode) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { mode -> FilterChip(prefs.themeMode == mode, { viewModel.setThemeMode(mode) }, { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }) } }
        } }
        item { SettingsCard("Download defaults", HugeIcons.Download) {
            Text("Preferred video quality", style = MaterialTheme.typography.labelLarge)
            MediaDockPillTabs(listOf("360p", "720p", "1080p", "Highest"), listOf("360p", "720p", "1080p", "highest").indexOf(prefs.defaultVideoQuality).coerceAtLeast(0), { viewModel.setDefaultVideoQuality(listOf("360p", "720p", "1080p", "highest")[it]) })
            Text("Preferred audio format", style = MaterialTheme.typography.labelLarge)
            MediaDockPillTabs(listOf("MP3", "M4A", "OPUS", "FLAC"), listOf("mp3", "m4a", "opus", "flac").indexOf(prefs.defaultAudioFormat).coerceAtLeast(0), { viewModel.setDefaultAudioFormat(listOf("mp3", "m4a", "opus", "flac")[it]) })
            Text("Preferences apply when the source supports them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Download on Wi-Fi only", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Switch(prefs.wifiOnlyDownloading, viewModel::setWifiOnly) }
            Text("Simultaneous transfers · ${concurrency.toInt()}", style = MaterialTheme.typography.bodyMedium)
            Slider(concurrency, { concurrency = it }, valueRange = 1f..5f, steps = 3, onValueChangeFinished = { viewModel.setMaxConcurrent(concurrency.toInt()) })
            OutlinedButton(onClick = { directory.launch(null) }, Modifier.fillMaxWidth()) { Icon(HugeIcons.Folder, null); Spacer(Modifier.width(8.dp)); Text(if (prefs.customDownloadDirectoryUri == null) "Choose download folder" else "Change download folder") }
            if (prefs.customDownloadDirectoryUri != null) TextButton(onClick = { viewModel.setCustomDownloadDir(null) }) { Text("Use default media folder") }
        } }
        item { SettingsCard("Storage overview", HugeIcons.FolderOpen) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatTile(FormattingUtils.formatBytes(storage.first), "Device free", Modifier.weight(1f)); StatTile(FormattingUtils.formatBytes(library.sumOf { it.fileSizeBytes }), "Library size", Modifier.weight(1f)) }
            if (storage.second > 0) LinearProgressIndicator(progress = { (1f - storage.first.toFloat() / storage.second).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
            Text("${library.size} library files · ${FormattingUtils.formatBytes(storage.second)} device capacity", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { diagnostics.refreshStorage() }) { Text("Refresh storage") }
        } }
        item { SettingsCard("Cloud connection", HugeIcons.Cloud) {
            Text(prefs.serverUrl, style = MaterialTheme.typography.bodySmall)
            Text(connection, style = MaterialTheme.typography.bodyMedium)
            if (checking) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandButton(onClick = diagnostics::check, enabled = !checking) { Text(if (checking) "Checking…" else "Test connection") }
                OutlinedButton(onClick = { server = prefs.serverUrl; serverDialog = true }) { Text("Edit URL") }
            }
            TextButton(onClick = { viewModel.setServerUrl(ApiConstants.DEFAULT_BASE_URL) }) { Text("Use default cloud server") }
            Text("Free hosting may sleep when idle. Availability depends on your host and provider limits.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        item { SettingsCard("Your workspace", HugeIcons.Collections) {
            OutlinedButton(onClick = onSavedLinks, Modifier.fillMaxWidth()) { Text("Manage saved links & recent pages") }
            TextButton(onClick = { val host = runCatching { java.net.URI(prefs.serverUrl).host }.getOrNull().orEmpty(); MediaActions.shareText(context, "MediaDock ${BuildConfig.VERSION_NAME}\nAndroid ${android.os.Build.VERSION.RELEASE}\nBackend host: $host\nConnection: $connection\nDevice free: ${FormattingUtils.formatBytes(storage.first)}\nLibrary files: ${library.size}") }) { Text("Share connection report") }
            TextButton(onClick = { reset = true }) { Text("Reset preferences") }
        } }
        item { Text("MediaDock ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleSmall); Text("Made for your media. Save content you have permission to download.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable private fun SettingsCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.clip(RoundedCornerShape(10.dp)).background(BrandCoralOrange).padding(8.dp).size(20.dp), tint = BrandWhite); Spacer(Modifier.width(12.dp)); Text(title, style = MaterialTheme.typography.titleMedium, color = BrandTextPrimary) }
        content()
    } }
}
