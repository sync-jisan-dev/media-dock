package com.mediadock.app.feature.media

import com.mediadock.app.core.designsystem.components.BrandButton

import com.mediadock.app.core.designsystem.icons.HugeIcons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mediadock.app.R
import com.mediadock.app.core.common.FormattingUtils
import com.mediadock.app.core.common.Resource
import com.mediadock.app.core.designsystem.components.MediaDockPillTabs
import com.mediadock.app.core.designsystem.components.MediaDockDownloadAction
import com.mediadock.app.core.designsystem.components.MediaDockEmptyState
import com.mediadock.app.core.designsystem.components.MediaDockLoadingIndicator
import com.mediadock.app.core.designsystem.theme.*
import com.mediadock.app.domain.model.ExtractedImage
import com.mediadock.app.domain.model.MediaDetails

@Composable
fun MediaInspectScreen(
    viewModel: MediaInspectViewModel,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val mediaResource by viewModel.mediaDetailsState.collectAsState()
    val imagesResource by viewModel.extractedImagesState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val submitting by viewModel.isSubmitting.collectAsState()
    val quickPreparing by viewModel.quickPreparing.collectAsState()
    val downloadError by viewModel.downloadError.collectAsState()
    val snackbar = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }
    LaunchedEffect(downloadError) {
        downloadError?.let { snackbar.showSnackbar(it); viewModel.downloadError.value = null }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { target ->
            if (target == "downloads") onNavigateToDownloads()
        }
    }

    Box(modifier.fillMaxSize()) {
        when (val resource = mediaResource) {
            is Resource.Loading -> {
                MediaPreparationScreen(sourceUrl = viewModel.sourceUrl, onViewQueue = onNavigateToDownloads)
            }
            is Resource.Error -> {
                if (imagesResource is Resource.Loading) {
                    MediaPreparationScreen(phase = "Looking for images", sourceUrl = viewModel.sourceUrl, onViewQueue = onNavigateToDownloads)
                } else if (imagesResource is Resource.Success && (imagesResource as Resource.Success<List<ExtractedImage>>).data.isNotEmpty()) {
                    val images = (imagesResource as Resource.Success<List<ExtractedImage>>).data
                    ImagesOnlyView(
                        images = images,
                        viewModel = viewModel,
                        modifier = modifier
                    )
                } else {
                    val isConnectionError = resource.message.contains("Cannot reach", ignoreCase = true) ||
                            resource.message.contains("connect", ignoreCase = true) ||
                            resource.message.contains("Settings", ignoreCase = true)
                    MediaDockEmptyState(
                        icon = HugeIcons.ErrorOutline,
                        title = if (isConnectionError) "Connection Failed" else "Analysis Failed",
                        message = resource.message,
                        actionLabel = "Try Again",
                        onActionClick = { viewModel.retry() },
                        secondaryActionLabel = if (isConnectionError && onNavigateToSettings != null) "Check Server Settings" else null,
                        onSecondaryActionClick = onNavigateToSettings
                    )
                }
            }
            is Resource.Success -> {
                val details = resource.data
                val availableTabs = buildList {
                    add(InspectTab.ALL)
                    if (details.videoFormats.isNotEmpty()) add(InspectTab.VIDEO)
                    if (details.audioFormats.isNotEmpty()) add(InspectTab.AUDIO)
                    add(InspectTab.IMAGES)
                    if ("gif" in details.videoFormats) add(InspectTab.GIF)
                }
                val activeTab = selectedTab.takeIf { it in availableTabs } ?: InspectTab.ALL
                Column(modifier = modifier.fillMaxSize()) {
                    // Header Thumbnail & Title
                    MediaHeaderCard(details = details)

                    MediaDockPillTabs(
                        labels = availableTabs.map { tab ->
                            if (tab == InspectTab.GIF) "GIF" else tab.name.lowercase().replaceFirstChar { it.uppercase() }
                        },
                        selectedIndex = availableTabs.indexOf(activeTab),
                        onSelected = { viewModel.selectTab(availableTabs[it]) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    // Tab Content with animated slide
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) + slideInVertically { 30 }
                    ) {
                        when (activeTab) {
                            InspectTab.ALL -> AllTabContent(details, viewModel)
                            InspectTab.VIDEO -> VideoTabContent(details, viewModel)
                            InspectTab.AUDIO -> AudioTabContent(details, viewModel)
                            InspectTab.IMAGES -> ImagesTabContent(imagesResource, viewModel)
                            InspectTab.GIF -> GifTabContent(details, viewModel)
                        }
                    }
                }
            }
            is Resource.Idle -> {}
        }
    if ((submitting || quickPreparing) && viewModel.quickDownload && mediaResource is Resource.Success) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MediaPreparationScreen(phase = "Starting your download", sourceUrl = viewModel.sourceUrl, onViewQueue = onNavigateToDownloads) }
    } else if (submitting) {
        androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth())
    }
    androidx.compose.material3.SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}

