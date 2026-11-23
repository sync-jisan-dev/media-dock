package com.mediadock.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mediadock.app.R
import com.mediadock.app.core.designsystem.theme.*

enum class IllustrationScene(val drawable: Int) {
    HIPPO_MEDIA(R.drawable.hippo_media),
    HIPPO_BROWSE(R.drawable.hippo_browse),
    HIPPO_QUEUE(R.drawable.hippo_queue),
    HIPPO_ERROR(R.drawable.hippo_error),
    HIPPO_LIBRARY(R.drawable.hippo_library),
    HOME(R.drawable.illustration_home),
    DOWNLOADS(R.drawable.illustration_downloads),
    ROVER_STREAM(R.drawable.illustration_rover_stream),
    INSPECT(R.drawable.illustration_inspect),
    COSMIC_PULSE(R.drawable.illustration_cosmic_pulse),
    LIBRARY(R.drawable.illustration_library),
    BROWSE(R.drawable.illustration_browse),
    CONNECTION(R.drawable.illustration_connection),
    WORKSPACE(R.drawable.illustration_workspace),
    AUDIO(R.drawable.illustration_audio),
    VIDEO(R.drawable.illustration_video)
}

@Composable fun AppIllustration(
    scene: IllustrationScene,
    modifier: Modifier = Modifier,
    showContainer: Boolean = true
) {
    val containerModifier = if (showContainer) {
        modifier
            .clip(RoundedCornerShape(CpdRadius.radiusXl))
            .background(BrandMintTint)
            .border(CpdBorderWidth.hairline * 2, BrandSageMint, RoundedCornerShape(CpdRadius.radiusXl))
    } else {
        modifier
    }

    Box(containerModifier, contentAlignment = Alignment.Center) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(scene.drawable)
                .crossfade(false)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showContainer) 12.dp else 4.dp)
        )
    }
}
