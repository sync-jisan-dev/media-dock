package com.mediadock.app.feature.downloads

import com.mediadock.app.core.designsystem.icons.HugeIcons

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.mediadock.app.core.designsystem.components.StatTile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mediadock.app.R
import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.designsystem.components.MediaDockEmptyState
import com.mediadock.app.core.designsystem.components.MediaDockTransferStatus
import com.mediadock.app.core.designsystem.theme.bouncyClickable
import com.mediadock.app.domain.model.ActiveDownload
import com.mediadock.app.domain.model.DownloadJobStatus

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val downloads by viewModel.activeDownloads.collectAsState()
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("All") }
    val actionError by viewModel.actionError.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val filtered = downloads.filter { (it.title.contains(search, true) || it.sourceUrl.contains(search, true)) && when (status) {
        "Running" -> it.status in setOf(DownloadJobStatus.PENDING, DownloadJobStatus.QUEUED, DownloadJobStatus.PROCESSING, DownloadJobStatus.DOWNLOADING)
        "Paused" -> it.status == DownloadJobStatus.PAUSED
        "Failed" -> it.status == DownloadJobStatus.FAILED
        else -> true
    } }


    if (downloads.isEmpty()) {
        MediaDockEmptyState(
            icon = HugeIcons.Download,
            title = stringResource(R.string.no_active_downloads),
            message = "Paste a link or share media from another app to begin downloading.",
            actionLabel = "Add Media Link",
            onActionClick = onNavigateHome,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp, 24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${downloads.size} Active Downloads",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(FormattingUtils.formatSpeed(downloads.filter { it.status == DownloadJobStatus.DOWNLOADING }.sumOf { it.speed }), "Total speed", Modifier.weight(1f))
                    StatTile(FormattingUtils.formatBytes(downloads.sumOf { it.downloadedBytes }), "Transferred", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), placeholder = { Text("Search your queue") }, singleLine = true, leadingIcon = { Icon(HugeIcons.Search, null) }, shape = RoundedCornerShape(16.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Running", "Paused", "Failed").forEach { label -> FilterChip(status == label, { status = label }, { Text(label) }) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    TextButton(onClick = viewModel::pauseAll, enabled = !busy && downloads.any { it.status == DownloadJobStatus.DOWNLOADING }) { Text("Pause all") }
                    TextButton(onClick = viewModel::resumeAll, enabled = !busy && downloads.any { it.status == DownloadJobStatus.PAUSED }) { Text("Resume all") }
                    TextButton(onClick = viewModel::retryFailed, enabled = !busy && downloads.any { it.status == DownloadJobStatus.FAILED }) { Text("Retry failed") }
                }
                if (actionError != null) Text(actionError!!, style = MaterialTheme.typography.bodySmall)
                if (filtered.isEmpty()) Text("No downloads match this filter.", style = MaterialTheme.typography.bodyMedium)
            }
            items(filtered, key = { it.jobId }) { item ->
                DownloadItemCard(
                    item = item,
                    onPause = { viewModel.pauseDownload(item.jobId) },
                    onResume = { if (item.status == DownloadJobStatus.FAILED) viewModel.retryDownload(item) else viewModel.resumeDownload(item.jobId) },
                    onCancel = { viewModel.cancelDownload(item.jobId) },
                    onDelete = { viewModel.deleteDownload(item.jobId) },
                    onOpen = {
                        item.localUri?.let { uriStr ->
                            try {
                                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(uriStr), "*/*")
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(openIntent)
                            } catch (_: Exception) {}
                        }
                    },
                    onShare = {
                        item.localUri?.let { uriStr ->
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "*/*"
                                    putExtra(Intent.EXTRA_STREAM, Uri.parse(uriStr))
                                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                            } catch (_: Exception) {}
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DownloadItemCard(
    item: ActiveDownload,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (item.progress / 100f).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DownloadProgressAnimation"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (item.status == DownloadJobStatus.DOWNLOADING) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!item.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = item.thumbnail,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = item.mediaType.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = item.format.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.fileSizeBytes > 0) {
                            Text(
                                text = "• ${FormattingUtils.formatBytes(item.fileSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Delete / Dismiss
                IconButton(onClick = onDelete) {
                    Icon(imageVector = HugeIcons.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            MediaDockTransferStatus(
                label = item.status.name.lowercase().replaceFirstChar { it.uppercase() },
                progress = animatedProgress,
                completed = item.status == DownloadJobStatus.COMPLETED
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (item.status == DownloadJobStatus.PAUSED) Text("Resuming restarts this file transfer from the beginning.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Progress Metrics & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (item.speed > 0) {
                        Text(
                            text = "${FormattingUtils.formatSpeed(item.speed)} • ${FormattingUtils.formatDuration(item.eta)} left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (item.status) {
                        DownloadJobStatus.DOWNLOADING -> {
                            IconButton(onClick = onPause) {
                                Icon(imageVector = HugeIcons.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onCancel) {
                                Icon(imageVector = HugeIcons.Cancel, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        DownloadJobStatus.PAUSED -> {
                            IconButton(onClick = onResume) {
                                Icon(imageVector = HugeIcons.PlayArrow, contentDescription = "Resume", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onCancel) {
                                Icon(imageVector = HugeIcons.Cancel, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        DownloadJobStatus.PROCESSING, DownloadJobStatus.QUEUED, DownloadJobStatus.PENDING -> {
                            IconButton(onClick = onCancel) { Icon(HugeIcons.Cancel, "Cancel processing") }
                        }
                        DownloadJobStatus.COMPLETED -> {
                            IconButton(onClick = onOpen) {
                                Icon(imageVector = HugeIcons.FileOpen, contentDescription = "Open", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onShare) {
                                Icon(imageVector = HugeIcons.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        DownloadJobStatus.FAILED -> {
                            IconButton(onClick = onResume) {
                                Icon(imageVector = HugeIcons.PlayArrow, contentDescription = "Retry", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
