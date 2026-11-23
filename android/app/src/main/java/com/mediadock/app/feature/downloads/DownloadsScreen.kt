package com.mediadock.app.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.common.MediaActions
import com.mediadock.app.core.designsystem.components.IllustrationScene
import com.mediadock.app.core.designsystem.components.MediaDockEmptyState
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.domain.model.DownloadJobStatus
import com.mediadock.app.feature.media.MediaInspectViewModel

private enum class QueueFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    PAUSED("Paused"),
    FAILED("Failed");

    fun matches(status: DownloadJobStatus): Boolean = when (this) {
        ALL -> true
        ACTIVE -> status.isActiveInQueue
        PAUSED -> status == DownloadJobStatus.PAUSED
        FAILED -> status == DownloadJobStatus.FAILED
    }
}

internal val DownloadJobStatus.isActiveInQueue: Boolean
    get() = when (this) {
        DownloadJobStatus.PENDING, DownloadJobStatus.QUEUED,
        DownloadJobStatus.PROCESSING, DownloadJobStatus.DOWNLOADING -> true
        else -> false
    }

internal val DownloadJobStatus.canCancelInQueue: Boolean
    get() = isActiveInQueue || this == DownloadJobStatus.PAUSED

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    inspectionViewModel: MediaInspectViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.activeDownloads.collectAsState()
    val inspectionUrl by inspectionViewModel.sourceUrl.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val context = LocalContext.current
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(QueueFilter.ALL) }
    var showQueueMenu by remember { mutableStateOf(false) }
    var confirmCancelAll by remember { mutableStateOf(false) }

    if (downloads.isEmpty() && inspectionUrl.isNullOrBlank() && actionError == null) {
        MediaDockEmptyState(
            icon = HugeIcons.Download,
            title = "Your queue is empty",
            message = "Downloads will appear here after you add a link.",
            modifier = modifier,
            illustration = IllustrationScene.HIPPO_QUEUE,
            illustrationSize = 190.dp,
            titleStyle = MaterialTheme.typography.titleLarge,
            messageStyle = MaterialTheme.typography.bodySmall
        )
        return
    }

    val query = search.trim()
    val visibleDownloads = downloads.filter { item ->
        filter.matches(item.status) &&
            (item.title.contains(query, ignoreCase = true) || item.sourceUrl.contains(query, ignoreCase = true))
    }
    val activeCount = downloads.count { it.status.isActiveInQueue }
    val canCancel = downloads.any { it.status.canCancelInQueue }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (downloads.isNotEmpty()) {
            item(key = "queue-controls") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (downloads.size == 1) "1 download" else "${downloads.size} downloads",
                                style = MaterialTheme.typography.titleLarge
                            )
                            if (activeCount > 0) {
                                Text(
                                    "$activeCount active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box {
                            IconButton(onClick = { showQueueMenu = true }) {
                                Icon(HugeIcons.MoreVert, contentDescription = "Queue actions")
                            }
                            DropdownMenu(expanded = showQueueMenu, onDismissRequest = { showQueueMenu = false }) {
                                if (downloads.any { it.status == DownloadJobStatus.DOWNLOADING }) {
                                    DropdownMenuItem(
                                        text = { Text("Pause all transfers") },
                                        enabled = !busy,
                                        onClick = { showQueueMenu = false; viewModel.pauseAll() }
                                    )
                                }
                                if (downloads.any { it.status == DownloadJobStatus.PAUSED }) {
                                    DropdownMenuItem(
                                        text = { Text("Resume paused") },
                                        enabled = !busy,
                                        onClick = { showQueueMenu = false; viewModel.resumeAll() }
                                    )
                                }
                                if (downloads.any { it.status == DownloadJobStatus.FAILED }) {
                                    DropdownMenuItem(
                                        text = { Text("Retry failed") },
                                        enabled = !busy,
                                        onClick = { showQueueMenu = false; viewModel.retryFailed() }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Copy all links") },
                                    onClick = {
                                        showQueueMenu = false
                                        MediaActions.copy(context, viewModel.exportQueueUrls())
                                    }
                                )
                                if (canCancel) {
                                    DropdownMenuItem(
                                        text = { Text("Cancel all transfers", color = MaterialTheme.colorScheme.error) },
                                        enabled = !busy,
                                        onClick = { showQueueMenu = false; confirmCancelAll = true }
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("Search queue") },
                        leadingIcon = { Icon(HugeIcons.Search, null, Modifier.size(20.dp)) },
                        trailingIcon = if (search.isNotEmpty()) {
                            {
                                IconButton(onClick = { search = "" }) {
                                    Icon(HugeIcons.Close, "Clear search", Modifier.size(20.dp))
                                }
                            }
                        } else null
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QueueFilter.entries.forEach { option ->
                            FilterChip(
                                selected = filter == option,
                                onClick = { filter = option },
                                label = { Text(option.label) }
                            )
                        }
                    }
                }
            }
        }
        actionError?.let { message ->
            item(key = "queue-action-error") {
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        val url = inspectionUrl
        if (!url.isNullOrBlank()) {
            item(key = "queue-inspection") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    QueueMediaInspection(url, inspectionViewModel, onNavigateToSettings)
                }
            }
        }
        if (downloads.isNotEmpty() && visibleDownloads.isEmpty()) {
            item(key = "queue-no-matches") {
                Text(
                    "No downloads match your search or filter.",
                    modifier = Modifier.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        items(visibleDownloads, key = { it.jobId }) { item ->
            QueueDownloadCard(
                item = item,
                actionsEnabled = !busy,
                onPause = { viewModel.pauseDownload(item.jobId) },
                onResume = { viewModel.resumeDownload(item.jobId) },
                onRetry = { viewModel.retryDownload(item) },
                onCancel = { viewModel.cancelDownload(item.jobId) },
                onRemove = { viewModel.deleteDownload(item.jobId) },
                onCopyLink = { MediaActions.copy(context, item.sourceUrl) }
            )
        }
    }
    if (confirmCancelAll) {
        AlertDialog(
            onDismissRequest = { confirmCancelAll = false },
            title = { Text("Cancel all transfers?") },
            text = { Text("This stops active and paused downloads.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancelAll = false
                    viewModel.cancelAll()
                }) { Text("Cancel all") }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancelAll = false }) { Text("Keep downloads") }
            }
        )
    }
}
