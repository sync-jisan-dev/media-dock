package com.mediadock.app.feature.workspace

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediadock.app.core.common.MediaActions
import com.mediadock.app.core.datastore.*
import com.mediadock.app.core.designsystem.components.*
import com.mediadock.app.core.designsystem.icons.HugeIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString

@Composable fun SavedLinksScreen(onBrowse: (String) -> Unit, onDownload: (String) -> Unit, viewModel: WorkspaceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var recent by rememberSaveable { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
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
            OutlinedTextField(text, { text = it.take(50000) }, Modifier.fillMaxWidth(), minLines = 4, maxLines = 8, placeholder = { Text("https://…") })
            Text("${WorkspaceRules.extractLinks(text).size} valid links", style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = {
                val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty().take(50000)
            }) { Icon(HugeIcons.ContentPaste, null); Spacer(Modifier.width(8.dp)); Text("Paste clipboard") }
        }
    }, confirmButton = { TextButton(onClick = { viewModel.add(text); addDialog = false; text = "" }, enabled = WorkspaceRules.extractLinks(text).isNotEmpty()) { Text("Save links") } }, dismissButton = { TextButton(onClick = { addDialog = false }) { Text("Cancel") } })
    if (editLink != null) EditTextDialog("Edit link title", editTitle, { editTitle = it }, { editLink = null }, { viewModel.renameLink(editLink!!.url, editTitle); editLink = null })
    if (clearRecent) AlertDialog(onDismissRequest = { clearRecent = false }, title = { Text("Clear recent pages?") }, text = { Text("Saved links and downloads will stay on your device.") }, confirmButton = { TextButton(onClick = { viewModel.clearRecent(); clearRecent = false }) { Text("Clear") } }, dismissButton = { TextButton(onClick = { clearRecent = false }) { Text("Cancel") } })
    val links = (if (recent) state.recentPages else state.links.sortedByDescending { it.pinned })
        .filter { it.title.contains(query, true) || it.url.contains(query, true) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionHeading("A place for every link", "Save now. Download when you’re ready.") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { StatTile(state.links.size.toString(), "Saved links", Modifier.weight(1f)); StatTile(state.recentPages.size.toString(), "Recent pages", Modifier.weight(1f)) } }
        item { Button(onClick = { addDialog = true }, Modifier.fillMaxWidth().height(48.dp)) { Icon(HugeIcons.ContentPaste, null); Spacer(Modifier.width(8.dp)); Text("Add links") } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { export.launch("MediaDock-links.json") }, Modifier.weight(1f)) { Text("Export JSON") }
            OutlinedButton(onClick = { import.launch(arrayOf("application/json", "text/plain")) }, Modifier.weight(1f)) { Text("Import JSON") }
        } }
        item { MediaDockPillTabs(listOf("Saved", "Recent"), if (recent) 1 else 0, { recent = it == 1 }) }
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search titles or links") }, leadingIcon = { Icon(HugeIcons.Search, null) }, singleLine = true, shape = RoundedCornerShape(16.dp)) }
        if (recent && state.recentPages.isNotEmpty()) item { TextButton(onClick = { clearRecent = true }) { Text("Clear recent pages") } }
        if (links.isEmpty()) item { Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.fillMaxWidth().padding(24.dp)) { Icon(HugeIcons.FolderOpen, null, Modifier.size(32.dp)); Spacer(Modifier.height(12.dp)); Text(if (query.isBlank()) "Your next discovery starts here" else "No matching links", style = MaterialTheme.typography.titleMedium); Text(if (recent) "Pages you visit in Browse will appear here." else "Tap Add links or bookmark a page in Browse.", style = MaterialTheme.typography.bodySmall) } } }
        items(links, key = { it.url }) { link ->
            var menu by remember { mutableStateOf(false) }
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text(link.title.ifBlank { runCatching { java.net.URI(link.url).host }.getOrDefault("Saved link") }, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(link.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        if (!recent) IconButton(onClick = { viewModel.pin(link.url) }) { Icon(HugeIcons.PushPin, if (link.pinned) "Unpin link" else "Pin link", tint = if (link.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) }
                        Box { IconButton(onClick = { menu = true }) { Icon(HugeIcons.MoreVert, "Link actions") }
                            DropdownMenu(menu, { menu = false }) {
                                if (!recent) DropdownMenuItem(text = { Text("Edit title") }, onClick = { editLink = link; editTitle = link.title; menu = false })
                                if (recent) DropdownMenuItem(text = { Text("Save link") }, onClick = { viewModel.save(link.url, link.title); menu = false })
                                DropdownMenuItem(text = { Text("Copy link") }, onClick = { MediaActions.copy(context, link.url); menu = false })
                                DropdownMenuItem(text = { Text("Share link") }, onClick = { MediaActions.shareText(context, link.url); menu = false })
                                if (!recent) DropdownMenuItem(text = { Text("Remove saved link") }, onClick = { viewModel.removeLink(link.url); menu = false })
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { onBrowse(link.url) }, Modifier.weight(1f)) { Text("Browse") }
                        Button(onClick = { onDownload(link.url) }, Modifier.weight(1f)) { Icon(HugeIcons.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Download") }
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
