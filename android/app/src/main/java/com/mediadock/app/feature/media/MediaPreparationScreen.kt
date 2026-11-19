package com.mediadock.app.feature.media

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.designsystem.components.BrandButton
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.BrandCoralOrange
import com.mediadock.app.feature.browser.BrowserRules
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MediaPreparationScreen(
    phase: String = "Downloading your media",
    sourceUrl: String = "",
    modifier: Modifier = Modifier,
    progress: Float? = null,
    onViewQueue: (() -> Unit)? = null
) {
    var seconds by remember { mutableStateOf(0) }
    var simulatedProgress by remember { mutableStateOf(12f) }
    var simulatedSpeed by remember { mutableStateOf(2.8f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            seconds++
            if (simulatedProgress < 92f) {
                val increment = when {
                    simulatedProgress < 35f -> 2.4f
                    simulatedProgress < 65f -> 1.7f
                    simulatedProgress < 82f -> 0.9f
                    else -> 0.4f
                }
                simulatedProgress = (simulatedProgress + increment).coerceAtMost(92f)
                val jitter = (kotlin.math.sin(seconds.toDouble() * 0.4).toFloat() * 0.6f)
                simulatedSpeed = (2.8f + jitter).coerceIn(1.8f, 3.9f)
            }
        }
    }

    val effectiveProgress = progress ?: simulatedProgress
    val effectiveSpeedStr = String.format(Locale.US, "%.1f MB/s", simulatedSpeed)
    val effectiveEta = ((100f - effectiveProgress) / 3.5f).toInt().coerceAtLeast(2)

    val tips = listOf(
        "A great find deserves a place. Save links to revisit later.",
        "Create collections to keep learning, music and inspiration together.",
        "Your browser keeps your tabs and cookies securely on this device.",
        "Use Favorites to find your best downloads faster."
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(BrandCoralOrange.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(52.dp),
                color = BrandCoralOrange,
                strokeWidth = 3.5.dp
            )
            Icon(
                imageVector = HugeIcons.Download,
                contentDescription = null,
                tint = BrandCoralOrange,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = phase,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = BrowserRules.displayHost(sourceUrl).ifBlank { "One moment, your media is on its way" },
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        // Determinate active progress bar that always progresses like a live download
        LinearProgressIndicator(
            progress = { (effectiveProgress / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = BrandCoralOrange,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Row(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${effectiveProgress.toInt()}% downloaded",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$effectiveSpeedStr • ${effectiveEta}s left",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "Receiving media packets from server",
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (onViewQueue != null) {
            Spacer(Modifier.height(18.dp))
            BrandButton(
                onClick = onViewQueue,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(44.dp)
            ) {
                Icon(HugeIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View in Queue", fontWeight = FontWeight.Normal)
            }
        }

        Spacer(Modifier.height(28.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        HugeIcons.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BrandCoralOrange
                    )
                    Text(
                        "While you wait",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                AnimatedContent(
                    targetState = (seconds / 8) % tips.size,
                    label = "waitingTip"
                ) { index ->
                    Text(
                        tips[index],
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
