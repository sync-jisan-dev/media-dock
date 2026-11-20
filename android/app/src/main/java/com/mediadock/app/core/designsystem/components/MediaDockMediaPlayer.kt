package com.mediadock.app.core.designsystem.components

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.common.MediaActions
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.*
import kotlinx.coroutines.delay
import java.io.File

/**
 * Universal In-App Media Player for MediaDock.
 * Supports Video (with scrub, seek, 10s skip, speed control, mute),
 * High-Res Image & GIF viewer (with zoom & pan), Audio visualizer, and PDF document preview.
 */
@Composable
fun MediaDockMediaPlayerDialog(
    uri: String,
    title: String,
    mediaType: String,
    format: String,
    fileSizeBytes: Long = 0L,
    sourceUrl: String = "",
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val cleanFormat = format.lowercase()
    val isPdf = cleanFormat == "pdf" || mediaType.lowercase() == "document" || uri.lowercase().endsWith(".pdf")
    val isImage = !isPdf && (mediaType.lowercase() in listOf("image", "gif") || cleanFormat in listOf("jpg", "jpeg", "png", "webp", "gif", "svg"))
    val isAudio = !isPdf && !isImage && (mediaType.lowercase() == "audio" || cleanFormat in listOf("mp3", "m4a", "opus", "wav", "flac", "aac", "ogg"))
    val isVideo = !isPdf && !isImage && !isAudio

    val platformName = remember(sourceUrl) {
        val lower = sourceUrl.lowercase()
        when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> "YouTube"
            lower.contains("instagram.com") -> "Instagram"
            lower.contains("tiktok.com") -> "TikTok"
            lower.contains("twitter.com") || lower.contains("x.com") -> "X / Twitter"
            lower.contains("facebook.com") || lower.contains("fb.watch") -> "Facebook"
            lower.contains("reddit.com") -> "Reddit"
            lower.contains("pinterest.com") -> "Pinterest"
            lower.contains("soundcloud.com") -> "SoundCloud"
            else -> runCatching { Uri.parse(sourceUrl).host?.removePrefix("www.") }.getOrNull() ?: "Web Source"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            when {
                isVideo -> {
                    VideoPlayerView(
                        uri = uri,
                        title = title,
                        format = format,
                        platform = platformName,
                        fileSizeBytes = fileSizeBytes,
                        onDismiss = onDismiss,
                        onShare = {
                            MediaActions.shareFiles(context, listOf(Uri.parse(uri)))
                        },
                        onOpenExternal = {
                            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(uri), "video/*")
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            runCatching { context.startActivity(openIntent) }
                        },
                        onDelete = onDelete
                    )
                }
                isImage -> {
                    ImageViewerView(
                        uri = uri,
                        title = title,
                        format = format,
                        fileSizeBytes = fileSizeBytes,
                        platform = platformName,
                        onDismiss = onDismiss,
                        onShare = {
                            MediaActions.shareFiles(context, listOf(Uri.parse(uri)))
                        },
                        onDelete = onDelete
                    )
                }
                isAudio -> {
                    AudioPlayerView(
                        uri = uri,
                        title = title,
                        format = format,
                        fileSizeBytes = fileSizeBytes,
                        platform = platformName,
                        onDismiss = onDismiss,
                        onShare = {
                            MediaActions.shareFiles(context, listOf(Uri.parse(uri)))
                        },
                        onDelete = onDelete
                    )
                }
                else -> {
                    DocumentPdfView(
                        uri = uri,
                        title = title,
                        format = format,
                        fileSizeBytes = fileSizeBytes,
                        platform = platformName,
                        onDismiss = onDismiss,
                        onShare = {
                            MediaActions.shareFiles(context, listOf(Uri.parse(uri)))
                        },
                        onOpenExternal = {
                            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(uri), "application/pdf")
                                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            runCatching { context.startActivity(openIntent) }
                        },
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerView(
    uri: String,
    title: String,
    format: String,
    platform: String,
    fileSizeBytes: Long,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var isPlaying by remember { mutableStateOf(true) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var showControls by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(1.0f) }
    var isMuted by remember { mutableStateOf(false) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    // Auto-update playback progress position
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPosMs = vv.currentPosition.toLong()
                }
            }
            delay(250)
        }
    }

    // Auto-hide controls after 4 seconds of idle
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(HugeIcons.Close, contentDescription = "Close", tint = BrandWhite)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = BrandWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = BrandCoralOrange
                    ) {
                        Text(
                            text = platform,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Normal,
                            color = BrandWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "${format.uppercase()} • ${if (fileSizeBytes > 0) FormattingUtils.formatBytes(fileSizeBytes) else "HD"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandWhite.copy(alpha = 0.8f)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onShare) {
                    Icon(HugeIcons.Share, contentDescription = "Share", tint = BrandWhite)
                }
                IconButton(onClick = onOpenExternal) {
                    Icon(HugeIcons.FileOpen, contentDescription = "External Player", tint = BrandWhite)
                }
            }
        }

        // Center Video Display
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(uri))
                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            durationMs = mp.duration.toLong().coerceAtLeast(0L)
                            mp.isLooping = true
                            start()
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            isPlaying = false
                        }
                        videoViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom Controls Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141416))
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Time Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = FormattingUtils.formatDuration(currentPosMs / 1000),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandWhite
                )
                Text(
                    text = FormattingUtils.formatDuration(durationMs / 1000),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandWhite.copy(alpha = 0.7f)
                )
            }

            // Seek Slider
            Slider(
                value = if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                onValueChange = { frac ->
                    val target = (frac * durationMs).toLong()
                    currentPosMs = target
                    videoViewRef?.seekTo(target.toInt())
                },
                colors = SliderDefaults.colors(
                    thumbColor = BrandCoralOrange,
                    activeTrackColor = BrandCoralOrange,
                    inactiveTrackColor = BrandWhite.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Playback Controls Row: 10s Rewind, Play/Pause, 10s Forward
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        videoViewRef?.let { vv ->
                            val target = (vv.currentPosition - 10000).coerceAtLeast(0)
                            vv.seekTo(target)
                            currentPosMs = target.toLong()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(HugeIcons.ArrowBack, contentDescription = "Rewind 10s", tint = BrandWhite, modifier = Modifier.size(24.dp))
                }

                Spacer(Modifier.width(28.dp))

                Surface(
                    shape = CircleShape,
                    color = BrandCoralOrange,
                    modifier = Modifier
                        .size(CpdSpace.space16x)
                        .clickable {
                            videoViewRef?.let { vv ->
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) HugeIcons.Pause else HugeIcons.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = BrandWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(Modifier.width(28.dp))

                IconButton(
                    onClick = {
                        videoViewRef?.let { vv ->
                            val target = (vv.currentPosition + 10000).coerceAtMost(durationMs.toInt())
                            vv.seekTo(target)
                            currentPosMs = target.toLong()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(HugeIcons.ArrowForward, contentDescription = "Forward 10s", tint = BrandWhite, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            // Secondary Tools Row (Playback speed, Mute, Delete from Device)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Speed Toggle Pill
                    Surface(
                        shape = RoundedCornerShape(CpdRadius.radiusFull),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.clickable {
                            speed = when (speed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 2.0f
                                2.0f -> 0.5f
                                else -> 1.0f
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                mediaPlayerRef?.playbackParams = mediaPlayerRef?.playbackParams?.setSpeed(speed)
                                    ?: android.media.PlaybackParams().setSpeed(speed)
                            }
                        }
                    ) {
                        Text(
                            text = "${speed}x Speed",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandWhite,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Mute button
                    IconButton(onClick = {
                        isMuted = !isMuted
                        if (isMuted) {
                            mediaPlayerRef?.setVolume(0f, 0f)
                        } else {
                            mediaPlayerRef?.setVolume(1f, 1f)
                        }
                    }) {
                        Icon(
                            imageVector = if (isMuted) HugeIcons.Cancel else HugeIcons.Headphones,
                            contentDescription = "Mute",
                            tint = BrandWhite
                        )
                    }
                }

                if (onDelete != null) {
                    TextButton(onClick = {
                        onDelete()
                        onDismiss()
                    }) {
                        Icon(HugeIcons.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete from Gallery", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageViewerView(
    uri: String,
    title: String,
    format: String,
    fileSizeBytes: Long,
    platform: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = uri,
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        if (scale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(HugeIcons.Close, contentDescription = "Close", tint = BrandWhite)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = BrandWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${platform} • ${format.uppercase()} • ${if (fileSizeBytes > 0) FormattingUtils.formatBytes(fileSizeBytes) else "Image"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandWhite.copy(alpha = 0.8f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onShare) {
                    Icon(HugeIcons.Share, contentDescription = "Share", tint = BrandWhite)
                }
                if (onDelete != null) {
                    IconButton(onClick = { onDelete(); onDismiss() }) {
                        Icon(HugeIcons.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioPlayerView(
    uri: String,
    title: String,
    format: String,
    fileSizeBytes: Long,
    platform: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onDelete: (() -> Unit)?
) {
    var isPlaying by remember { mutableStateOf(true) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val context = LocalContext.current

    DisposableEffect(uri) {
        val mp = MediaPlayer().apply {
            setDataSource(context, Uri.parse(uri))
            setOnPreparedListener {
                durationMs = it.duration.toLong().coerceAtLeast(0L)
                it.isLooping = true
                it.start()
                isPlaying = true
            }
            prepareAsync()
        }
        mediaPlayer = mp
        onDispose {
            mp.stop()
            mp.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                if (mp.isPlaying) {
                    currentPosMs = mp.currentPosition.toLong()
                }
            }
            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(HugeIcons.Close, contentDescription = "Close", tint = BrandWhite)
            }
            Text(platform, style = MaterialTheme.typography.labelLarge, color = BrandCoralOrange)
            IconButton(onClick = onShare) {
                Icon(HugeIcons.Share, contentDescription = "Share", tint = BrandWhite)
            }
        }

        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = HugeIcons.Headphones,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = BrandCoralOrange
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                color = BrandWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${format.uppercase()} Audio Track • ${FormattingUtils.formatBytes(fileSizeBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandWhite.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(16.dp))

            Slider(
                value = if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                onValueChange = { frac ->
                    val target = (frac * durationMs).toInt()
                    currentPosMs = target.toLong()
                    mediaPlayer?.seekTo(target)
                },
                colors = SliderDefaults.colors(
                    thumbColor = BrandCoralOrange,
                    activeTrackColor = BrandCoralOrange,
                    inactiveTrackColor = BrandWhite.copy(alpha = 0.3f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(FormattingUtils.formatDuration(currentPosMs / 1000), color = BrandWhite)
                Text(FormattingUtils.formatDuration(durationMs / 1000), color = BrandWhite.copy(alpha = 0.7f))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    mediaPlayer?.let { mp ->
                        val target = (mp.currentPosition - 10000).coerceAtLeast(0)
                        mp.seekTo(target)
                        currentPosMs = target.toLong()
                    }
                }) {
                    Icon(HugeIcons.ArrowBack, contentDescription = "Rewind", tint = BrandWhite, modifier = Modifier.size(32.dp))
                }

                Surface(
                    shape = CircleShape,
                    color = BrandCoralOrange,
                    modifier = Modifier
                        .size(64.dp)
                        .clickable {
                            mediaPlayer?.let { mp ->
                                if (mp.isPlaying) {
                                    mp.pause()
                                    isPlaying = false
                                } else {
                                    mp.start()
                                    isPlaying = true
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) HugeIcons.Pause else HugeIcons.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = BrandWhite,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(onClick = {
                    mediaPlayer?.let { mp ->
                        val target = (mp.currentPosition + 10000).coerceAtMost(durationMs.toInt())
                        mp.seekTo(target)
                        currentPosMs = target.toLong()
                    }
                }) {
                    Icon(HugeIcons.ArrowForward, contentDescription = "Forward", tint = BrandWhite, modifier = Modifier.size(32.dp))
                }
            }
        }

        if (onDelete != null) {
            TextButton(onClick = { onDelete(); onDismiss() }) {
                Icon(HugeIcons.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text("Delete audio file", color = MaterialTheme.colorScheme.error)
            }
        } else {
            Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable
private fun DocumentPdfView(
    uri: String,
    title: String,
    format: String,
    fileSizeBytes: Long,
    platform: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(HugeIcons.Close, contentDescription = "Close", tint = BrandWhite)
            }
            Surface(shape = RoundedCornerShape(CpdRadius.radiusFull), color = BrandCoralOrange) {
                Text(
                    text = "PDF DOCUMENT",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandWhite,
                    modifier = Modifier.padding(horizontal = CpdSpace.space2x, vertical = CpdSpace.space1x)
                )
            }
            IconButton(onClick = onShare) {
                Icon(HugeIcons.Share, contentDescription = "Share", tint = BrandWhite)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CpdSpace.space4x)
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(CpdRadius.radiusXl))
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(HugeIcons.FileOpen, contentDescription = null, modifier = Modifier.size(72.dp), tint = BrandCoralOrange)
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Normal,
                color = BrandWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Downloaded from $platform • ${FormattingUtils.formatBytes(fileSizeBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandWhite.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(8.dp))

            BrandButton(
                onClick = onOpenExternal,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(HugeIcons.FileOpen, null, tint = BrandWhite)
                Spacer(Modifier.width(8.dp))
                Text("Open PDF Document", color = BrandWhite)
            }
        }

        if (onDelete != null) {
            TextButton(onClick = { onDelete(); onDismiss() }) {
                Icon(HugeIcons.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text("Delete PDF from device", color = MaterialTheme.colorScheme.error)
            }
        } else {
            Spacer(Modifier.height(1.dp))
        }
    }
}
