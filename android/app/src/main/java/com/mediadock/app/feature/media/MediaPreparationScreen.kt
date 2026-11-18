package com.mediadock.app.feature.media

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.mediadock.app.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.*
import com.mediadock.app.feature.browser.BrowserRules
import kotlinx.coroutines.delay

@Composable fun MediaPreparationScreen(phase: String = "Finding your media", sourceUrl: String = "", modifier: Modifier = Modifier, progress: Float? = null) {
    var seconds by remember { mutableStateOf(0) }
    var play by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(Unit) { while (true) { delay(1000); seconds++ } }
    val tips = listOf("A great find deserves a place. Save links to revisit later.", "Create collections to keep learning, music and inspiration together.", "Your browser keeps your tabs and cookies on this device.", "Use Favorites to find your best downloads faster.")
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .size(if (play) 64.dp else 84.dp)
                .clip(CircleShape)
                .background(BrandCoralOrange.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (play) 44.dp else 56.dp),
                color = BrandCoralOrange,
                strokeWidth = 3.dp
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(phase, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = BrandTextPrimary)
        Text(BrowserRules.displayHost(sourceUrl).ifBlank { "One moment, your media is on its way" }, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium, color = BrandTextSecondary)
        Spacer(Modifier.height(18.dp))
        if (progress != null) {
            LinearProgressIndicator(progress = { (progress / 100f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(.8f), color = BrandCoralOrange, trackColor = BrandDarkBorder)
            Text("${progress.toInt()}% transferred", style = MaterialTheme.typography.labelMedium, color = BrandTextPrimary)
        } else LinearProgressIndicator(Modifier.fillMaxWidth(.65f), color = BrandCoralOrange, trackColor = BrandDarkBorder)
        Text("${seconds}s elapsed", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelMedium, color = BrandTextSecondary)
        if (seconds >= 15) Text("Some websites or a sleeping cloud server need a little longer. You can go back at any time.", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = BrandTextMuted)
        Spacer(Modifier.height(24.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = BrandDarkSurface, border = androidx.compose.foundation.BorderStroke(1.dp, BrandDarkBorder)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (play) "Snake · your waiting companion" else "A little inspiration while you wait", style = MaterialTheme.typography.titleSmall, color = BrandTextPrimary)
                if (play) SnakeGame(Modifier.padding(top = 12.dp))
                else AnimatedContent((seconds / 8) % tips.size, label = "waitingTip") { index -> Text(tips[index], Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = BrandTextSecondary) }
                TextButton(onClick = { play = !play }) { Text(if (play) "Show tips" else "Play while waiting", color = BrandCoralOrange) }
            }
        }
    }
}
