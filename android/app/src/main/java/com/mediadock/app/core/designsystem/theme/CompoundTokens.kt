package com.mediadock.app.core.designsystem.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compound / ElementX Design System Tokens for MediaDock.
 *
 * Implements the 2-layer token architecture:
 * Layer 1: Primitives (Base color ramps & scales)
 * Layer 2: Semantic tokens (Intent & usage)
 * Preserves the signature MediaDock dark canvas (#111111) and coral-orange (#FE5223).
 */
object CpdColor {
    // --- Primitive Color Foundations ---
    val DarkCanvas = Color(0xFF111111)
    val Gray100 = Color(0xFF14171B)
    val Gray200 = Color(0xFF181A1F)
    val Gray300 = Color(0xFF1D1F24)
    val Gray400 = Color(0xFF26282D)
    val Gray500 = Color(0xFF323539)
    val Gray700 = Color(0xFF4A4F55)
    val Gray800 = Color(0xFF656C76)
    val Gray900 = Color(0xFF808994)
    val White = Color(0xFFFFFFFF)

    // Vibrant Coral-Orange Action Ramp (Compound Action Primary)
    val CoralRest = Color(0xFFFE5223)
    val CoralHover = Color(0xFFFF663D)
    val CoralPressed = Color(0xFFE0451B)
    val CoralDisabled = Color(0xFF323539)
    val CoralLightGradientStart = Color(0xFFFE5827)
    val CoralLightGradientEnd = Color(0xFFFC4724)

    // --- Semantic Background Tokens ---
    val bgCanvasDefault = DarkCanvas
    val bgSubtlePrimary = Gray400       // Level 3 elevated
    val bgSubtleSecondary = Gray300     // Level 2 cards & inputs
    val bgSubtleTertiary = Gray200      // Level 1 navbars & containers

    // --- Semantic Text Tokens ---
    val textPrimary = White
    val textSecondary = Gray900
    val textDisabled = Gray800
    val textOnSolidPrimary = White
    val textActionAccent = CoralRest

    // --- Semantic Action Tokens ---
    val bgActionPrimaryRest = CoralRest
    val bgActionPrimaryHovered = CoralHover
    val bgActionPrimaryPressed = CoralPressed
    val bgActionPrimaryDisabled = CoralDisabled
    val bgActionSecondaryRest = Gray300
    val bgActionSecondaryHovered = Gray400

    // --- Semantic Border Tokens ---
    val borderInteractivePrimary = Gray800
    val borderInteractiveSecondary = Gray400
    val borderFocused = CoralRest
    val borderDisabled = Gray500

    // --- Premium Gradient ---
    val actionPrimaryGradient = Brush.horizontalGradient(
        listOf(CoralLightGradientStart, CoralLightGradientEnd)
    )
}

/**
 * Strict 4px Spacing Tokens from Compound Design System.
 * Token Nx = N * 4px.
 */
object CpdSpace {
    val space0x = 0.dp
    val space05x = 2.dp
    val space1x = 4.dp
    val space15x = 6.dp
    val space2x = 8.dp
    val space3x = 12.dp
    val space4x = 16.dp
    val space5x = 20.dp
    val space6x = 24.dp
    val space7x = 28.dp
    val space8x = 32.dp
    val space9x = 36.dp
    val space10x = 40.dp
    val space11x = 44.dp // Standard minimum accessible touch target
    val space12x = 48.dp // Standard action touch target
    val space13x = 52.dp // Primary CTA height
    val space14x = 56.dp // Standard search input height
}

/**
 * Corner Radius Tokens (Compound Extensions).
 */
object CpdRadius {
    val radiusNone = 0.dp
    val radiusSm = 4.dp
    val radiusMd = 8.dp
    val radiusLg = 12.dp
    val radiusXl = 16.dp
    val radiusFull = 9999.dp
}

/**
 * Border Width Tokens.
 */
object CpdBorderWidth {
    val hairline = 0.5.dp
    val default = 1.dp
    val focused = 2.dp
}
