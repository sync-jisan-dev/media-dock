package com.mediadock.app.feature.history

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.common.MediaActions
import com.mediadock.app.core.database.entity.HistoryEntity
import com.mediadock.app.core.datastore.*
import com.mediadock.app.core.designsystem.components.*
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.feature.workspace.WorkspaceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.text.DateFormat
import java.util.Date

@Composable fun HistoryScreen(viewModel: HistoryViewModel, modifier: Modifier = Modifier, onBrowse: (String) -> Unit = { }, workspace: WorkspaceViewModel = hiltViewModel()) {
    val allFiles by viewModel.historyItems.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.selectedMediaType.collectAsState()
    val state by workspace.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var favorites by rememberSaveable { mutableStateOf(false) }
    var collectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selection by remember { mutableStateOf(setOf<Long>()) }
    var sortMenu by remember { mutableStateOf(false) }
    var collectionDialog by remember { mutableStateOf(false) }
    var collectionEdit by remember { mutableStateOf<MediaCollection?>(null) }
    var collectionName by remember { mutableStateOf("") }
    var createCollection by remember { mutableStateOf(false) }
    var deleteCollection by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf<HistoryEntity?>(null) }
    var editFile by remember { mutableStateOf<HistoryEntity?>(null) }
    var editNote by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var remove by remember { mutableStateOf<List<HistoryEntity>>(emptyList()) }
    val files = LibraryRules.select(allFiles, state, query, filter, favorites, collectionId)
    val selectedFiles = allFiles.filter { it.id in selection }
    LaunchedEffect(allFiles) { selection = selection.intersect(allFiles.map { it.id }.toSet()) }
    BackHandler(selectionMode) { selectionMode = false; selection = emptySet() }
    fun favorite(ids: Set<Long>) = workspace.edit { it.copy(favorites = if (it.favorites.containsAll(ids)) it.favorites - ids else it.favorites + ids) }
    fun open(file: HistoryEntity) {
        val mime = when (file.mediaType.lowercase()) { "audio" -> "audio/*"; "image", "gif" -> "image/*"; else -> "video/*" }
        MediaActions.launch(context, Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(file.localUri), mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val payload = buildJsonArray { (if (selectionMode) selectedFiles else files).forEach { file -> add(buildJsonObject {
                put("title", LibraryRules.title(file, state)); put("sourceUrl", file.sourceUrl); put("mediaType", file.mediaType); put("format", file.format)
                put("sizeBytes", file.fileSizeBytes); put("downloadedAt", file.downloadedAt); put("favorite", file.id in state.favorites); put("note", state.notes[file.id.toString()].orEmpty())
            }) } }.toString()
            val result = withContext(Dispatchers.IO) { runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(payload.toByteArray()) } ?: error("Cannot open file") } }
            MediaActions.message(context, if (result.isSuccess) "Library manifest exported" else "Export failed")
        }
    }
    if (editFile != null) AlertDialog(onDismissRequest = { editFile = null }, title = { Text(if (editNote) "Edit note" else "Edit display title") }, text = {
        Column { if (!editNote) Text("This changes the title in MediaDock. The saved file keeps its filename.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(editText, { editText = it.take(if (editNote) 2000 else 200) }, Modifier.fillMaxWidth(), minLines = if (editNote) 3 else 1) }
    }, confirmButton = { TextButton(onClick = {
        val key = editFile!!.id.toString()
        workspace.edit { if (editNote) it.copy(notes = if (editText.isBlank()) it.notes - key else it.notes + (key to editText.trim())) else it.copy(titles = if (editText.isBlank()) it.titles - key else it.titles + (key to editText.trim())) }
        editFile = null
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = { editFile = null }) { Text("Cancel") } })
    if (createCollection || collectionEdit != null) EditTextDialog(if (collectionEdit == null) "New collection" else "Rename collection", collectionName, { collectionName = it.take(60) }, { createCollection = false; collectionEdit = null }, {
        val editingId = collectionEdit?.id; val name = collectionName.trim()
        workspace.edit { current -> current.copy(collections = if (editingId == null) current.collections + MediaCollection(name = name) else current.collections.map { if (it.id == editingId) it.copy(name = name) else it }) }
        createCollection = false; collectionEdit = null
    })
    if (collectionDialog) AlertDialog(onDismissRequest = { collectionDialog = false }, title = { Text("Add to collection") }, text = {
        Column { if (state.collections.isEmpty()) Text("Create a collection using New collection first.")
            state.collections.forEach { col -> TextButton(onClick = { workspace.edit { it.copy(collections = it.collections.map { if (it.id == col.id) it.copy(fileIds = it.fileIds + selection) else it }) }; collectionDialog = false; MediaActions.message(context, "Added to ${col.name}") }) { Text(col.name) } }
        }
    }, confirmButton = { TextButton(onClick = { collectionDialog = false }) { Text("Done") } })
    if (deleteCollection) AlertDialog(onDismissRequest = { deleteCollection = false }, title = { Text("Delete this collection?") }, text = { Text("The collection is removed. Your files stay saved.") }, confirmButton = { TextButton(onClick = { val deletedId = collectionId; workspace.edit { it.copy(collections = it.collections.filterNot { it.id == deletedId }) }; collectionId = null; deleteCollection = false }) { Text("Delete collection") } }, dismissButton = { TextButton(onClick = { deleteCollection = false }) { Text("Cancel") } })
    if (remove.isNotEmpty()) AlertDialog(onDismissRequest = { remove = emptyList() }, title = { Text("Remove ${remove.size} from library?") }, text = { Text("The downloaded files stay on your device. You can undo this change.") }, confirmButton = { TextButton(onClick = {
        val removed = remove; remove = emptyList(); selection = emptySet(); selectionMode = false
        scope.launch { viewModel.removeItems(removed); if (snackbar.showSnackbar("Removed from library", "Undo", duration = SnackbarDuration.Long) == SnackbarResult.ActionPerformed) viewModel.restoreItems(removed) }
    }) { Text("Remove") } }, dismissButton = { TextButton(onClick = { remove = emptyList() }) { Text("Cancel") } })
    if (details != null) AlertDialog(onDismissRequest = { details = null }, title = { Text("File details") }, text = {
        val file = details!!
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(LibraryRules.title(file, state), style = MaterialTheme.typography.titleSmall)
            Text("${file.format.uppercase()} · ${FormattingUtils.formatBytes(file.fileSizeBytes)}")
            Text(DateFormat.getDateTimeInstance().format(Date(file.downloadedAt)), style = MaterialTheme.typography.bodySmall)
            Text(file.sourceUrl, style = MaterialTheme.typography.bodySmall)
            Text(file.localUri, style = MaterialTheme.typography.bodySmall)
            if (state.notes[file.id.toString()].orEmpty().isNotBlank()) Text(state.notes[file.id.toString()].orEmpty())
            TextButton(onClick = { MediaActions.copy(context, file.sourceUrl) }) { Text("Copy source link") }
        }
    }, confirmButton = { TextButton(onClick = { details = null }) { Text("Done") } })
    Box(modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { SectionHeading("Your offline collection", "Organized by you. Ready whenever.", action = { TextButton(onClick = { selectionMode = !selectionMode; selection = emptySet() }) { Text(if (selectionMode) "Done" else "Select") } }) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatTile(allFiles.size.toString(), "Files", Modifier.weight(1f)); StatTile(FormattingUtils.formatBytes(allFiles.sumOf { it.fileSizeBytes }), "Saved media", Modifier.weight(1f)) } }
            item { OutlinedTextField(query, viewModel::onSearchChange, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search titles, formats or notes") }, leadingIcon = { Icon(HugeIcons.Search, null) }, shape = RoundedCornerShape(16.dp)) }
            item { val types = listOf("ALL" to "All", "video" to "Videos", "audio" to "Music", "image" to "Images", "gif" to "GIFs"); MediaDockPillTabs(types.map { it.second }, types.indexOfFirst { it.first == filter }.coerceAtLeast(0), { viewModel.selectFilter(types[it].first) }) }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(favorites, { favorites = !favorites }, { Text("Favorites") }, leadingIcon = { Icon(HugeIcons.PushPin, null, Modifier.size(16.dp)) })
                Box { TextButton(onClick = { sortMenu = true }) { Text(state.librarySort) }; DropdownMenu(sortMenu, { sortMenu = false }) { LibraryRules.sorts.forEach { sort -> DropdownMenuItem(text = { Text(sort) }, onClick = { workspace.edit { it.copy(librarySort = sort) }; sortMenu = false }) } } }
                IconButton(onClick = { workspace.edit { it.copy(grid = !it.grid) } }) { Icon(if (state.grid) HugeIcons.FolderOpen else HugeIcons.Collections, if (state.grid) "List view" else "Grid view") }
            } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { createCollection = true; collectionName = "" }) { Text("New collection") }
                TextButton(onClick = { workspace.edit { it.copy(compact = !it.compact) } }) { Text(if (state.compact) "Comfortable" else "Compact rows") }
                TextButton(onClick = { export.launch("MediaDock-library.json") }, enabled = files.isNotEmpty()) { Icon(HugeIcons.Share, "Export library manifest", Modifier.size(20.dp)) }
            } }
            if (state.collections.isNotEmpty()) item { Column {
                MediaDockPillTabs(listOf("All collections") + state.collections.map { it.name }, (state.collections.indexOfFirst { it.id == collectionId } + 1).coerceAtLeast(0), { collectionId = if (it == 0) null else state.collections[it - 1].id })
                if (collectionId != null) Row { TextButton(onClick = { collectionEdit = state.collections.find { it.id == collectionId }; collectionName = collectionEdit?.name.orEmpty() }) { Text("Rename") }; TextButton(onClick = { deleteCollection = true }) { Text("Delete collection") } }
            } }
            if (selectionMode) item { Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("${selection.size} selected", style = MaterialTheme.typography.titleSmall)
                    Row { TextButton(onClick = { selection = files.map { it.id }.toSet() }) { Text("Select all") }; TextButton(onClick = { selection = emptySet() }) { Text("Clear") }; TextButton(onClick = { favorite(selection) }, enabled = selection.isNotEmpty()) { Text("Favorite") } }
                    Row { TextButton(onClick = { MediaActions.shareFiles(context, selectedFiles.map { Uri.parse(it.localUri) }) }, enabled = selection.isNotEmpty()) { Text("Share") }; TextButton(onClick = { collectionDialog = true }, enabled = selection.isNotEmpty()) { Text("Collect") }; TextButton(onClick = { remove = selectedFiles }, enabled = selection.isNotEmpty()) { Text("Remove") } }
                    if (collectionId != null) TextButton(onClick = { val ids = selection; val currentCollection = collectionId; workspace.edit { it.copy(collections = it.collections.map { if (it.id == currentCollection) it.copy(fileIds = it.fileIds - ids) else it }) }; selection = emptySet() }, enabled = selection.isNotEmpty()) { Text("Remove from this collection") }
                }
            } }
            if (files.isEmpty()) item { Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.fillMaxWidth().padding(24.dp)) { Icon(HugeIcons.VideoLibrary, null, Modifier.size(36.dp)); Spacer(Modifier.height(12.dp)); Text(if (allFiles.isEmpty()) "Your library starts here" else "No matching files", style = MaterialTheme.typography.titleMedium); Text(if (allFiles.isEmpty()) "Download media to see it in your collection." else "Try another filter, collection or search.", style = MaterialTheme.typography.bodySmall) } } }
            items(files.chunked(if (state.grid) 2 else 1), key = { it.first().id }) { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { file -> LibraryCard(file, LibraryRules.title(file, state), file.id in state.favorites, selectionMode, file.id in selection, state.grid, state.compact, Modifier.weight(1f),
                    onOpen = { if (selectionMode) selection = if (file.id in selection) selection - file.id else selection + file.id else open(file) },
                    onLongClick = { selectionMode = true; selection = selection + file.id }, onFavorite = { favorite(setOf(file.id)) }, onAction = { action -> when (action) {
                        "Details" -> details = file
                        "Edit title" -> { editFile = file; editNote = false; editText = LibraryRules.title(file, state) }
                        "Edit note" -> { editFile = file; editNote = true; editText = state.notes[file.id.toString()].orEmpty() }
                        "Copy source link" -> MediaActions.copy(context, file.sourceUrl)
                        "Open source" -> onBrowse(file.sourceUrl)
                        "Share file" -> MediaActions.shareFiles(context, listOf(Uri.parse(file.localUri)))
                        "Remove from library" -> remove = listOf(file)
                    } }) }
                if (state.grid && row.size == 1) Spacer(Modifier.weight(1f))
            } }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable private fun LibraryCard(file: HistoryEntity, title: String, favorite: Boolean, selecting: Boolean, selected: Boolean, grid: Boolean, compact: Boolean, modifier: Modifier, onOpen: () -> Unit, onLongClick: () -> Unit, onFavorite: () -> Unit, onAction: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Surface(modifier.combinedClickable(onClick = onOpen, onLongClick = onLongClick), shape = RoundedCornerShape(18.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(if (compact) 10.dp else 14.dp)) {
            if (grid) { Icon(if (file.mediaType == "audio") HugeIcons.Headphones else HugeIcons.PlayCircle, null, Modifier.fillMaxWidth().height(if (compact) 44.dp else 72.dp).padding(8.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(8.dp)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selecting) Checkbox(selected, { onOpen() })
                else if (!grid) { Icon(if (file.mediaType == "audio") HugeIcons.Headphones else HugeIcons.PlayCircle, null, Modifier.size(if (compact) 32.dp else 44.dp)); Spacer(Modifier.width(12.dp)) }
                Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleSmall, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis); Text("${file.format.uppercase()} · ${FormattingUtils.formatBytes(file.fileSizeBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                if (!grid && !compact) Text(DateFormat.getDateInstance(DateFormat.SHORT).format(Date(file.downloadedAt)), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onFavorite, Modifier.size(40.dp)) { Icon(HugeIcons.PushPin, if (favorite) "Remove favorite" else "Favorite file", tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) }
                Box { IconButton(onClick = { menu = true }, Modifier.size(40.dp)) { Icon(HugeIcons.MoreVert, "File actions") }; DropdownMenu(menu, { menu = false }) { listOf("Details", "Edit title", "Edit note", "Copy source link", "Open source", "Share file", "Remove from library").forEach { action -> DropdownMenuItem(text = { Text(action) }, onClick = { menu = false; onAction(action) }) } } }
            }
        }
    }
}
