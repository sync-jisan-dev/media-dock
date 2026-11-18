package com.mediadock.app.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mediadock.app.core.designsystem.theme.*

/**
 * Compound Tier 1 Action: The Premium CTA Button.
 *
 * Implements Compound Token rules:
 * - Height: 52dp (space-13x)
 * - Corner Radius: 16dp (radius-xl)
 * - Gradient: Coral-Orange #FE5827 -> #FC4724
 * - Tactile Micro-Interaction: 0.98 scale spring on touch
 * - Specular Depth: 0.5dp top-highlight rim
 */
@Composable
fun BrandButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(CpdRadius.radiusXl),
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = CpdColor.bgActionPrimaryRest,
        contentColor = CpdColor.textOnSolidPrimary,
        disabledContainerColor = CpdColor.bgActionPrimaryDisabled,
        disabledContentColor = CpdColor.textDisabled
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    border: BorderStroke? = BorderStroke(CpdBorderWidth.hairline, Color(0x33FFFFFF)),
    contentPadding: PaddingValues = PaddingValues(horizontal = CpdSpace.space6x, vertical = CpdSpace.space3x),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "btnScale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .defaultMinSize(minHeight = CpdSpace.space13x),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = if (enabled) border else null,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Compound Tier 2 Action: Outlined Secondary Button.
 * Height: 44dp (space-11x) or 48dp (space-12x) touch target.
 */
@Composable
fun CompoundSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(CpdRadius.radiusLg),
    contentPadding: PaddingValues = PaddingValues(horizontal = CpdSpace.space4x, vertical = CpdSpace.space2x),
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = CpdSpace.space11x),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CpdColor.bgSubtleSecondary,
            contentColor = CpdColor.textPrimary,
            disabledContainerColor = CpdColor.bgSubtleTertiary,
            disabledContentColor = CpdColor.textDisabled
        ),
        border = BorderStroke(CpdBorderWidth.default, CpdColor.borderInteractiveSecondary),
        contentPadding = contentPadding,
        content = content
    )
}
