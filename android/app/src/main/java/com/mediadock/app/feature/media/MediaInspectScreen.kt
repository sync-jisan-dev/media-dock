package com.mediadock.app.feature.media

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mediadock.app.R
import com.mediadock.app.core.common.Resource
import com.mediadock.app.core.designsystem.components.MediaDockDownloadAction
import com.mediadock.app.core.designsystem.components.MediaDockTransferStatus
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.CpdBorderWidth
import com.mediadock.app.core.designsystem.theme.CpdRadius
import com.mediadock.app.core.designsystem.theme.CpdSpace
import com.mediadock.app.domain.model.ExtractedImage
import com.mediadock.app.domain.model.MediaDetails

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MediaInspectOptions(
    details: MediaDetails?,
    images: Resource<List<ExtractedImage>>,
    viewModel: MediaInspectViewModel
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val busy by viewModel.isSubmitting.collectAsState()

    if (details == null) {
        ImagesTabContent(images, viewModel, busy)
        return
    }

    val videoFormats = details.videoFormats.filterNot { it == "gif" }
    val tabs = buildList {
        add(InspectTab.ALL to "All")
        if (videoFormats.isNotEmpty()) add(InspectTab.VIDEO to "Video")
        if (details.audioFormats.isNotEmpty()) add(InspectTab.AUDIO to "Audio")
        if ("gif" in details.videoFormats) add(InspectTab.GIF to "GIF")
        add(InspectTab.IMAGES to "Images")
    }
    val activeTab = selectedTab.takeIf { tab -> tabs.any { it.first == tab } } ?: InspectTab.ALL

    Text("Download options", style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        tabs.forEach { (tab, label) ->
            FilterChip(selected = activeTab == tab, onClick = { viewModel.selectTab(tab) }, label = { Text(label) })
        }
    }
    when (activeTab) {
        InspectTab.ALL -> {
            if (videoFormats.isNotEmpty()) VideoOptions(details, videoFormats, viewModel, busy)
            if (videoFormats.isNotEmpty() && details.audioFormats.isNotEmpty()) HorizontalDivider()
            if (details.audioFormats.isNotEmpty()) AudioOptions(details, viewModel, busy)
            if (videoFormats.isEmpty() && details.audioFormats.isEmpty()) {
                if ("gif" in details.videoFormats) GifTabContent(details, viewModel, busy)
                else ImagesTabContent(images, viewModel, busy)
            }
        }
        InspectTab.VIDEO -> VideoOptions(details, videoFormats, viewModel, busy)
        InspectTab.AUDIO -> AudioOptions(details, viewModel, busy)
        InspectTab.GIF -> GifTabContent(details, viewModel, busy)
        InspectTab.IMAGES -> ImagesTabContent(images, viewModel, busy)
    }
}

@Composable
private fun VideoOptions(
    details: MediaDetails,
    formats: List<String>,
    viewModel: MediaInspectViewModel,
    busy: Boolean
) {
    val quality by viewModel.selectedVideoQuality.collectAsState()
    val format by viewModel.selectedVideoFormat.collectAsState()
    val embedSubtitles by viewModel.embedSubtitles.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Video", style = MaterialTheme.typography.titleMedium)
        FormatChoices(stringResource(R.string.quality), details.videoQualities, quality, busy) {
            viewModel.selectedVideoQuality.value = it
        }
        FormatChoices(stringResource(R.string.format), formats, format, busy) {
            viewModel.selectedVideoFormat.value = it
        }
        if (details.subtitles.isNotEmpty()) {
            OptionSwitch("Embed subtitles", embedSubtitles, busy) { viewModel.embedSubtitles.value = it }
        }
        MediaDockDownloadAction("Download video", busy, { viewModel.downloadVideo(details) })
    }
}

@Composable
private fun AudioOptions(details: MediaDetails, viewModel: MediaInspectViewModel, busy: Boolean) {
    val quality by viewModel.selectedAudioQuality.collectAsState()
    val format by viewModel.selectedAudioFormat.collectAsState()
    val embedThumbnail by viewModel.embedAudioThumbnail.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Audio", style = MaterialTheme.typography.titleMedium)
        FormatChoices(stringResource(R.string.format), details.audioFormats, format, busy) {
            viewModel.selectedAudioFormat.value = it
        }
        FormatChoices("Bitrate / quality", details.audioQualities, quality, busy) {
            viewModel.selectedAudioQuality.value = it
        }
        OptionSwitch("Embed cover art", embedThumbnail, busy) { viewModel.embedAudioThumbnail.value = it }
        MediaDockDownloadAction("Download audio", busy, { viewModel.downloadAudio(details) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FormatChoices(
    title: String,
    choices: List<String>,
    selected: String,
    busy: Boolean,
    onSelect: (String) -> Unit
) {
    if (choices.isEmpty()) return

    Column {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.distinct().forEach { choice ->
                FilterChip(
                    selected = selected == choice,
                    enabled = !busy,
                    onClick = { onSelect(choice) },
                    label = { Text(if (choice == "highest") "Best available" else choice.uppercase()) }
                )
            }
        }
    }
}

@Composable
private fun OptionSwitch(label: String, checked: Boolean, busy: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = !busy)
    }
}

@Composable
private fun GifTabContent(details: MediaDetails, viewModel: MediaInspectViewModel, busy: Boolean) {
    val startTime by viewModel.gifStartTime.collectAsState()
    val endTime by viewModel.gifEndTime.collectAsState()
    val maxDuration = (details.durationSeconds ?: 60.0).toFloat().coerceAtLeast(10f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        MediaDockDownloadAction(stringResource(R.string.gif_generate), busy, { viewModel.downloadGif(details) })
    }
}

@Composable
private fun ImagesTabContent(
    images: Resource<List<ExtractedImage>>,
    viewModel: MediaInspectViewModel,
    busy: Boolean
) {
    when (images) {
        Resource.Loading -> MediaDockTransferStatus("Looking for images")
        is Resource.Error -> Text(images.message, color = MaterialTheme.colorScheme.error)
        is Resource.Success -> {
            if (images.data.isEmpty()) Text("No images found.")
            else ImagesGridView(images.data, viewModel, busy)
        }
        Resource.Idle -> Unit
    }
}

@Composable
private fun ImagesGridView(
    images: List<ExtractedImage>,
    viewModel: MediaInspectViewModel,
    busy: Boolean
) {
    val selectedIds by viewModel.selectedImageIds.collectAsState()

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedIds.size} of ${images.size} selected",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(enabled = !busy, onClick = { viewModel.selectAllImages(images) }) {
                Text(text = if (selectedIds.size == images.size) "Deselect All" else "Select All")
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
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
                        .clickable(enabled = !busy) { viewModel.toggleImageSelection(image.id) }
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
            MediaDockDownloadAction(
                "Download ${selectedIds.size} selected",
                busy,
                { viewModel.downloadSelectedImages(images) }
            )
        }
    }
}
