package com.mediadock.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.CpdRadius
import androidx.compose.ui.unit.dp

/** Coil decodes cached remote artwork, image files, and frames from local videos. */
@Composable fun MediaThumbnail(model: String?, title: String, mediaType: String, modifier: Modifier = Modifier) {
    val type = mediaType.lowercase()
    val isAudio = type == "audio"
    val isDoc = type in listOf("document", "pdf", "doc")
    val isImage = type in listOf("image", "gif")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CpdRadius.radiusMd))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val fallbackIcon = when {
            isAudio -> HugeIcons.Headphones
            isDoc -> HugeIcons.FileOpen
            isImage -> HugeIcons.Image
            else -> HugeIcons.PlayCircle
        }
        Icon(
            imageVector = fallbackIcon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        if (!model.isNullOrBlank()) {
            AsyncImage(
                model = model,
                contentDescription = "Preview of $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
