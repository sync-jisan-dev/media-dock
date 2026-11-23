package com.mediadock.app.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.designsystem.icons.HugeIcons
import com.mediadock.app.core.designsystem.theme.*

// Native adaptations of Motion Brew's pill-tabs and download-button interactions.
// Compose respects Android's animator duration scale, including reduced motion.
@Composable
fun MediaDockPillTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cellWidth = (maxWidth / labels.size).coerceAtLeast(76.dp)
        val indicatorOffset by animateDpAsState(
            cellWidth * selectedIndex,
            tween(260, easing = CubicBezierEasing(.22f, 1f, .36f, 1f)),
            label = "selectionPosition"
        )
        Box(Modifier.clip(RoundedCornerShape(CpdRadius.radiusFull)).background(MaterialTheme.colorScheme.surfaceVariant).horizontalScroll(rememberScrollState())) {
            Box(Modifier.width(cellWidth * labels.size).height(48.dp)) {
                Box(Modifier.offset(x = indicatorOffset).width(cellWidth).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(CpdRadius.radiusFull)).background(MaterialTheme.colorScheme.primary))
                Row(Modifier.selectableGroup()) {
                    labels.forEachIndexed { index, label ->
                        val ink by animateColorAsState(
                            if (index == selectedIndex) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            tween(160), label = "selectionInk"
                        )
                        Box(
                            Modifier.width(cellWidth).height(48.dp).selectable(selected = index == selectedIndex, role = Role.Tab, onClick = { onSelected(index) }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = ink, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaDockDownloadAction(label: String, busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BrandButton(
        onClick = onClick,
        enabled = !busy,
        modifier = modifier.fillMaxWidth().height(CpdSize.buttonLarge).semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(CpdRadius.radiusFull),
        colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.primary, disabledContentColor = MaterialTheme.colorScheme.onPrimary)
    ) {
        AnimatedContent(busy, label = "downloadAction") { pending ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CpdSpace.space2x)) {
                if (pending) CircularProgressIndicator(Modifier.size(CpdSize.iconSm), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = CpdBorderWidth.focused)
                else Icon(HugeIcons.Download, null, Modifier.size(CpdSize.iconMd))
                Text(if (pending) "Adding to downloads…" else label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun MediaDockTransferStatus(
    label: String,
    progress: Float? = null,
    modifier: Modifier = Modifier,
    paused: Boolean = false
) {
    val measuredProgress = progress?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val bar = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            if (measuredProgress != null) {
                Text(
                    text = "${(measuredProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (measuredProgress != null) {
            LinearProgressIndicator(
                progress = { measuredProgress },
                modifier = bar,
                color = if (paused) MaterialTheme.colorScheme.onSurfaceVariant else BrandCoralOrange,
                trackColor = trackColor
            )
        } else if (!paused) {
            LinearProgressIndicator(modifier = bar, color = BrandCoralOrange, trackColor = trackColor)
        }
    }
}
