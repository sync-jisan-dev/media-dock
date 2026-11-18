package com.mediadock.app.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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
 * Supports dynamic switching between Dark, Light, and System themes.
 */
data class CompoundColorTokens(
    val isDark: Boolean,
    val bgCanvasDefault: Color,
    val bgSubtlePrimary: Color,
    val bgSubtleSecondary: Color,
    val bgSubtleTertiary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val textOnSolidPrimary: Color,
    val textActionAccent: Color,
    val bgActionPrimaryRest: Color,
    val bgActionPrimaryHovered: Color,
    val bgActionPrimaryPressed: Color,
    val bgActionPrimaryDisabled: Color,
    val bgActionSecondaryRest: Color,
    val bgActionSecondaryHovered: Color,
    val borderInteractivePrimary: Color,
    val borderInteractiveSecondary: Color,
    val borderFocused: Color,
    val borderDisabled: Color,
    val actionPrimaryGradient: Brush
)

// --- Dark Mode Compound Tokens (Signature Cloudhub Palette) ---
val DarkCompoundColorTokens = CompoundColorTokens(
    isDark = true,
    bgCanvasDefault = Color(0xFF111111),
    bgSubtleTertiary = Color(0xFF181A1F),
    bgSubtleSecondary = Color(0xFF1D1F24),
    bgSubtlePrimary = Color(0xFF26282D),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF808994),
    textDisabled = Color(0xFF656C76),
    textOnSolidPrimary = Color(0xFFFFFFFF),
    textActionAccent = Color(0xFFFE5223),
    bgActionPrimaryRest = Color(0xFFFE5223),
    bgActionPrimaryHovered = Color(0xFFFF663D),
    bgActionPrimaryPressed = Color(0xFFE0451B),
    bgActionPrimaryDisabled = Color(0xFF323539),
    bgActionSecondaryRest = Color(0xFF1D1F24),
    bgActionSecondaryHovered = Color(0xFF26282D),
    borderInteractivePrimary = Color(0xFF656C76),
    borderInteractiveSecondary = Color(0xFF26282D),
    borderFocused = Color(0xFFFE5223),
    borderDisabled = Color(0xFF323539),
    actionPrimaryGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFE5827), Color(0xFFFC4724))
    )
)

// --- Light Mode Compound Tokens (Crisp Clean Light Aesthetic) ---
val LightCompoundColorTokens = CompoundColorTokens(
    isDark = false,
    bgCanvasDefault = Color(0xFFF8F9FA),
    bgSubtleTertiary = Color(0xFFFFFFFF),
    bgSubtleSecondary = Color(0xFFF1F3F5),
    bgSubtlePrimary = Color(0xFFE9ECEF),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF6B7280),
    textDisabled = Color(0xFF9CA3AF),
    textOnSolidPrimary = Color(0xFFFFFFFF),
    textActionAccent = Color(0xFFFE5223),
    bgActionPrimaryRest = Color(0xFFFE5223),
    bgActionPrimaryHovered = Color(0xFFFF663D),
    bgActionPrimaryPressed = Color(0xFFE0451B),
    bgActionPrimaryDisabled = Color(0xFFE5E7EB),
    bgActionSecondaryRest = Color(0xFFFFFFFF),
    bgActionSecondaryHovered = Color(0xFFF3F4F6),
    borderInteractivePrimary = Color(0xFFD1D5DB),
    borderInteractiveSecondary = Color(0xFFE5E7EB),
    borderFocused = Color(0xFFFE5223),
    borderDisabled = Color(0xFFE5E7EB),
    actionPrimaryGradient = Brush.horizontalGradient(
        listOf(Color(0xFFFE5827), Color(0xFFFC4724))
    )
)

val LocalCompoundColors = staticCompositionLocalOf { DarkCompoundColorTokens }

object CpdColor {
    // --- Primitive Color Foundations ---
    val DarkCanvas = Color(0xFF111111)
    val LightCanvas = Color(0xFFF8F9FA)
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

    // Dynamic Semantic Accessors (adapts automatically to Light / Dark / System theme)
    val current: CompoundColorTokens
        @Composable get() = LocalCompoundColors.current

    val isDark: Boolean
        @Composable get() = LocalCompoundColors.current.isDark

    val bgCanvasDefault: Color
        @Composable get() = LocalCompoundColors.current.bgCanvasDefault

    val bgSubtlePrimary: Color
        @Composable get() = LocalCompoundColors.current.bgSubtlePrimary

    val bgSubtleSecondary: Color
        @Composable get() = LocalCompoundColors.current.bgSubtleSecondary

    val bgSubtleTertiary: Color
        @Composable get() = LocalCompoundColors.current.bgSubtleTertiary

    val textPrimary: Color
        @Composable get() = LocalCompoundColors.current.textPrimary

    val textSecondary: Color
        @Composable get() = LocalCompoundColors.current.textSecondary

    val textDisabled: Color
        @Composable get() = LocalCompoundColors.current.textDisabled

    val textOnSolidPrimary: Color
        @Composable get() = LocalCompoundColors.current.textOnSolidPrimary

    val textActionAccent: Color
        @Composable get() = LocalCompoundColors.current.textActionAccent

    val bgActionPrimaryRest: Color
        @Composable get() = LocalCompoundColors.current.bgActionPrimaryRest

    val bgActionPrimaryHovered: Color
        @Composable get() = LocalCompoundColors.current.bgActionPrimaryHovered

    val bgActionPrimaryPressed: Color
        @Composable get() = LocalCompoundColors.current.bgActionPrimaryPressed

    val bgActionPrimaryDisabled: Color
        @Composable get() = LocalCompoundColors.current.bgActionPrimaryDisabled

    val bgActionSecondaryRest: Color
        @Composable get() = LocalCompoundColors.current.bgActionSecondaryRest

    val bgActionSecondaryHovered: Color
        @Composable get() = LocalCompoundColors.current.bgActionSecondaryHovered

    val borderInteractivePrimary: Color
        @Composable get() = LocalCompoundColors.current.borderInteractivePrimary

    val borderInteractiveSecondary: Color
        @Composable get() = LocalCompoundColors.current.borderInteractiveSecondary

    val borderFocused: Color
        @Composable get() = LocalCompoundColors.current.borderFocused

    val borderDisabled: Color
        @Composable get() = LocalCompoundColors.current.borderDisabled

    val actionPrimaryGradient: Brush
        @Composable get() = LocalCompoundColors.current.actionPrimaryGradient
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
