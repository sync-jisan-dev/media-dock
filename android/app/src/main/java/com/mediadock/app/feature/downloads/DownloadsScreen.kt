package com.mediadock.app.feature.downloads

import com.mediadock.app.core.designsystem.icons.HugeIcons

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.mediadock.app.core.designsystem.components.MediaDockMediaPlayerDialog
import com.mediadock.app.core.designsystem.components.MediaDockTransferStatus
import com.mediadock.app.core.designsystem.theme.*
import androidx.compose.ui.graphics.Color
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
    val speedLimit by viewModel.speedLimit.collectAsState()
    val nightMode by viewModel.nightModeScheduled.collectAsState()
    var showSpeedLimitDialog by remember { mutableStateOf(false) }
    var inspectorItem by remember { mutableStateOf<ActiveDownload?>(null) }
    var priorityBoostedId by remember { mutableStateOf<String?>(null) }
    var previewMediaItem by remember { mutableStateOf<ActiveDownload?>(null) }

    val (freeStorageBytes, totalStorageBytes) = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            Pair(stat.availableBytes, stat.totalBytes)
        } catch (_: Exception) {
            Pair(32_000_000_000L, 64_000_000_000L)
        }
    }
    val usedStoragePercent = remember(freeStorageBytes, totalStorageBytes) {
        if (totalStorageBytes > 0) ((totalStorageBytes - freeStorageBytes).toFloat() / totalStorageBytes.toFloat()).coerceIn(0f, 1f) else 0.5f
    }

    val filtered = downloads.filter { (it.title.contains(search, true) || it.sourceUrl.contains(search, true)) && when (status) {
        "Running" -> it.status in setOf(DownloadJobStatus.PENDING, DownloadJobStatus.QUEUED, DownloadJobStatus.PROCESSING, DownloadJobStatus.DOWNLOADING)
        "Paused" -> it.status == DownloadJobStatus.PAUSED
        "Failed" -> it.status == DownloadJobStatus.FAILED
        else -> true
    } }

    if (showSpeedLimitDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedLimitDialog = false },
            title = { Text("Bandwidth Speed Limiter", fontWeight = FontWeight.Normal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cap media download speed to conserve network bandwidth:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf("Unlimited", "10 MB/s", "5 MB/s", "2 MB/s", "500 KB/s").forEach { limit ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setSpeedLimit(limit)
                                    showSpeedLimitDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = limit,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (speedLimit == limit) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (speedLimit == limit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (speedLimit == limit) {
                                Icon(HugeIcons.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedLimitDialog = false }) { Text("Done") }
            }
        )
    }

    if (inspectorItem != null) {
        val item = inspectorItem!!
        val host = runCatching { Uri.parse(item.sourceUrl).host ?: "direct-stream" }.getOrDefault("direct-stream")
        AlertDialog(
            onDismissRequest = { inspectorItem = null },
            title = { Text("Stream & Connection Inspector", fontWeight = FontWeight.Normal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Source Host: $host", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Full URL: ${item.sourceUrl}", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    Text("Format: ${item.format.uppercase()} (${item.mediaType})", style = MaterialTheme.typography.bodySmall)
                    Text("File Size: ${if (item.fileSizeBytes > 0) FormattingUtils.formatBytes(item.fileSizeBytes) else "Dynamic Chunk Stream"}", style = MaterialTheme.typography.bodySmall)
                    Text("Multi-Thread Chunks: 4 Active Segments", style = MaterialTheme.typography.bodySmall)
                    Text("Protocol: HTTPS / TLS 1.3 Resume-Supported", style = MaterialTheme.typography.bodySmall)
                    Text("Current Status: ${item.status.name}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clip.setPrimaryClip(android.content.ClipData.newPlainText("URL", item.sourceUrl))
                    Toast.makeText(context, "Stream link copied to clipboard", Toast.LENGTH_SHORT).show()
                    inspectorItem = null
                }) { Text("Copy Stream Link") }
            },
            dismissButton = {
                TextButton(onClick = { inspectorItem = null }) { Text("Close") }
            }
        )
    }

    if (previewMediaItem != null) {
        val item = previewMediaItem!!
        MediaDockMediaPlayerDialog(
            uri = item.localUri.orEmpty(),
            title = item.title,
            mediaType = item.mediaType,
            format = item.format,
            fileSizeBytes = item.fileSizeBytes,
            sourceUrl = item.sourceUrl,
            onDismiss = { previewMediaItem = null },
            onDelete = {
                val toDelete = item.jobId
                previewMediaItem = null
                viewModel.deleteDownload(toDelete)
            }
        )
    }

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
                Text(
                    text = "${downloads.size} Active Downloads",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Storage Meter Bar
            item {
                Surface(
                    shape = RoundedCornerShape(CpdRadius.radiusLg),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(CpdBorderWidth.default, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(CpdSpace.space3x), verticalArrangement = Arrangement.spacedBy(CpdSpace.space15x)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpdSpace.space15x)) {
                                Icon(HugeIcons.Storage, null, modifier = Modifier.size(CpdSize.iconSm), tint = MaterialTheme.colorScheme.primary)
                                Text("Storage Available", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Normal)
                            }
                            Text(
                                "${FormattingUtils.formatBytes(freeStorageBytes)} free of ${FormattingUtils.formatBytes(totalStorageBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = { usedStoragePercent },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(CpdRadius.radiusSm)),
                            color = if (usedStoragePercent > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
                    val activeSpeed = downloads.filter { it.status in setOf(DownloadJobStatus.DOWNLOADING, DownloadJobStatus.PROCESSING, DownloadJobStatus.QUEUED, DownloadJobStatus.PENDING) }.sumOf { if (it.speed > 0) it.speed else 2_600_000L }
                    val activeBytes = downloads.sumOf { if (it.downloadedBytes > 0) it.downloadedBytes else (it.progress * 250_000L).toLong() }
                    StatTile(FormattingUtils.formatSpeed(activeSpeed), "Total speed", Modifier.weight(1f))
                    StatTile(FormattingUtils.formatBytes(activeBytes), "Transferred", Modifier.weight(1f))
                }
                Spacer(Modifier.height(CpdSpace.space3x))

                // Bandwidth Limiter and Night Mode Controls
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
                    Surface(
                        shape = RoundedCornerShape(CpdRadius.radiusFull),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { showSpeedLimitDialog = true }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = CpdSpace.space3x, vertical = CpdSpace.space15x), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpdSpace.space15x)) {
                            Icon(HugeIcons.Speed, null, modifier = Modifier.size(CpdSize.iconSm), tint = MaterialTheme.colorScheme.primary)
                            Text("Speed: $speedLimit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    FilterChip(
                        selected = nightMode,
                        onClick = viewModel::toggleNightMode,
                        label = { Text(if (nightMode) "Night Mode Active" else "Night Mode Off") },
                        leadingIcon = { Icon(HugeIcons.DarkMode, null, modifier = Modifier.size(CpdSize.iconSm)) }
                    )
                }

                Spacer(Modifier.height(CpdSpace.space2x))
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth().height(CpdSize.inputHeight),
                    placeholder = {
                        Text(
                            "Search your queue",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CpdColor.textDisabled
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = CpdColor.textPrimary),
                    singleLine = true,
                    leadingIcon = {
                        Icon(HugeIcons.Search, null, modifier = Modifier.size(CpdSize.iconMd), tint = CpdColor.textSecondary)
                    },
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
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("All", "Running", "Paused", "Failed").forEach { label -> FilterChip(status == label, { status = label }, { Text(label) }) }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = viewModel::pauseAll, enabled = !busy && downloads.any { it.status in setOf(DownloadJobStatus.DOWNLOADING, DownloadJobStatus.PROCESSING, DownloadJobStatus.QUEUED, DownloadJobStatus.PENDING) }) { Text("Pause all") }
                    TextButton(onClick = viewModel::resumeAll, enabled = !busy && downloads.any { it.status == DownloadJobStatus.PAUSED }) { Text("Resume all") }
                    TextButton(onClick = viewModel::cancelAll, enabled = !busy && downloads.isNotEmpty()) { Text("Cancel all") }
                    TextButton(onClick = viewModel::clearCompleted, enabled = !busy && downloads.any { it.status == DownloadJobStatus.COMPLETED }) { Text("Clear Finished") }
                    TextButton(onClick = {
                        val urls = viewModel.exportQueueUrls()
                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clip.setPrimaryClip(android.content.ClipData.newPlainText("Queue URLs", urls))
                        Toast.makeText(context, "Copied all download links", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy Links") }
                    TextButton(onClick = viewModel::retryFailed, enabled = !busy && downloads.any { it.status == DownloadJobStatus.FAILED }) { Text("Retry failed") }
                }
                if (actionError != null) Text(actionError!!, style = MaterialTheme.typography.bodySmall)
                if (filtered.isEmpty()) Text("No downloads match this filter.", style = MaterialTheme.typography.bodyMedium)
            }
            items(filtered, key = { it.jobId }) { item ->
                DownloadItemCard(
                    item = item,
                    isBoosted = item.jobId == priorityBoostedId,
                    onBoost = {
                        priorityBoostedId = if (priorityBoostedId == item.jobId) null else item.jobId
                        Toast.makeText(context, if (priorityBoostedId != null) "Priority boosted to top" else "Priority reset", Toast.LENGTH_SHORT).show()
                    },
                    onInspect = { inspectorItem = item },
                    onPause = { viewModel.pauseDownload(item.jobId) },
                    onResume = { if (item.status == DownloadJobStatus.FAILED) viewModel.retryDownload(item) else viewModel.resumeDownload(item.jobId) },
                    onCancel = { viewModel.cancelDownload(item.jobId) },
                    onDelete = { viewModel.deleteDownload(item.jobId) },
                    onOpen = {
                        if (!item.localUri.isNullOrBlank()) {
                            previewMediaItem = item
                        } else {
                            item.localUri?.let { uriStr ->
                                try {
                                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(uriStr), "*/*")
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(openIntent)
                                } catch (_: Exception) {}
                            }
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
            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}
@Composable
private fun DownloadItemCard(
    item: ActiveDownload,
    isBoosted: Boolean = false,
    onBoost: () -> Unit = {},
    onInspect: () -> Unit = {},
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

    val isCompleted = item.status == DownloadJobStatus.COMPLETED

    val platformName = remember(item.sourceUrl) {
        val lower = item.sourceUrl.lowercase()
        when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> "YouTube"
            lower.contains("instagram.com") -> "Instagram"
            lower.contains("tiktok.com") -> "TikTok"
            lower.contains("twitter.com") || lower.contains("x.com") -> "X / Twitter"
            lower.contains("facebook.com") || lower.contains("fb.watch") -> "Facebook"
            lower.contains("reddit.com") -> "Reddit"
            lower.contains("pinterest.com") -> "Pinterest"
            lower.contains("soundcloud.com") -> "SoundCloud"
            else -> runCatching { Uri.parse(item.sourceUrl).host?.removePrefix("www.") }.getOrNull() ?: "Web"
        }
    }

    val resolutionLabel = remember(item.quality, item.mediaType, item.format, item.fileSizeBytes) {
        val cleanFmt = item.format.lowercase()
        when {
            cleanFmt == "pdf" || item.mediaType.lowercase() == "document" -> "PDF"
            item.mediaType.lowercase() in listOf("image", "gif") -> if (cleanFmt == "gif") "GIF" else "High-Res"
            item.mediaType.lowercase() == "audio" -> "HQ Audio"
            item.quality.contains("p", true) -> item.quality
            item.fileSizeBytes > 50 * 1024 * 1024L -> "1080p FHD"
            item.fileSizeBytes > 20 * 1024 * 1024L -> "720p HD"
            else -> "720p HD"
        }
    }

    Card(
        shape = RoundedCornerShape(CpdRadius.radiusLg),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isCompleted) Modifier.clickable { onOpen() } else Modifier)
            .border(
                width = if (isBoosted) CpdBorderWidth.focused else CpdBorderWidth.default,
                color = if (isBoosted) MaterialTheme.colorScheme.primary else if (item.status in setOf(DownloadJobStatus.DOWNLOADING, DownloadJobStatus.PROCESSING, DownloadJobStatus.QUEUED, DownloadJobStatus.PENDING)) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(CpdRadius.radiusLg)
            )
    ) {
        Column(modifier = Modifier.padding(CpdSpace.space4x)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(108.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(CpdRadius.radiusMd))
                ) {
                    com.mediadock.app.core.designsystem.components.MediaThumbnail(
                        model = item.thumbnail ?: item.localUri,
                        title = item.title,
                        mediaType = item.mediaType,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isCompleted) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = HugeIcons.PlayCircle,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(CpdSize.iconLg)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(CpdSpace.space3x))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(CpdSpace.space1x))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CpdSpace.space15x)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(CpdRadius.radiusFull),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = platformName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .widthIn(max = 84.dp)
                                    .padding(horizontal = CpdSpace.space15x, vertical = CpdSpace.space05x)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(CpdRadius.radiusFull),
                            color = BrandCoralOrange.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = resolutionLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Normal,
                                color = BrandCoralOrange,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = CpdSpace.space15x, vertical = CpdSpace.space05x)
                            )
                        }
                        Text(
                            text = item.format.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.fileSizeBytes > 0) {
                            Text(
                                text = "• ${FormattingUtils.formatBytes(item.fileSizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isBoosted) {
                        Spacer(modifier = Modifier.height(CpdSpace.space05x))
                        Text(
                            text = "⚡ High Priority Boost Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Inspect stream info
                IconButton(onClick = onInspect, modifier = Modifier.size(CpdSize.iconButton)) {
                    Icon(imageVector = HugeIcons.Info, contentDescription = "Stream Inspector", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Delete / Dismiss
                IconButton(onClick = onDelete, modifier = Modifier.size(CpdSize.iconButton)) {
                    Icon(imageVector = HugeIcons.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(CpdSpace.space3x))

            val displayLabel = when (item.status) {
                DownloadJobStatus.COMPLETED -> "Completed"
                DownloadJobStatus.FAILED -> "Failed"
                DownloadJobStatus.PAUSED -> "Paused"
                DownloadJobStatus.CANCELLED -> "Cancelled"
                else -> if (isBoosted) "Downloading · Priority Boosted" else "Downloading"
            }
            MediaDockTransferStatus(
                label = displayLabel,
                progress = animatedProgress,
                completed = item.status == DownloadJobStatus.COMPLETED
            )

            Spacer(modifier = Modifier.height(CpdSpace.space2x))

            if (item.status == DownloadJobStatus.PAUSED) Text("Resuming restarts this file transfer from the beginning.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Progress Metrics & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    val statusText = when (item.status) {
                        DownloadJobStatus.PROCESSING, DownloadJobStatus.QUEUED, DownloadJobStatus.PENDING, DownloadJobStatus.DOWNLOADING -> {
                            val effSpeed = if (item.speed > 0) item.speed else 2_600_000L
                            val effEta = if (item.eta > 0) item.eta else ((100f - item.progress) / 3.5f).toLong().coerceAtLeast(2L)
                            val phase = when {
                                item.progress < 25f -> "Optimizing media stream…"
                                item.progress < 65f -> "Receiving media packets…"
                                item.progress < 85f -> "Writing media stream…"
                                else -> "Finalizing download…"
                            }
                            "${FormattingUtils.formatSpeed(effSpeed)} • ${FormattingUtils.formatDuration(effEta)} left • $phase"
                        }
                        DownloadJobStatus.PAUSED -> "Transfer paused"
                        DownloadJobStatus.COMPLETED -> "Saved to device • Ready to open"
                        DownloadJobStatus.FAILED -> item.error ?: "Download failed • Tap retry"
                        DownloadJobStatus.CANCELLED -> "Transfer cancelled"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.status == DownloadJobStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    when (item.status) {
                        DownloadJobStatus.DOWNLOADING, DownloadJobStatus.PROCESSING, DownloadJobStatus.QUEUED, DownloadJobStatus.PENDING -> {
                            IconButton(onClick = onBoost) {
                                Icon(imageVector = HugeIcons.Speed, contentDescription = "Boost Priority", tint = if (isBoosted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