@Composable
private fun MediaHeaderCard(details: MediaDetails) {
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!details.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = details.thumbnail,
                    contentDescription = details.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(CpdRadius.radiusMd))
                        .border(CpdBorderWidth.focus, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(CpdRadius.radiusMd))
                )
                Spacer(modifier = Modifier.width(CpdSpace.space4x))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(CpdSpace.space1x))
                if (!details.uploader.isNullOrBlank()) {
                    Text(
                        text = details.uploader,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (details.durationSeconds != null && details.durationSeconds > 0) {
                    Spacer(modifier = Modifier.height(CpdSpace.space1x))
                    Surface(
                        shape = RoundedCornerShape(CpdRadius.radiusFull),
                        color = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = "⏱ ${FormattingUtils.formatDuration(details.durationSeconds.roundToLong())}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = CpdSpace.space15x, vertical = CpdSpace.space05x)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoTabContent(details: MediaDetails, viewModel: MediaInspectViewModel) {
    val selectedQuality by viewModel.selectedVideoQuality.collectAsState()
    val selectedFormat by viewModel.selectedVideoFormat.collectAsState()
    val embedSubs by viewModel.embedSubtitles.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(CpdSpace.space4x),
        verticalArrangement = Arrangement.spacedBy(CpdSpace.space4x)
    ) {
        item {
            Text(text = stringResource(R.string.quality), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(CpdSpace.space2x))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
                details.videoQualities.ifEmpty { listOf("highest") }.forEach { q ->
                    FilterChip(
                        selected = selectedQuality == q,
                        onClick = { viewModel.selectedVideoQuality.value = q },
                        label = { Text(text = q.uppercase(), fontWeight = FontWeight.SemiBold) },
                        leadingIcon = if (selectedQuality == q) {
                            { Icon(imageVector = HugeIcons.Check, contentDescription = null, modifier = Modifier.size(CpdSize.iconSm)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        item {
            Text(text = stringResource(R.string.format), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(CpdSpace.space2x))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
                details.videoFormats.ifEmpty { listOf("mp4", "webm", "mkv") }.forEach { fmt ->
                    FilterChip(
                        selected = selectedFormat == fmt,
                        onClick = { viewModel.selectedVideoFormat.value = fmt },
                        label = { Text(text = fmt.uppercase(), fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(CpdRadius.radiusLg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(CpdSpace.space4x),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Embed Subtitles", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(text = "Include available subtitles inside video", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = embedSubs,
                        onCheckedChange = { viewModel.embedSubtitles.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(CpdSpace.space2x))
            BrandButton(
                onClick = { viewModel.downloadVideo(details) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CpdSize.buttonLarge)
                    .bouncyClickable { viewModel.downloadVideo(details) },
                shape = RoundedCornerShape(CpdRadius.radiusFull),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = HugeIcons.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(CpdSpace.space2x))
                Text(text = "Download Video (${selectedQuality.uppercase()})", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioTabContent(details: MediaDetails, viewModel: MediaInspectViewModel) {
    val selectedQuality by viewModel.selectedAudioQuality.collectAsState()
    val selectedFormat by viewModel.selectedAudioFormat.collectAsState()
    val embedArt by viewModel.embedAudioThumbnail.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(CpdSpace.space4x),
        verticalArrangement = Arrangement.spacedBy(CpdSpace.space4x)
    ) {
        item {
            Text(text = "Audio Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(CpdSpace.space2x))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
                details.audioFormats.ifEmpty { listOf("mp3", "m4a", "opus", "wav", "flac") }.forEach { fmt ->
                    FilterChip(
                        selected = selectedFormat == fmt,
                        onClick = { viewModel.selectedAudioFormat.value = fmt },
                        label = { Text(text = fmt.uppercase(), fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        item {
            Text(text = "Bitrate / Quality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(CpdSpace.space2x))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
                details.audioQualities.ifEmpty { listOf("best", "high", "medium", "low") }.forEach { q ->
                    FilterChip(
                        selected = selectedQuality == q,
                        onClick = { viewModel.selectedAudioQuality.value = q },
                        label = { Text(text = q.replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(CpdRadius.radiusLg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(CpdSpace.space4x),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Embed Cover Art", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(text = "Embed album thumbnail into audio tags", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = embedArt,
                        onCheckedChange = { viewModel.embedAudioThumbnail.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(CpdSpace.space2x))
            BrandButton(
                onClick = { viewModel.downloadAudio(details) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CpdSize.buttonLarge)
                    .bouncyClickable { viewModel.downloadAudio(details) },
                shape = RoundedCornerShape(CpdRadius.radiusFull),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = HugeIcons.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(CpdSpace.space2x))
                Text(text = "Download Audio (${selectedFormat.uppercase()})", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun GifTabContent(details: MediaDetails, viewModel: MediaInspectViewModel) {
    val startTime by viewModel.gifStartTime.collectAsState()
    val endTime by viewModel.gifEndTime.collectAsState()
    val maxDuration = (details.durationSeconds ?: 60.0).toFloat().coerceAtLeast(10f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(CpdSpace.space4x),
        verticalArrangement = Arrangement.spacedBy(CpdSpace.space4x)
    ) {
        item {
            Text(text = stringResource(R.string.gif_creator_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(CpdSpace.space1x))
            Text(text = "Trim and convert video sections into high-quality animated GIFs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(
                shape = RoundedCornerShape(CpdRadius.radiusLg),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(CpdSpace.space4x)) {
                    Text(text = "Start Time: ${startTime}s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = startTime.toFloat(),
                        onValueChange = { viewModel.gifStartTime.value = it.toLong() },
                        valueRange = 0f..maxDuration.coerceAtMost(300f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(CpdSpace.space2x))
                    Text(text = "End Time: ${endTime}s (Duration: ${endTime - startTime}s)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = endTime.toFloat(),
                        onValueChange = {
                            val newEnd = it.toLong()
                            if (newEnd > startTime) viewModel.gifEndTime.value = newEnd
                        },
                        valueRange = 0f..maxDuration.coerceAtMost(300f),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(CpdSpace.space2x))
            BrandButton(
                onClick = { viewModel.downloadGif(details) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CpdSize.buttonLarge)
                    .bouncyClickable { viewModel.downloadGif(details) },
                shape = RoundedCornerShape(CpdRadius.radiusFull),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = HugeIcons.Gif, contentDescription = null, modifier = Modifier.size(26.dp), tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(CpdSpace.space2x))
                Text(text = stringResource(R.string.gif_generate), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun ImagesTabContent(
    imagesResource: Resource<List<ExtractedImage>>,
    viewModel: MediaInspectViewModel
) {
    when (imagesResource) {
        is Resource.Loading -> MediaDockLoadingIndicator(message = "Extracting images...")
        is Resource.Error -> MediaDockEmptyState(
            icon = HugeIcons.Image,
            title = "No Images Found",
            message = imagesResource.message
        )
        is Resource.Success -> {
            val images = imagesResource.data
            if (images.isEmpty()) {
                MediaDockEmptyState(
                    icon = HugeIcons.Image,
                    title = "No Images",
                    message = "No images could be extracted from this webpage."
                )
            } else {
                ImagesGridView(images = images, viewModel = viewModel)
            }
        }
        is Resource.Idle -> {}
    }
}

@Composable
private fun ImagesGridView(
    images: List<ExtractedImage>,
    viewModel: MediaInspectViewModel
) {
    val selectedIds by viewModel.selectedImageIds.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${images.size} Images Found (${selectedIds.size} selected)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(onClick = { viewModel.selectAllImages(images) }) {
                Text(text = if (selectedIds.size == images.size) "Deselect All" else "Select All")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(images, key = { it.id }) { image ->
                val isSelected = selectedIds.contains(image.id)
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(CpdRadius.radiusLg))
                        .border(
                            width = if (isSelected) CpdBorderWidth.focus else CpdBorderWidth.hairline * 2,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(CpdRadius.radiusLg)
                        )
                        .clickable { viewModel.toggleImageSelection(image.id) }
                ) {
                    AsyncImage(
                        model = image.url,
                        contentDescription = image.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = HugeIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (selectedIds.isNotEmpty()) {
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                BrandButton(
                    onClick = { viewModel.downloadSelectedImages(images) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(CpdSpace.space4x)
                        .height(CpdSize.buttonLarge)
                        .bouncyClickable { viewModel.downloadSelectedImages(images) },
                    shape = RoundedCornerShape(CpdRadius.radiusFull),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(imageVector = HugeIcons.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(CpdSpace.space2x))
                    Text(text = "Download ${selectedIds.size} Selected Images", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun ImagesOnlyView(
    images: List<ExtractedImage>,
    viewModel: MediaInspectViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(CpdSpace.space4x)) {
                Text(
                    text = "Webpage Images Detected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Select images from this webpage to download in original quality",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        ImagesGridView(images = images, viewModel = viewModel)
    }
}

@Composable
private fun AllTabContent(
    details: MediaDetails,
    viewModel: MediaInspectViewModel
) {
    val busy by viewModel.isSubmitting.collectAsState()
    val videoQuality by viewModel.selectedVideoQuality.collectAsState()
    val audioFormat by viewModel.selectedAudioFormat.collectAsState()
    val videoFormat by viewModel.selectedVideoFormat.collectAsState()
    LazyColumn(contentPadding = PaddingValues(CpdSpace.space5x), verticalArrangement = Arrangement.spacedBy(CpdSpace.space4x)) {
        item { Text("Download media as", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) }
        if (details.audioFormats.isNotEmpty()) item {
            Text("Music", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(CpdSpace.space3x))
            DownloadOptionGrid(details.audioFormats, audioFormat, "Audio format") {
                viewModel.selectedAudioFormat.value = it
            }
            Spacer(Modifier.height(CpdSpace.space2x))
            MediaDockDownloadAction("Download ${audioFormat.uppercase()}", busy, { viewModel.downloadAudio(details) })
        }
        item {
            Text("Video quality", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(CpdSpace.space3x))
            DownloadOptionGrid(details.videoQualities.ifEmpty { listOf("highest") }, videoQuality, videoFormat.uppercase()) {
                viewModel.selectedVideoQuality.value = it
            }
            Spacer(Modifier.height(CpdSpace.space3x))
            MediaDockDownloadAction("Download video", busy, { viewModel.downloadVideo(details) })
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(CpdSpace.space3x)) {
                if ("gif" in details.videoFormats) OutlinedButton(onClick = { viewModel.selectTab(InspectTab.GIF) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(CpdRadius.radiusFull)) {
                    Icon(HugeIcons.Gif, null)
                    Spacer(Modifier.width(CpdSpace.space15x))
                    Text("Create GIF")
                }
                OutlinedButton(onClick = { viewModel.selectTab(InspectTab.IMAGES) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(CpdRadius.radiusFull)) {
                    Icon(HugeIcons.Image, null)
                    Spacer(Modifier.width(CpdSpace.space15x))
                    Text("Images")
                }
            }
        }
    }
}

@Composable
private fun DownloadOptionGrid(options: List<String>, selected: String, subtitle: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
                row.forEach { option ->
                    val active = option == selected
                    Surface(
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(CpdRadius.radiusLg),
                        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (active) CpdBorderWidth.focus else CpdBorderWidth.hairline * 2,
                            if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(Modifier.padding(horizontal = CpdSpace.space15x, vertical = CpdSpace.space4x), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (option == "highest") "BEST" else option.uppercase(), style = MaterialTheme.typography.titleSmall, color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = if (active) MaterialTheme.colorScheme.onPrimary.copy(alpha = .7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
