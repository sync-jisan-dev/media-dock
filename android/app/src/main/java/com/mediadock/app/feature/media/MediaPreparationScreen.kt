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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.BrandCoralOrange
import com.mediadock.app.feature.browser.BrowserRules
import kotlinx.coroutines.delay

@Composable
fun MediaPreparationScreen(
    phase: String = "Finding your media",
    sourceUrl: String = "",
    modifier: Modifier = Modifier,
    progress: Float? = null
) {
    var seconds by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
        }
    }
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
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = phase,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = BrowserRules.displayHost(sourceUrl).ifBlank { "One moment, your media is on its way" },
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        if (progress != null) {
            LinearProgressIndicator(
                progress = { (progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(0.85f),
                color = BrandCoralOrange,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                "${progress.toInt()}% transferred",
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(0.7f),
                color = BrandCoralOrange,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
        Text(
            "${seconds}s elapsed",
            modifier = Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (seconds >= 15) {
            Text(
                "Some websites or a sleeping cloud server need a little longer. You can go back at any time.",
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
