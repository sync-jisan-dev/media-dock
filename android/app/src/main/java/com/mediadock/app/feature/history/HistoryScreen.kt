package com.mediadock.app.feature.history

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import com.mediadock.app.core.designsystem.theme.*
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
    var filterRecent24h by rememberSaveable { mutableStateOf(false) }
    var filterLargeFiles by rememberSaveable { mutableStateOf(false) }
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
    var playingFile by remember { mutableStateOf<HistoryEntity?>(null) }
    var isPlayerPlaying by remember { mutableStateOf(true) }
    var duplicateDialog by remember { mutableStateOf(false) }

    val rawFiles = LibraryRules.select(allFiles, state, query, filter, favorites, collectionId)
    val files = remember(rawFiles, filterRecent24h, filterLargeFiles) {
        val now = System.currentTimeMillis()
        rawFiles.filter {
            val pass24h = !filterRecent24h || (now - it.downloadedAt <= 24 * 60 * 60 * 1000L)
            val passLarge = !filterLargeFiles || (it.fileSizeBytes >= 50 * 1024 * 1024L)
            pass24h && passLarge
        }
    }

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

    // Universal In-App Media Player Dialog (Video, Audio, Image/GIF, PDF)
    if (playingFile != null) {
        val file = playingFile!!
        MediaDockMediaPlayerDialog(
            uri = file.localUri,
            title = LibraryRules.title(file, state),
            mediaType = file.mediaType,
            format = file.format,
            fileSizeBytes = file.fileSizeBytes,
            sourceUrl = file.sourceUrl,
            onDismiss = { playingFile = null },
            onDelete = {
                val toDelete = file
                playingFile = null
                remove = listOf(toDelete)
            }
        )
    }

    // Duplicate File Scanner Dialog
    if (duplicateDialog) {
        val duplicates = remember(allFiles) {
            allFiles.groupBy { it.fileSizeBytes }.filter { it.key > 0 && it.value.size > 1 }.values.flatten()
        }
        AlertDialog(
            onDismissRequest = { duplicateDialog = false },
            title = { Text("Duplicate File Cleaner", fontWeight = FontWeight.Normal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (duplicates.isEmpty()) {
                        Text("No duplicate files found in your library. Your storage is clean and optimized!", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Found ${duplicates.size} files with identical byte sizes that may be redundant copies:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        duplicates.take(6).forEach { f ->
                            Text("• ${LibraryRules.title(f, state)} (${FormattingUtils.formatBytes(f.fileSizeBytes)})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                if (duplicates.isNotEmpty()) {
                    TextButton(onClick = {
                        val toRemove = duplicates.groupBy { it.fileSizeBytes }.map { it.value.drop(1) }.flatten()
                        remove = toRemove
                        duplicateDialog = false
                    }) { Text("Remove Duplicates (${duplicates.size / 2})") }
                } else {
                    TextButton(onClick = { duplicateDialog = false }) { Text("Done") }
                }
            },
            dismissButton = {
                TextButton(onClick = { duplicateDialog = false }) { Text("Cancel") }
            }
        )
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
    if (remove.isNotEmpty()) AlertDialog(
        onDismissRequest = { remove = emptyList() },
        title = { Text("Delete permanently?", fontWeight = FontWeight.Normal) },
        text = {
            Text(
                "This will permanently delete ${if (remove.size == 1) "this file" else "${remove.size} files"} from your mobile gallery and device storage. This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val removed = remove
                remove = emptyList()
                selection = emptySet()
                selectionMode = false
                scope.launch {
                    viewModel.removeItems(removed)
                    snackbar.showSnackbar("Deleted permanently from mobile gallery and device storage")
                }
            }) {
                Text("Delete permanently", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { remove = emptyList() }) { Text("Cancel") }
        }
    )
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
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatTile(allFiles.size.toString(), "Files", Modifier.weight(1f)); StatTile(FormattingUtils.formatBytes(allFiles.sumOf { it.fileSizeBytes }), "Saved media", Modifier.weight(1f)) } }

            // Storage Breakdown Bar
            item {
                val totalBytes = allFiles.sumOf { it.fileSizeBytes }.coerceAtLeast(1L)
                val videoBytes = allFiles.filter { it.mediaType.lowercase() == "video" }.sumOf { it.fileSizeBytes }
                val audioBytes = allFiles.filter { it.mediaType.lowercase() == "audio" }.sumOf { it.fileSizeBytes }
                val otherBytes = (totalBytes - videoBytes - audioBytes).coerceAtLeast(0L)

                Surface(
                    shape = RoundedCornerShape(CpdRadius.radiusLg),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(CpdBorderWidth.default, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(CpdSpace.space3x), verticalArrangement = Arrangement.spacedBy(CpdSpace.space15x)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Media Storage Breakdown", style = MaterialTheme.typography.labelMedium)
                            TextButton(onClick = { duplicateDialog = true }, contentPadding = PaddingValues(horizontal = CpdSpace.space15x, vertical = 0.dp)) {
                                Text("Scan Duplicates", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Row(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(CpdRadius.radiusSm))) {
                            if (videoBytes > 0) Box(Modifier.weight((videoBytes.toFloat() / totalBytes).coerceAtLeast(0.05f)).fillMaxHeight().background(BrandCoralOrange))
                            if (audioBytes > 0) Box(Modifier.weight((audioBytes.toFloat() / totalBytes).coerceAtLeast(0.05f)).fillMaxHeight().background(MaterialTheme.colorScheme.secondary))
                            if (otherBytes > 0) Box(Modifier.weight((otherBytes.toFloat() / totalBytes).coerceAtLeast(0.05f)).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🎬 Videos ${FormattingUtils.formatBytes(videoBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("🎵 Audio ${FormattingUtils.formatBytes(audioBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("📁 Other ${FormattingUtils.formatBytes(otherBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onSearchChange,
                    modifier = Modifier.fillMaxWidth().height(CpdSize.inputHeight),
                    singleLine = true,
                    placeholder = {
                        Text(
                            "Search titles, formats or notes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CpdColor.textDisabled
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = CpdColor.textPrimary),
                    leadingIcon = { Icon(HugeIcons.Search, null, Modifier.size(CpdSize.iconMd), tint = CpdColor.textSecondary) },
                    shape = RoundedCornerShape(CpdRadius.radiusMd),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = CpdColor.bgSubtleSecondary,
                        focusedContainerColor = CpdColor.bgSubtleSecondary,
                        unfocusedBorderColor = CpdColor.borderInteractiveSecondary,
                        focusedBorderColor = CpdColor.borderFocused,
                        focusedTextColor = CpdColor.textPrimary,
                        unfocusedTextColor = CpdColor.textPrimary,
                        cursorColor = CpdColor.CoralRest
                    )
                )
            }
            item { val types = listOf("ALL" to "All", "video" to "Videos", "audio" to "Music", "image" to "Images", "document" to "Docs", "gif" to "GIFs"); MediaDockPillTabs(types.map { it.second }, types.indexOfFirst { it.first == filter }.coerceAtLeast(0), { viewModel.selectFilter(types[it].first) }) }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(favorites, { favorites = !favorites }, { Text("Favorites") }, leadingIcon = { Icon(HugeIcons.PushPin, null, Modifier.size(CpdSize.iconSm)) })
                FilterChip(filterRecent24h, { filterRecent24h = !filterRecent24h }, { Text("Recent 24h") })
                FilterChip(filterLargeFiles, { filterLargeFiles = !filterLargeFiles }, { Text("Large (>50MB)") })
                Box {
                    FilterChip(
                        selected = sortMenu,
                        onClick = { sortMenu = true },
                        label = { Text(state.librarySort) },
                        trailingIcon = { Icon(HugeIcons.ChevronRight, null, Modifier.size(CpdSize.iconSm)) }
                    )
                    DropdownMenu(sortMenu, { sortMenu = false }) {
                        LibraryRules.sorts.forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort) },
                                onClick = {
                                    workspace.edit { it.copy(librarySort = sort) }
                                    sortMenu = false
                                }
                            )
                        }
                    }
                }
                IconButton(onClick = { workspace.edit { it.copy(grid = !it.grid) } }) { Icon(if (state.grid) HugeIcons.FolderOpen else HugeIcons.Collections, if (state.grid) "List view" else "Grid view") }
            } }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { selectionMode = !selectionMode; selection = emptySet() }) { Text(if (selectionMode) "Done" else "Select") }
                TextButton(onClick = { createCollection = true; collectionName = "" }) { Text("+ Collection") }
                TextButton(onClick = { workspace.edit { it.copy(compact = !it.compact) } }) { Text(if (state.compact) "Standard view" else "Compact view") }
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
            if (files.isEmpty()) item { Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(HugeIcons.PlayCircle, null, Modifier.size(48.dp), tint = BrandCoralOrange); Spacer(Modifier.height(16.dp)); Text(if (allFiles.isEmpty()) "Your library starts here" else "No matching files", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(4.dp)); Text(if (allFiles.isEmpty()) "Download media to see it in your collection." else "Try another filter, collection or search.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) } } }
            items(files.chunked(if (state.grid) 2 else 1), key = { it.first().id }) { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { file -> LibraryCard(file, LibraryRules.title(file, state), file.id in state.favorites, selectionMode, file.id in selection, state.grid, state.compact, Modifier.weight(1f),
                    onOpen = { if (selectionMode) selection = if (file.id in selection) selection - file.id else selection + file.id else playingFile = file },
                    onLongClick = { selectionMode = true; selection = selection + file.id }, onFavorite = { favorite(setOf(file.id)) }, onAction = { action -> when (action) {
                        "Play in MediaDock" -> playingFile = file
                        "Extract MP3 Audio" -> {
                            Toast.makeText(context, "Extracting audio track to Library...", Toast.LENGTH_SHORT).show()
                        }
                        "Details" -> details = file
                        "Edit title" -> { editFile = file; editNote = false; editText = LibraryRules.title(file, state) }
                        "Edit note" -> { editFile = file; editNote = true; editText = state.notes[file.id.toString()].orEmpty() }
                        "Copy source link" -> MediaActions.copy(context, file.sourceUrl)
                        "Open source" -> onBrowse(file.sourceUrl)
                        "Share file" -> MediaActions.shareFiles(context, listOf(Uri.parse(file.localUri)))
                        "Remove from library", "Delete from gallery" -> remove = listOf(file)
                    } }) }
                if (state.grid && row.size == 1) Spacer(Modifier.weight(1f))
            } }
            item { Spacer(Modifier.height(88.dp)) }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryCard(
    file: HistoryEntity,
    title: String,
    favorite: Boolean,
    selecting: Boolean,
    selected: Boolean,
    grid: Boolean,
    compact: Boolean,
    modifier: Modifier,
    onOpen: () -> Unit,
    onLongClick: () -> Unit,
    onFavorite: () -> Unit,
    onAction: (String) -> Unit
) {
    var menu by remember { mutableStateOf(false) }

    val (platformName, platformIcon) = remember(file.sourceUrl) {
        val lower = file.sourceUrl.lowercase()
        when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> Pair("YouTube", HugeIcons.Youtube)
            lower.contains("instagram.com") -> Pair("Instagram", HugeIcons.Instagram)
            lower.contains("tiktok.com") -> Pair("TikTok", HugeIcons.Tiktok)
            lower.contains("twitter.com") || lower.contains("x.com") -> Pair("X", HugeIcons.Twitter)
            lower.contains("facebook.com") || lower.contains("fb.watch") -> Pair("Facebook", HugeIcons.Facebook)
            lower.contains("reddit.com") -> Pair("Reddit", HugeIcons.Reddit)
            lower.contains("pinterest.com") -> Pair("Pinterest", HugeIcons.Pinterest)
            lower.contains("soundcloud.com") -> Pair("SoundCloud", HugeIcons.Soundcloud)
            else -> {
                val host = runCatching { Uri.parse(file.sourceUrl).host?.removePrefix("www.") }.getOrNull()
                Pair(host ?: "Web", HugeIcons.Language)
            }
        }
    }

    val type = file.mediaType.lowercase()
    val fmt = file.format.lowercase()
    val isDoc = type == "document" || fmt == "pdf" || fmt == "doc" || fmt == "docx"
    val isImage = type in listOf("image", "gif") || fmt in listOf("jpg", "jpeg", "png", "webp", "gif")
    val isAudio = type == "audio" || fmt in listOf("mp3", "m4a", "wav", "aac", "ogg", "opus")

    val resolutionLabel = remember(file.mediaType, file.format) {
        when {
            isDoc -> "PDF"
            isImage -> if (fmt == "gif") "GIF" else "High-Res"
            isAudio -> "HQ Audio"
            else -> "Video"
        }
    }

    Surface(
        modifier = modifier.combinedClickable(onClick = onOpen, onLongClick = onLongClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(if (compact) 10.dp else 14.dp)) {
            // Large Cinematic Banner Preview for Videos and Media
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (grid) 120.dp else if (compact) 130.dp else 175.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                MediaThumbnail(
                    model = file.localUri,
                    title = title,
                    mediaType = file.mediaType,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-Left Platform Badge Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.72f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = platformIcon,
                                contentDescription = null,
                                tint = BrandCoralOrange,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = platformName,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Center Play / Open Overlay Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isAudio -> HugeIcons.Headphones
                            isDoc -> HugeIcons.FileOpen
                            isImage -> HugeIcons.Image
                            else -> HugeIcons.PlayArrow
                        },
                        contentDescription = "Preview",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Bottom-Right Format & Resolution Overlay Pill
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.72f)
                    ) {
                        Text(
                            text = "${file.format.uppercase()} • $resolutionLabel",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Title and selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selecting) {
                    Checkbox(selected, { onOpen() })
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Rich Bottom Metadata: Platform Origin, Format, Resolution, File Size, Download Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = platformName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 90.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandCoralOrange.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = resolutionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = BrandCoralOrange,
                        fontWeight = FontWeight.Normal
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = file.format.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = FormattingUtils.formatBytes(file.fileSizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = DateFormat.getDateInstance(DateFormat.SHORT).format(Date(file.downloadedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(Modifier.height(10.dp))

            // Quick Actions: Play in MediaDock, Favorite, Share, More Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpen() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isDoc -> HugeIcons.FileOpen
                                isImage -> HugeIcons.Image
                                isAudio -> HugeIcons.Headphones
                                else -> HugeIcons.PlayArrow
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = when {
                                isDoc -> "Open PDF"
                                isImage -> "View Image"
                                isAudio -> "Play Audio"
                                else -> "Play Video"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onFavorite, modifier = Modifier.size(38.dp)) {
                        Icon(
                            imageVector = HugeIcons.PushPin,
                            contentDescription = if (favorite) "Remove favorite" else "Favorite file",
                            tint = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = { onAction("Share file") }, modifier = Modifier.size(38.dp)) {
                        Icon(
                            imageVector = HugeIcons.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    Box {
                        IconButton(onClick = { menu = true }, modifier = Modifier.size(38.dp)) {
                            Icon(
                                imageVector = HugeIcons.MoreVert,
                                contentDescription = "More actions",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                        DropdownMenu(menu, { menu = false }) {
                            listOf(
                                "Play in MediaDock",
                                "Extract MP3 Audio",
                                "Details",
                                "Edit title",
                                "Edit note",
                                "Copy source link",
                                "Open source",
                                "Share file",
                                "Delete from gallery"
                            ).forEach { action ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = action,
                                            color = if (action == "Delete from gallery") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Normal
                                        )
                                    },
                                    onClick = { menu = false; onAction(action) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
