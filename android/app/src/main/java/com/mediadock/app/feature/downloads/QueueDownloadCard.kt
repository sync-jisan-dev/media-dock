package com.mediadock.app.feature.downloads

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.designsystem.components.MediaDockTransferStatus
import com.mediadock.app.core.designsystem.components.MediaThumbnail
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.domain.model.ActiveDownload
import com.mediadock.app.domain.model.DownloadJobStatus
import com.mediadock.app.feature.browser.BrowserRules
import java.util.Locale

@Composable
internal fun QueueCard(content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
internal fun QueueDownloadCard(
    item: ActiveDownload,
    actionsEnabled: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onCopyLink: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isPaused = item.status == DownloadJobStatus.PAUSED
    val isFailed = item.status == DownloadJobStatus.FAILED
    val isDownloading = item.status == DownloadJobStatus.DOWNLOADING
    val hasTransferredBytes = (isDownloading || isPaused) && item.downloadedBytes > 0
    val progress = item.progress.takeIf { hasTransferredBytes && it > 0f }?.div(100f)
    val subtitle = listOf(
        BrowserRules.displayHost(item.sourceUrl),
        item.format.uppercase(Locale.ROOT)
    ).filter { it.isNotBlank() }.joinToString(" · ")
    val status = when (item.status) {
        DownloadJobStatus.PENDING -> "Waiting for server"
        DownloadJobStatus.QUEUED -> "Queued"
        DownloadJobStatus.PROCESSING -> "Preparing media"
        DownloadJobStatus.DOWNLOADING -> if (hasTransferredBytes) "Downloading" else "Starting transfer"
        DownloadJobStatus.PAUSED -> "Paused"
        DownloadJobStatus.COMPLETED -> "Saved to My files"
        DownloadJobStatus.FAILED -> "Download failed"
        DownloadJobStatus.CANCELLED -> "Cancelled"
    }

    QueueCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MediaThumbnail(
                model = item.thumbnail,
                title = item.title,
                mediaType = item.mediaType,
                modifier = Modifier.size(width = 72.dp, height = 52.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    item.title.ifBlank { "Media download" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(HugeIcons.MoreVert, contentDescription = "Download options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Copy source link") },
                        onClick = {
                            menuOpen = false
                            onCopyLink()
                        }
                    )
                    if (item.status.canCancelInQueue) {
                        DropdownMenuItem(
                            text = { Text("Cancel download", color = MaterialTheme.colorScheme.error) },
                            enabled = actionsEnabled,
                            onClick = {
                                menuOpen = false
                                onCancel()
                            }
                        )
                    } else if (isFailed || item.status == DownloadJobStatus.CANCELLED) {
                        DropdownMenuItem(
                            text = { Text("Remove from queue") },
                            enabled = actionsEnabled,
                            onClick = {
                                menuOpen = false
                                onRemove()
                            }
                        )
                    }
                }
            }
        }

        if (item.status.isActiveInQueue || isPaused) {
            MediaDockTransferStatus(label = status, progress = progress, paused = isPaused)
        } else {
            Text(
                status,
                style = MaterialTheme.typography.labelLarge,
                color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val detail = when {
            (isPaused || isFailed) && !item.error.isNullOrBlank() -> item.error
            hasTransferredBytes -> buildList {
                add("${FormattingUtils.formatBytes(item.downloadedBytes)} received")
                if (isDownloading && item.speed > 0) add(FormattingUtils.formatSpeed(item.speed))
                if (isDownloading && item.eta > 0) add("${FormattingUtils.formatDuration(item.eta)} left")
            }.joinToString(" · ")
            else -> null
        }
        if (detail != null) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val action = when (item.status) {
            DownloadJobStatus.DOWNLOADING -> "Pause" to onPause
            DownloadJobStatus.PAUSED -> "Resume" to onResume
            DownloadJobStatus.FAILED -> "Retry" to onRetry
            else -> null
        }
        if (action != null) {
            TextButton(
                onClick = action.second,
                enabled = actionsEnabled,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(action.first)
            }
        }
    }
}
