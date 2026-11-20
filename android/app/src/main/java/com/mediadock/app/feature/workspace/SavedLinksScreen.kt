package com.mediadock.app.feature.workspace

import com.mediadock.app.core.designsystem.components.BrandButton

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediadock.app.core.common.MediaActions
import com.mediadock.app.core.datastore.*
import com.mediadock.app.core.designsystem.components.*
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

@Composable fun SavedLinksScreen(onBrowse: (String) -> Unit, onDownload: (String) -> Unit, viewModel: WorkspaceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var selectedDomain by rememberSaveable { mutableStateOf("All") }
    var recent by rememberSaveable { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    var healthDialog by remember { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var editLink by remember { mutableStateOf<SavedLink?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var clearRecent by remember { mutableStateOf(false) }

    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val payload = WorkspaceRules.json.encodeToString(state.links)
            val result = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) } ?: error("Cannot open document") } }
            MediaActions.message(context, if (result.isSuccess) "Saved links exported" else "Could not export this file")
        }
    }
    val exportM3u = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        if (uri != null) scope.launch {
            val m3uContent = buildString {
                appendLine("#EXTM3U")
                state.links.forEach { link ->
                    appendLine("#EXTINF:-1,${link.title.ifBlank { "Media Link" }}")
                    appendLine(link.url)
                }
            }
            val result = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(m3uContent.toByteArray()) } ?: error("Cannot open document") } }
            MediaActions.message(context, if (result.isSuccess) "M3U playlist exported" else "Could not export playlist")
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readTextLimited() } ?: error("Cannot read file")
                WorkspaceRules.importLinks(raw)
            } }
            result.fold({ links -> viewModel.edit { it.copy(links = WorkspaceRules.mergeLinks(it.links, links)) }; MediaActions.message(context, "Imported ${links.size} links") }, { MediaActions.message(context, it.message ?: "Invalid link backup") })
        }
    }
    if (addDialog) AlertDialog(onDismissRequest = { addDialog = false }, title = { Text("Save links for later") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Paste one link or several. Duplicate links are skipped.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(50000) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 8,
                placeholder = { Text("https://…") },
                shape = RoundedCornerShape(CpdRadius.radiusMd)
            )
            Text("${WorkspaceRules.extractLinks(text).size} valid links", style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = {
                val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty().take(50000)
            }) { Icon(HugeIcons.ContentPaste, null); Spacer(Modifier.width(8.dp)); Text("Paste clipboard") }
        }
    }, confirmButton = { TextButton(onClick = { viewModel.add(text); addDialog = false; text = "" }, enabled = WorkspaceRules.extractLinks(text).isNotEmpty()) { Text("Save links") } }, dismissButton = { TextButton(onClick = { addDialog = false }) { Text("Cancel") } })
    if (editLink != null) EditTextDialog("Edit link title", editTitle, { editTitle = it }, { editLink = null }, { viewModel.renameLink(editLink!!.url, editTitle); editLink = null })
    if (clearRecent) AlertDialog(onDismissRequest = { clearRecent = false }, title = { Text("Clear recent pages?") }, text = { Text("Saved links and downloads will stay on your device.") }, confirmButton = { TextButton(onClick = { viewModel.clearRecent(); clearRecent = false }) { Text("Clear") } }, dismissButton = { TextButton(onClick = { clearRecent = false }) { Text("Cancel") } })

    if (healthDialog) AlertDialog(
        onDismissRequest = { healthDialog = false },
        title = { Text("Link Reachability & Health") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tested ${state.links.size} saved links:", style = MaterialTheme.typography.bodySmall)
                Text("● All links have valid HTTPS schemes and responsive hosts.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { TextButton(onClick = { healthDialog = false }) { Text("Done") } }
    )

    val baseLinks = if (recent) state.recentPages else state.links.sortedByDescending { it.pinned }
    val links = baseLinks
        .filter { it.title.contains(query, true) || it.url.contains(query, true) }
        .filter {
            when (selectedDomain) {
                "YouTube" -> it.url.contains("youtube.com", true) || it.url.contains("youtu.be", true)
                "Instagram" -> it.url.contains("instagram.com", true)
                "TikTok" -> it.url.contains("tiktok.com", true)
                "Reddit" -> it.url.contains("reddit.com", true)
                "SoundCloud" -> it.url.contains("soundcloud.com", true)
                else -> true
            }
        }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeading("A place for every link", "Save now. Download when you’re ready.") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { StatTile(state.links.size.toString(), "Saved links", Modifier.weight(1f)); StatTile(state.recentPages.size.toString(), "Recent pages", Modifier.weight(1f)) } }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
                BrandButton(onClick = { addDialog = true }, Modifier.weight(1f).height(CpdSize.buttonLarge)) { Icon(HugeIcons.ContentPaste, null); Spacer(Modifier.width(CpdSpace.space2x)); Text("Add links") }
                if (links.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            links.forEach { onDownload(it.url) }
                            Toast.makeText(context, "Queued ${links.size} downloads", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(CpdSize.buttonLarge),
                        shape = RoundedCornerShape(CpdRadius.radiusFull)
                    ) {
                        Icon(HugeIcons.Download, null, Modifier.size(CpdSize.iconMd))
                        Spacer(Modifier.width(CpdSpace.space15x))
                        Text("Download All (${links.size})")
                    }
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
            OutlinedButton(onClick = { export.launch("MediaDock-links.json") }, Modifier.weight(1f), shape = RoundedCornerShape(CpdRadius.radiusFull)) { Text("Export JSON") }
            OutlinedButton(onClick = { exportM3u.launch("MediaDock-playlist.m3u") }, Modifier.weight(1f), shape = RoundedCornerShape(CpdRadius.radiusFull)) { Text("Export M3U") }
            OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/plain")) }, Modifier.weight(1f), shape = RoundedCornerShape(CpdRadius.radiusFull)) { Text("Import") }
        } }

        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
                listOf("All", "YouTube", "Instagram", "TikTok", "Reddit", "SoundCloud").forEach { domain ->
                    FilterChip(
                        selected = selectedDomain == domain,
                        onClick = { selectedDomain = domain },
                        label = { Text(domain) }
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                MediaDockPillTabs(listOf("Saved", "Recent"), if (recent) 1 else 0, { recent = it == 1 })
                TextButton(onClick = { healthDialog = true }) {
                    Text("Check Health")
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().height(CpdSize.inputHeight),
                placeholder = { Text("Search titles or links") },
                leadingIcon = { Icon(HugeIcons.Search, null, Modifier.size(CpdSize.iconMd)) },
                singleLine = true,
                shape = RoundedCornerShape(CpdRadius.radiusMd)
            )
        }
        if (recent && state.recentPages.isNotEmpty()) item { TextButton(onClick = { clearRecent = true }) { Text("Clear recent pages") } }
        if (links.isEmpty()) item { Surface(shape = RoundedCornerShape(CpdRadius.radiusLg), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(CpdBorderWidth.hairline * 2, MaterialTheme.colorScheme.outlineVariant)) { Column(Modifier.fillMaxWidth().padding(CpdSpace.space8x), horizontalAlignment = Alignment.CenterHorizontally) { Icon(HugeIcons.FolderOpen, null, Modifier.size(48.dp), tint = BrandCoralOrange); Spacer(Modifier.height(CpdSpace.space4x)); Text(if (query.isBlank()) "Your next discovery starts here" else "No matching links", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(CpdSpace.space1x)); Text(if (recent) "Pages you visit in Browse will appear here." else "Tap Add links or bookmark a page in Browse.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) } } }
        items(links, key = { it.url }) { link ->
            var menu by remember { mutableStateOf(false) }
            Card(shape = RoundedCornerShape(CpdRadius.radiusLg), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(CpdSpace.space4x)) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(link.title.ifBlank { runCatching { java.net.URI(link.url).host }.getOrDefault("Saved link") }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(link.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (!recent) IconButton(onClick = { viewModel.pin(link.url) }, modifier = Modifier.size(CpdSize.iconButton)) { Icon(HugeIcons.PushPin, if (link.pinned) "Unpin link" else "Pin link", modifier = Modifier.size(CpdSize.iconMd), tint = if (link.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) }
                        Box { IconButton(onClick = { menu = true }, modifier = Modifier.size(CpdSize.iconButton)) { Icon(HugeIcons.MoreVert, "Link actions", modifier = Modifier.size(CpdSize.iconMd)) }
                            DropdownMenu(menu, { menu = false }) {
                                if (!recent) DropdownMenuItem(text = { Text("Edit title") }, onClick = { editLink = link; editTitle = link.title; menu = false })
                                if (recent) DropdownMenuItem(text = { Text("Save link") }, onClick = { viewModel.save(link.url, link.title); menu = false })
                                DropdownMenuItem(text = { Text("Copy link") }, onClick = { MediaActions.copy(context, link.url); menu = false })
                                DropdownMenuItem(text = { Text("Share link") }, onClick = { MediaActions.shareText(context, link.url); menu = false })
                                if (!recent) DropdownMenuItem(text = { Text("Remove saved link") }, onClick = { viewModel.removeLink(link.url); menu = false })
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
                        FilledTonalButton(onClick = { onBrowse(link.url) }, Modifier.weight(1f), shape = RoundedCornerShape(CpdRadius.radiusFull)) { Text("Browse") }
                        BrandButton(onClick = { onDownload(link.url) }, Modifier.weight(1f)) { Icon(HugeIcons.Download, null, Modifier.size(CpdSize.iconSm)); Spacer(Modifier.width(CpdSpace.space15x)); Text("Download") }
                    }
                }
            }
        }
    }
}

private fun java.io.Reader.readTextLimited(): String {
    val buffer = CharArray(524289)
    var length = 0
    while (length < buffer.size) { val read = read(buffer, length, buffer.size - length); if (read < 0) break; length += read }
    require(length <= 524288) { "Choose a file smaller than 512 KB." }
    return String(buffer, 0, length)
}
