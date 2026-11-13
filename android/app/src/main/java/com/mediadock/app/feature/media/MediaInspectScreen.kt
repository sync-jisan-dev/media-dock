package com.mediadock.app.feature.media

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Gif
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
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
import com.mediadock.app.core.designsystem.components.MediaDockEmptyState
import com.mediadock.app.core.designsystem.components.MediaDockLoadingIndicator
import com.mediadock.app.core.designsystem.theme.BrightFern
import com.mediadock.app.core.designsystem.theme.Chartreuse
import com.mediadock.app.core.designsystem.theme.DarkSpruce
import com.mediadock.app.core.designsystem.theme.ForestGreen
import com.mediadock.app.core.designsystem.theme.RadioactiveGrass
import com.mediadock.app.core.designsystem.theme.SlimeLime
import com.mediadock.app.core.designsystem.theme.bouncyClickable
import com.mediadock.app.domain.model.ExtractedImage
import com.mediadock.app.domain.model.MediaDetails

@Composable
fun MediaInspectScreen(
    viewModel: MediaInspectViewModel,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaResource by viewModel.mediaDetailsState.collectAsState()
    val imagesResource by viewModel.extractedImagesState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { target ->
            if (target == "downloads") onNavigateToDownloads()
        }
    }

    when (val resource = mediaResource) {
        is Resource.Loading -> {
            MediaDockLoadingIndicator(message = "Analyzing media link...")
        }
        is Resource.Error -> {
            if (imagesResource is Resource.Success && (imagesResource as Resource.Success<List<ExtractedImage>>).data.isNotEmpty()) {
                val images = (imagesResource as Resource.Success<List<ExtractedImage>>).data
                ImagesOnlyView(
                    images = images,
                    viewModel = viewModel,
                    modifier = modifier
                )
            } else {
                MediaDockEmptyState(
                    icon = Icons.Default.ErrorOutline,
                    title = "Analysis Failed",
                    message = resource.message,
                    actionLabel = "Try Again",
                    onActionClick = { viewModel.retry() }
                )
            }
        }
        is Resource.Success -> {
            val details = resource.data
            Column(modifier = modifier.fillMaxSize()) {
                // Header Thumbnail & Title
                MediaHeaderCard(details = details)

                // Navigation Tabs with Spring Indicator
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = SlimeLime,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = SlimeLime,
                            height = 3.dp
                        )
                    }
                ) {
                    InspectTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = {
                                Text(
                                    text = tab.name,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == tab) SlimeLime else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                // Tab Content with animated slide
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)) + slideInVertically { 30 }
                ) {
                    when (selectedTab) {
                        InspectTab.ALL -> AllTabContent(details, viewModel, imagesResource)
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
                        .size(92.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.5.dp, RadioactiveGrass.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = details.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (!details.uploader.isNullOrBlank()) {
                    Text(
                        text = details.uploader,
                        style = MaterialTheme.typography.bodySmall,
                        color = SlimeLime,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (details.durationSeconds != null && details.durationSeconds > 0) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSpruce
                    ) {
                        Text(
                            text = "⏱ ${FormattingUtils.formatDuration(details.durationSeconds)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Chartreuse,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = stringResource(R.string.quality), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                details.videoQualities.ifEmpty { listOf("highest", "1080p", "720p", "480p") }.forEach { q ->
                    FilterChip(
                        selected = selectedQuality == q,
                        onClick = { viewModel.selectedVideoQuality.value = q },
                        label = { Text(text = q.uppercase(), fontWeight = FontWeight.Bold) },
                        leadingIcon = if (selectedQuality == q) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SlimeLime,
                            selectedLabelColor = DarkSpruce,
                            selectedLeadingIconColor = DarkSpruce
                        )
                    )
                }
            }
        }

        item {
            Text(text = stringResource(R.string.format), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                details.videoFormats.ifEmpty { listOf("mp4", "webm", "mkv") }.forEach { fmt ->
                    FilterChip(
                        selected = selectedFormat == fmt,
                        onClick = { viewModel.selectedVideoFormat.value = fmt },
                        label = { Text(text = fmt.uppercase(), fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Chartreuse,
                            selectedLabelColor = DarkSpruce
                        )
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            checkedThumbColor = DarkSpruce,
                            checkedTrackColor = SlimeLime
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.downloadVideo(details) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .bouncyClickable { viewModel.downloadVideo(details) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SlimeLime,
                    contentColor = DarkSpruce
                )
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = DarkSpruce)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Download Video (${selectedQuality.uppercase()})", fontWeight = FontWeight.Black, color = DarkSpruce)
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Audio Format", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                details.audioFormats.ifEmpty { listOf("mp3", "m4a", "opus", "wav", "flac") }.forEach { fmt ->
                    FilterChip(
                        selected = selectedFormat == fmt,
                        onClick = { viewModel.selectedAudioFormat.value = fmt },
                        label = { Text(text = fmt.uppercase(), fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Chartreuse,
                            selectedLabelColor = DarkSpruce
                        )
                    )
                }
            }
        }

        item {
            Text(text = "Bitrate / Quality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                details.audioQualities.ifEmpty { listOf("best", "high", "medium", "low") }.forEach { q ->
                    FilterChip(
                        selected = selectedQuality == q,
                        onClick = { viewModel.selectedAudioQuality.value = q },
                        label = { Text(text = q.replaceFirstChar { it.uppercase() }) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SlimeLime,
                            selectedLabelColor = DarkSpruce
                        )
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            checkedThumbColor = DarkSpruce,
                            checkedTrackColor = SlimeLime
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.downloadAudio(details) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .bouncyClickable { viewModel.downloadAudio(details) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SlimeLime,
                    contentColor = DarkSpruce
                )
            ) {
                Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = DarkSpruce)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Download Audio (${selectedFormat.uppercase()})", fontWeight = FontWeight.Black, color = DarkSpruce)
            }
        }
    }
}

@Composable
private fun GifTabContent(details: MediaDetails, viewModel: MediaInspectViewModel) {
    val startTime by viewModel.gifStartTime.collectAsState()
    val endTime by viewModel.gifEndTime.collectAsState()
    val maxDuration = (details.durationSeconds ?: 60L).toFloat().coerceAtLeast(10f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = stringResource(R.string.gif_creator_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Trim and convert video sections into high-quality animated GIFs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Start Time: ${startTime}s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Slider(
                        value = startTime.toFloat(),
                        onValueChange = { viewModel.gifStartTime.value = it.toLong() },
                        valueRange = 0f..maxDuration.coerceAtMost(300f),
                        colors = SliderDefaults.colors(
                            thumbColor = SlimeLime,
                            activeTrackColor = SlimeLime,
                            inactiveTrackColor = ForestGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "End Time: ${endTime}s (Duration: ${endTime - startTime}s)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Slider(
                        value = endTime.toFloat(),
                        onValueChange = {
                            val newEnd = it.toLong()
                            if (newEnd > startTime) viewModel.gifEndTime.value = newEnd
                        },
                        valueRange = 0f..maxDuration.coerceAtMost(300f),
                        colors = SliderDefaults.colors(
                            thumbColor = Chartreuse,
                            activeTrackColor = Chartreuse,
                            inactiveTrackColor = ForestGreen
                        )
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.downloadGif(details) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .bouncyClickable { viewModel.downloadGif(details) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SlimeLime,
                    contentColor = DarkSpruce
                )
            ) {
                Icon(imageVector = Icons.Default.Gif, contentDescription = null, modifier = Modifier.size(26.dp), tint = DarkSpruce)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.gif_generate), fontWeight = FontWeight.Black, color = DarkSpruce)
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
            icon = Icons.Default.Image,
            title = "No Images Found",
            message = imagesResource.message
        )
        is Resource.Success -> {
            val images = imagesResource.data
            if (images.isEmpty()) {
                MediaDockEmptyState(
                    icon = Icons.Default.Image,
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
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) SlimeLime else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
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
                            color = SlimeLime,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = DarkSpruce,
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
                Button(
                    onClick = { viewModel.downloadSelectedImages(images) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                        .bouncyClickable { viewModel.downloadSelectedImages(images) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SlimeLime,
                        contentColor = DarkSpruce
                    )
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, tint = DarkSpruce)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Download ${selectedIds.size} Selected Images", fontWeight = FontWeight.Black, color = DarkSpruce)
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
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Webpage Images Detected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
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
    viewModel: MediaInspectViewModel,
    imagesResource: Resource<List<ExtractedImage>>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable { viewModel.selectTab(InspectTab.VIDEO) }
                    .border(1.dp, RadioactiveGrass.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSpruce),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Movie, contentDescription = null, tint = SlimeLime, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Download Video", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Available up to ${details.videoQualities.firstOrNull() ?: "1080p"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { viewModel.downloadVideo(details) },
                        colors = ButtonDefaults.buttonColors(containerColor = SlimeLime, contentColor = DarkSpruce)
                    ) {
                        Text(text = "Get Video", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable { viewModel.selectTab(InspectTab.AUDIO) }
                    .border(1.dp, BrightFern.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSpruce),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Audiotrack, contentDescription = null, tint = Chartreuse, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Extract Audio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Direct MP3 / M4A extraction with album art", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { viewModel.downloadAudio(details) },
                        colors = ButtonDefaults.buttonColors(containerColor = Chartreuse, contentColor = DarkSpruce)
                    ) {
                        Text(text = "Get Audio", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable { viewModel.selectTab(InspectTab.GIF) }
                    .border(1.dp, ForestGreen.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSpruce),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Gif, contentDescription = null, tint = RadioactiveGrass, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Create Animated GIF", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = "Convert video clip to GIF", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { viewModel.selectTab(InspectTab.GIF) },
                        colors = ButtonDefaults.buttonColors(containerColor = RadioactiveGrass, contentColor = DarkSpruce)
                    ) {
                        Text(text = "Create GIF", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
