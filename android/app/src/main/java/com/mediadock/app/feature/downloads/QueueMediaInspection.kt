package com.mediadock.app.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.common.Resource
import com.mediadock.app.core.designsystem.components.MediaDockTransferStatus
import com.mediadock.app.core.designsystem.components.MediaThumbnail
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.feature.browser.BrowserRules
import com.mediadock.app.feature.media.MediaInspectOptions
import com.mediadock.app.feature.media.MediaInspectViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QueueMediaInspection(
    url: String,
    viewModel: MediaInspectViewModel,
    onNavigateToSettings: () -> Unit
) {
    val media by viewModel.mediaDetailsState.collectAsState()
    val images by viewModel.extractedImagesState.collectAsState()
    val quickPreparing by viewModel.quickPreparing.collectAsState()
    val submitting by viewModel.isSubmitting.collectAsState()
    val downloadError by viewModel.downloadError.collectAsState()
    val lookingForImages = media is Resource.Error && images is Resource.Loading
    val busy = media is Resource.Loading || lookingForImages || submitting || quickPreparing
    val details = (media as? Resource.Success)?.data
    val hasMedia = details != null
    val hasImages = (images as? Resource.Success)?.data?.isNotEmpty() == true
    val ready = hasMedia || hasImages
    val analysisError = (media as? Resource.Error)?.message?.takeUnless { busy || ready }
    val connectionError = analysisError?.let {
        it.contains("reach", ignoreCase = true) || it.contains("connect", ignoreCase = true) ||
            it.contains("Settings", ignoreCase = true)
    } == true

    QueueCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MediaThumbnail(
                model = details?.thumbnail,
                title = details?.title.orEmpty(),
                mediaType = if (hasImages && !hasMedia) "image" else "video",
                modifier = Modifier.size(width = 72.dp, height = 52.dp)
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    details?.title?.takeIf { it.isNotBlank() } ?: "Media link",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    BrowserRules.displayHost(url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = viewModel::dismissInspection, enabled = !submitting) {
                Icon(HugeIcons.Close, contentDescription = "Dismiss media link")
            }
        }
        when {
            busy -> MediaDockTransferStatus(
                label = when {
                    submitting || (quickPreparing && ready) -> "Adding to queue"
                    lookingForImages -> "Looking for images"
                    else -> "Checking available formats"
                }
            )
            analysisError != null -> {
                Text(analysisError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = viewModel::retry) { Text("Try again") }
                    if (connectionError) {
                        TextButton(onClick = onNavigateToSettings) { Text("Server settings") }
                    }
                }
            }
            ready && downloadError != null -> {
                Text(downloadError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (ready && !quickPreparing) {
        QueueCard {
            MediaInspectOptions(details, images, viewModel)
        }
    }
}
