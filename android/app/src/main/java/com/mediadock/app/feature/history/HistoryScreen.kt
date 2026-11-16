package com.mediadock.app.feature.history

import com.mediadock.app.core.designsystem.icons.HugeIcons

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.core.designsystem.components.MediaDockEmptyState
import com.mediadock.app.core.designsystem.components.MediaDockPillTabs

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
    val files by viewModel.historyItems.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.selectedMediaType.collectAsState()
    val context = LocalContext.current
    var clearConfirm by remember { mutableStateOf(false) }
    if (clearConfirm) AlertDialog(
        onDismissRequest = { clearConfirm = false },
        title = { Text("Clear download history?") },
        text = { Text("Files saved on your device will stay available in your device’s file manager.") },
        confirmButton = { TextButton(onClick = { viewModel.clearAllHistory(); clearConfirm = false }) { Text("Clear history") } },
        dismissButton = { TextButton(onClick = { clearConfirm = false }) { Text("Cancel") } }
    )
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(HugeIcons.FolderCopy, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Saved and ready to play", style = MaterialTheme.typography.titleMedium)
                    Text("${files.size} files shown · ${FormattingUtils.formatBytes(files.sumOf { it.fileSizeBytes })}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { clearConfirm = true }, enabled = files.isNotEmpty()) { Icon(HugeIcons.DeleteSweep, "Clear history") }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(query, viewModel::onSearchChange, Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("Search your downloads") }, leadingIcon = { Icon(HugeIcons.Search, null) },
            shape = RoundedCornerShape(16.dp))
        val filters = listOf("ALL" to "All files", "video" to "Videos", "audio" to "Music", "image" to "Images", "gif" to "GIFs")
        MediaDockPillTabs(filters.map { it.second }, filters.indexOfFirst { it.first == filter }.coerceAtLeast(0), { viewModel.selectFilter(filters[it].first) }, Modifier.padding(vertical = 12.dp))
        if (files.isEmpty()) {
            MediaDockEmptyState(HugeIcons.VideoLibrary, "Your offline collection", "Videos, music and images you download will appear here.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(files, key = { it.id }) { file ->
                    val mime = when (file.mediaType.lowercase()) { "audio" -> "audio/*"; "image", "gif" -> "image/*"; else -> "video/*" }
                    fun launch(intent: Intent) {
                        try { context.startActivity(intent) } catch (_: Exception) { Toast.makeText(context, "Unable to open this file. Check that it is still on your device.", Toast.LENGTH_LONG).show() }
                    }
                    FileCard(file,
                        onOpen = { launch(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(file.localUri), mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) },
                        onShare = { launch(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, Uri.parse(file.localUri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), "Share media")) },
                        onDelete = { viewModel.deleteItem(file.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FileCard(file: HistoryEntity, onOpen: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(onClick = onOpen, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(88.dp, 72.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(if (file.mediaType == "audio") HugeIcons.Album else HugeIcons.PlayCircle, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary)
                if (file.mediaType != "audio") AsyncImage(file.localUri, file.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${file.format.uppercase()} · ${FormattingUtils.formatBytes(file.fileSizeBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(HugeIcons.CheckCircle, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Downloaded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(HugeIcons.MoreVert, "File actions") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Play / open") }, onClick = { menu = false; onOpen() }, leadingIcon = { Icon(HugeIcons.PlayArrow, null) })
                    DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; onShare() }, leadingIcon = { Icon(HugeIcons.Share, null) })
                    DropdownMenuItem(text = { Text("Remove from history") }, onClick = { menu = false; onDelete() }, leadingIcon = { Icon(HugeIcons.DeleteOutline, null) })
                }
            }
        }
    }
}
