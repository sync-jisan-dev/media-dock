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
import androidx.compose.ui.unit.dp

/** Coil decodes cached remote artwork, image files, and frames from local videos. */
@Composable fun MediaThumbnail(model: String?, title: String, mediaType: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Icon(if (mediaType == "audio") HugeIcons.Headphones else HugeIcons.PlayCircle, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!model.isNullOrBlank()) AsyncImage(model = model, contentDescription = "Preview of $title", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}
