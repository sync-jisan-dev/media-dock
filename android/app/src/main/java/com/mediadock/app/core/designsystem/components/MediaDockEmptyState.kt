package com.mediadock.app.core.designsystem.components

import com.mediadock.app.core.designsystem.theme.*

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mediadock.app.R
import org.json.JSONObject

@Composable
fun MediaDockEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    illustration: IllustrationScene? = null,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
    illustrationSize: androidx.compose.ui.unit.Dp = 168.dp,
    titleStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    messageStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    val cleanMessage = remember(message) {
        val trimmed = message.trim()
        if (trimmed.startsWith("{") || trimmed.contains("\"statusCode\"") || trimmed.contains("\"error\"")) {
            try {
                val obj = JSONObject(trimmed)
                val msg = obj.optString("message")
                when {
                    msg.contains("Failed to extract", ignoreCase = true) || msg.contains("yt-dlp", ignoreCase = true) ->
                        "MediaDock could not extract media from this link. The video may be restricted, private, or temporarily unavailable."
                    msg.isNotBlank() && !msg.startsWith("{") && msg.length < 160 ->
                        msg
                    else ->
                        "Could not process media link. Please verify the URL and try again."
                }
            } catch (_: Exception) {
                "Could not process media link. Please verify the URL and try again."
            }
        } else {
            message
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (illustration != null) {
                AppIllustration(
                    scene = illustration,
                    modifier = Modifier.size(illustrationSize),
                    showContainer = false
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(CpdRadius.radiusLg))
                        .background(CpdColor.bgSubtleSecondary)
                        .border(CpdBorderWidth.hairline, CpdColor.borderInteractiveSecondary, RoundedCornerShape(CpdRadius.radiusLg)),
                    contentAlignment = Alignment.Center
                ) {
                    AsciiHeroAnimation(
                        modifier = Modifier.fillMaxSize(),
                        isDark = CpdColor.isDark,
                        interactive = true
                    )
                    Icon(
                        icon,
                        null,
                        Modifier.size(32.dp),
                        tint = CpdColor.CoralRest
                    )
                }
            }
            Spacer(Modifier.height(CpdSpace.space4x))
            Text(
                title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(CpdSpace.space2x))
            Text(
                cleanMessage,
                style = messageStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!actionLabel.isNullOrBlank()) {
                Spacer(Modifier.height(CpdSpace.space5x))
                BrandButton(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth().height(CpdSize.buttonLarge),
                    shape = RoundedCornerShape(CpdRadius.radiusFull)
                ) { Text(actionLabel) }
            }
            if (!secondaryActionLabel.isNullOrBlank() && onSecondaryActionClick != null) {
                Spacer(Modifier.height(CpdSpace.space3x))
                OutlinedButton(
                    onClick = onSecondaryActionClick,
                    modifier = Modifier.fillMaxWidth().height(CpdSize.buttonLarge),
                    shape = RoundedCornerShape(CpdRadius.radiusFull)
                ) { Text(secondaryActionLabel) }
            }
        }
    }
}
