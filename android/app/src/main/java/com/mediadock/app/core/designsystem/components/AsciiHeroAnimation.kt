package com.mediadock.app.core.designsystem.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.designsystem.theme.CpdColor
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Procedural ASCII Gradient Animation inspired by WebGL shader field.
 *
 * Renders an animated matrix of ASCII glyphs (' =+xX0S8#@') with dynamic
 * wave flow, organic breathing shapes, brand coral-orange gradient tones,
 * and interactive touch scramble physics.
 *
 * Adapts dynamically to Dark Mode and Light Mode.
 */
private const val GLYPH_CHARS = " =+xX0S8#@"

private data class TouchRipple(
    val x: Float,
    val y: Float,
    var age: Float = 0f
)

@Composable
fun AsciiHeroAnimation(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(130.dp),
    isDark: Boolean = CpdColor.isDark,
    interactive: Boolean = true,
    opacity: Float = 0.16f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "asciiClock")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f, // 2 * PI
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "asciiTime"
    )

    // Touch ripples list for interactive scrambles
    val ripples = remember { mutableStateListOf<TouchRipple>() }

    // Color definitions
    val coralBright = CpdColor.CoralHover
    val coralDeep = CpdColor.CoralPressed
    val coralRest = CpdColor.CoralRest
    val textMuted = if (isDark) CpdColor.Gray700 else CpdColor.Gray800

    val textPaint = remember(isDark) {
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.MONOSPACE
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
    }

    Box(
        modifier = modifier
            .pointerInput(interactive) {
                if (!interactive) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        ripples.add(TouchRipple(offset.x, offset.y))
                        if (ripples.size > 6) ripples.removeAt(0)
                    }
                )
            }
            .pointerInput(interactive) {
                if (!interactive) return@pointerInput
                detectDragGestures { change, _ ->
                    val pos = change.position
                    ripples.add(TouchRipple(pos.x, pos.y))
                    if (ripples.size > 8) ripples.removeAt(0)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            // Update ripple ages
            val iterator = ripples.iterator()
            while (iterator.hasNext()) {
                val r = iterator.next()
                r.age += 0.05f
                if (r.age > 1.2f) iterator.remove()
            }

            val cellWidth = 24f
            val cellHeight = 32f
            val cols = (width / cellWidth).toInt() + 1
            val rows = (height / cellHeight).toInt() + 1

            val aspect = width / height

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas

                for (row in 0 until rows) {
                    val y = row * cellHeight + cellHeight * 0.7f
                    val yNorm = (row.toFloat() / rows.toFloat()) * 2f - 1f

                    for (col in 0 until cols) {
                        val x = col * cellWidth + cellWidth * 0.5f
                        val xNorm = ((col.toFloat() / cols.toFloat()) * 2f - 1f) * aspect

                        // Wave synthesis matching shader field
                        val wave1 = sin(xNorm * 1.8f + time * 1.2f) * 0.25f
                        val wave2 = cos(yNorm * 2.2f - time * 0.9f) * 0.22f
                        val arch = -0.15f - 0.25f * xNorm * xNorm + 0.05f * sin(time * 0.6f)
                        val edge = yNorm - arch
                        val rim = (0.5f + wave1 + wave2 - edge * 0.6f).coerceIn(0f, 1f)

                        // Interactive touch scrambler
                        var scrambleBonus = 0f
                        for (r in ripples) {
                            val dist = hypot(x - r.x, y - r.y)
                            val k = (1f - r.age / 1.2f).coerceAtLeast(0f)
                            val factor = exp(-dist * 0.025f) * k
                            scrambleBonus = maxOf(scrambleBonus, factor)
                        }

                        val combinedIntensity = (rim + scrambleBonus * 0.85f).coerceIn(0f, 1f)
                        val charIdx = ((combinedIntensity * (GLYPH_CHARS.length - 1)) + 0.5f).toInt()
                            .coerceIn(0, GLYPH_CHARS.length - 1)
                        val glyph = GLYPH_CHARS[charIdx]

                        if (glyph != ' ') {
                            // Compute edge fade all around (vignette so glyphs smoothly dissolve into canvas)
                            val edgeDistX = minOf(col.toFloat(), (cols - 1 - col).toFloat()) / (cols * 0.22f).coerceAtLeast(1f)
                            val edgeDistY = minOf(row.toFloat(), (rows - 1 - row).toFloat()) / (rows * 0.25f).coerceAtLeast(1f)
                            val edgeFade = (edgeDistX.coerceIn(0f, 1f) * edgeDistY.coerceIn(0f, 1f))

                            // Subtle airy opacity requested by user
                            val maxOpacity = opacity.coerceIn(0f, 1f)
                            val baseAlpha = (maxOpacity * 0.22f + combinedIntensity * maxOpacity * 0.78f) * edgeFade
                            val scrambleAlpha = (maxOpacity * 0.72f + scrambleBonus * maxOpacity * 0.28f) * edgeFade
                            val alpha = if (scrambleBonus > 0.1f) scrambleAlpha else baseAlpha

                            val resolvedColor = when {
                                scrambleBonus > 0.3f -> coralBright
                                combinedIntensity > 0.65f -> coralRest
                                combinedIntensity > 0.35f -> coralDeep
                                else -> textMuted
                            }

                            textPaint.color = resolvedColor.copy(alpha = alpha).toArgb()
                            native.drawText(glyph.toString(), x, y, textPaint)
                        }
                    }
                }
            }
        }
    }
}
