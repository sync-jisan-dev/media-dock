package com.mediadock.app.core.designsystem.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
fun MediaDockTransferStatus(label: String, progress: Float, completed: Boolean, modifier: Modifier = Modifier) {
    val fill by animateFloatAsState(progress.coerceIn(0f, 1f), tween(350), label = "transferFill")
    val infiniteTransition = rememberInfiniteTransition(label = "transferPulse")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    val progressGradient = if (completed) {
        Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))
    } else {
        Brush.horizontalGradient(
            listOf(
                BrandCoralOrange.copy(alpha = shimmerAlpha),
                Color(0xFFFF6E40).copy(alpha = shimmerAlpha)
            )
        )
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(CpdRadius.radiusFull))
            .background(if (CpdColor.isDark) Color(0xFF1A1A1E) else Color(0xFFF1F3F5))
            .border(
                width = CpdBorderWidth.default,
                color = if (completed) Color(0xFF10B981).copy(alpha = 0.4f) else BrandCoralOrange.copy(alpha = 0.35f),
                shape = RoundedCornerShape(CpdRadius.radiusFull)
            )
            .semantics { stateDescription = "$label, ${(fill * 100).toInt()} percent" },
        contentAlignment = Alignment.CenterStart
    ) {
        if (fill > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(fill)
                    .fillMaxHeight()
                    .background(progressGradient)
            )
        }
        Row(
            Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (completed) HugeIcons.CheckCircle else HugeIcons.Download,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (completed || fill > 0.15f) Color.White else MaterialTheme.colorScheme.onSurface
            )
            AnimatedContent(label, label = "transferStatus") {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (completed || fill > 0.35f) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${(fill * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                color = if (completed || fill > 0.85f) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
