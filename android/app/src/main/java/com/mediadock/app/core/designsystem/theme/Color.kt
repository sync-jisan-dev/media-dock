package com.mediadock.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// Cloudhub Dark Mode & Premium Vibrant Coral Orange Palette
val BrandDarkCanvas = Color(0xFF111111)           // Main deep dark background (#111111)
val BrandDarkSurface = Color(0xFF1A1A1A)          // Card/dialog surface (#1A1A1A)
val BrandDarkRaised = Color(0xFF222222)           // Elevated surface / containers (#222222)
val BrandDarkBorder = Color(0xFF2E2E2E)           // Border and dividers (#2E2E2E)

val BrandCoralOrange = Color(0xFFFE5223)          // Signature premium CTA button (#FE5223)
val BrandCoralOrangeDark = Color(0xFFE0451B)      // Pressed / darker orange
val BrandCoralOrangeLight = Color(0xFFFF6B42)     // Lighter / hover orange

val BrandTextPrimary = Color(0xFFFFFFFF)          // High-contrast primary text
val BrandTextSecondary = Color(0xFFA1A1AA)        // Muted secondary text
val BrandTextMuted = Color(0xFF71717A)            // Tertiary muted caption
val BrandWhite = Color(0xFFFFFFFF)
val BrandBlack = Color(0xFF000000)

// Legacy aliases mapping to Cloudhub dark palette for complete system backwards compatibility
val BrandSageMint = BrandDarkCanvas
val BrandMintTint = BrandDarkRaised
val BrandForestDark = BrandCoralOrange
val BrandForestMuted = BrandTextSecondary
val BrandVibrantMint = BrandCoralOrange
val BrandCosmicViolet = BrandCoralOrange
val BrandForestNight = BrandDarkCanvas
val BrandForestNightSurface = BrandDarkSurface
val BrandForestNightRaised = BrandDarkRaised
val BrandForestNightBorder = BrandDarkBorder

val BrandPink = BrandDarkSurface
val BrandLime = BrandCoralOrange
val BrandHotPink = BrandCoralOrange
val BrandPrussianBlue = BrandDarkCanvas
val BrandOrange = BrandCoralOrange
val BrandAlabaster = BrandDarkSurface

// Supporting surfaces and borders
val DockInk = BrandWhite
val DockMuted = BrandTextSecondary
val DockPaper = BrandDarkCanvas
val DockPaperSurface = BrandDarkSurface
val DockPaperRaised = BrandDarkRaised
val DockPaperBorder = BrandDarkBorder
val DockNight = BrandDarkCanvas
val DockNightSurface = BrandDarkSurface
val DockNightRaised = BrandDarkRaised
val DockNightBorder = BrandDarkBorder
val HairlineSubtle = BrandDarkBorder

// Semantic aliases
val ObsidianCanvas = DockNight
val DarkSurfaceSpruce = DockNightSurface
val DarkElevatedSpruce = DockNightRaised
val DarkCardSpruce = DockNightRaised
val DeepBlackSpruce = DockNight
val BorderSpruce = DockNightBorder
val TextPrimary = BrandTextPrimary
val TextSecondary = BrandTextSecondary
val TextTertiary = BrandTextMuted
val DarkSpruce = BrandDarkSurface
val ForestGreen = BrandCoralOrange
val ForestGreen2 = BrandTextSecondary
val ForestGreen3 = BrandTextMuted
val BrightFern = BrandCoralOrange
val RadioactiveGrass = BrandCoralOrange
val SlimeLime = BrandCoralOrange
val Chartreuse = BrandDarkRaised
val DockAqua = BrandCoralOrange
val DockIndigo = BrandDarkCanvas
val DockIndigoDark = BrandDarkSurface
val DockSky = BrandCoralOrange
val DockLavender = BrandCoralOrange

// Material 3 Color Schemes - Unified on Cloudhub Dark Aesthetic
val md_theme_light_primary = BrandCoralOrange
val md_theme_light_onPrimary = BrandWhite
val md_theme_light_primaryContainer = BrandDarkRaised
val md_theme_light_onPrimaryContainer = BrandCoralOrange
val md_theme_light_secondary = BrandCoralOrange
val md_theme_light_onSecondary = BrandWhite
val md_theme_light_secondaryContainer = BrandDarkSurface
val md_theme_light_onSecondaryContainer = BrandTextPrimary
val md_theme_light_tertiary = BrandCoralOrangeLight
val md_theme_light_onTertiary = BrandWhite
val md_theme_light_tertiaryContainer = BrandDarkRaised
val md_theme_light_onTertiaryContainer = BrandCoralOrangeLight
val md_theme_light_error = Color(0xFFFF5252)
val md_theme_light_errorContainer = Color(0xFF3B1212)
val md_theme_light_onError = BrandWhite
val md_theme_light_onErrorContainer = Color(0xFFFFB4AB)
val md_theme_light_background = BrandDarkCanvas
val md_theme_light_onBackground = BrandTextPrimary
val md_theme_light_surface = BrandDarkSurface
val md_theme_light_onSurface = BrandTextPrimary
val md_theme_light_surfaceVariant = BrandDarkRaised
val md_theme_light_onSurfaceVariant = BrandTextSecondary
val md_theme_light_outline = BrandDarkBorder

val md_theme_dark_primary = BrandCoralOrange
val md_theme_dark_onPrimary = BrandWhite
val md_theme_dark_primaryContainer = BrandDarkRaised
val md_theme_dark_onPrimaryContainer = BrandCoralOrange
val md_theme_dark_secondary = BrandCoralOrange
val md_theme_dark_onSecondary = BrandWhite
val md_theme_dark_secondaryContainer = BrandDarkSurface
val md_theme_dark_onSecondaryContainer = BrandTextPrimary
val md_theme_dark_tertiary = BrandCoralOrangeLight
val md_theme_dark_onTertiary = BrandWhite
val md_theme_dark_tertiaryContainer = BrandDarkRaised
val md_theme_dark_onTertiaryContainer = BrandCoralOrangeLight
val md_theme_dark_error = Color(0xFFFF5252)
val md_theme_dark_errorContainer = Color(0xFF3B1212)
val md_theme_dark_onError = BrandWhite
val md_theme_dark_onErrorContainer = Color(0xFFFFB4AB)
val md_theme_dark_background = BrandDarkCanvas
val md_theme_dark_onBackground = BrandTextPrimary
val md_theme_dark_surface = BrandDarkSurface
val md_theme_dark_onSurface = BrandTextPrimary
val md_theme_dark_surfaceVariant = BrandDarkRaised
val md_theme_dark_onSurfaceVariant = BrandTextSecondary
val md_theme_dark_outline = BrandDarkBorder
