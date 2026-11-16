package com.mediadock.app.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.designsystem.icons.HugeIcons

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
        val cellWidth = (maxWidth / labels.size).coerceAtLeast(60.dp)
        val indicatorOffset by animateDpAsState(
            cellWidth * selectedIndex,
            tween(260, easing = CubicBezierEasing(.22f, 1f, .36f, 1f)),
            label = "selectionPosition"
        )
        Box(Modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).horizontalScroll(rememberScrollState())) {
            Box(Modifier.width(cellWidth * labels.size).height(50.dp)) {
                Box(Modifier.offset(x = indicatorOffset).width(cellWidth).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primary))
                Row(Modifier.selectableGroup()) {
                    labels.forEachIndexed { index, label ->
                        val ink by animateColorAsState(
                            if (index == selectedIndex) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            tween(160), label = "selectionInk"
                        )
                        Box(
                            Modifier.width(cellWidth).height(50.dp).selectable(selected = index == selectedIndex, role = Role.Tab, onClick = { onSelected(index) }),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = ink, textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MediaDockDownloadAction(label: String, busy: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = !busy,
        modifier = modifier.fillMaxWidth().height(54.dp).semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.primary, disabledContentColor = MaterialTheme.colorScheme.onPrimary)
    ) {
        AnimatedContent(busy, label = "downloadAction") { pending ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pending) CircularProgressIndicator(Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else Icon(HugeIcons.Download, null, Modifier.size(22.dp))
                Text(if (pending) "Adding to downloads…" else label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun MediaDockTransferStatus(label: String, progress: Float, completed: Boolean, modifier: Modifier = Modifier) {
    val fill by animateFloatAsState(progress.coerceIn(0f, 1f), tween(350), label = "transferFill")
    Box(
        modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { stateDescription = "$label, ${(fill * 100).toInt()} percent" },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth(fill).fillMaxHeight().background(MaterialTheme.colorScheme.onSurface.copy(alpha = .09f)))
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (completed) HugeIcons.CheckCircle else HugeIcons.Download, null, Modifier.size(20.dp))
            AnimatedContent(label, label = "transferStatus") { Text(it, style = MaterialTheme.typography.labelMedium) }
            Spacer(Modifier.weight(1f))
            Text("${(fill * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
        }
    }
}
