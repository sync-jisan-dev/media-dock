package com.mediadock.app.core.designsystem.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mediadock.app.R
import com.mediadock.app.core.designsystem.theme.BorderSpruce
import com.mediadock.app.core.designsystem.theme.DarkElevatedSpruce
import com.mediadock.app.core.designsystem.theme.DarkSurfaceSpruce
import com.mediadock.app.core.designsystem.theme.ObsidianCanvas
import com.mediadock.app.core.designsystem.theme.SlimeLime
import com.mediadock.app.core.designsystem.theme.TextPrimary
import com.mediadock.app.core.designsystem.theme.TextSecondary
import org.json.JSONObject

@Composable
fun MediaDockEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
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

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(R.drawable.media_dock_hero),
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Spacer(Modifier.height(20.dp))

            // Status Icon Pill
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(DarkElevatedSpruce)
                    .border(1.dp, BorderSpruce, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = SlimeLime
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Card container for message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurfaceSpruce)
                    .border(1.dp, BorderSpruce, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cleanMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!actionLabel.isNullOrBlank()) {
                    Button(
                        onClick = onActionClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SlimeLime,
                            contentColor = ObsidianCanvas
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!secondaryActionLabel.isNullOrBlank() && onSecondaryActionClick != null) {
                    OutlinedButton(
                        onClick = onSecondaryActionClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = secondaryActionLabel,
                            color = SlimeLime,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
