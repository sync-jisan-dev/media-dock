package com.mediadock.app.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// MediaDock Stageshow Master Palette — Deep Obsidian Spruce & Electric Lime
// Designed with quiet dark surfaces, purposeful hierarchy, and laser accent.
// ============================================================================

// Brand Core Colors (Requested by User)
val DarkSpruce = Color(0xFF004B23)
val ForestGreen = Color(0xFF006400)
val ForestGreen2 = Color(0xFF007200)
val ForestGreen3 = Color(0xFF008000)
val BrightFern = Color(0xFF38B000)
val RadioactiveGrass = Color(0xFF70E000)
val SlimeLime = Color(0xFF9EF01A)
val Chartreuse = Color(0xFFCCFF33)

// Luxury Obsidian Surface Hierarchy (Stageshow Craft Standard)
val ObsidianCanvas = Color(0xFF060F0A)          // Level 0: Pure Deep Canvas
val DarkSurfaceSpruce = Color(0xFF0D1B13)        // Level 1: Primary Cards & Containers
val DarkElevatedSpruce = Color(0xFF14271D)       // Level 2: Interactive Controls & Nested Cards
val DarkCardSpruce = Color(0xFF1B3125)           // Level 3: Active Pills & Hover Surfaces
val DeepBlackSpruce = Color(0xFF040A07)

// Hairline Borders & Dividers
val BorderSpruce = Color(0x2470E000)             // 14% Radioactive Grass for crisp borders
val HairlineSubtle = Color(0x14FFFFFF)           // 8% White for quiet dividers

// Text & Content Hierarchy
val TextPrimary = Color(0xFFF2FBF5)              // Crisp High-Legibility White
val TextSecondary = Color(0xFFA1BCAD)            // Muted Sage Gray for metadata & subtitles
val TextTertiary = Color(0xFF6B8A77)             // Quiet Muted for timestamps & inactive labels

// Brand and Legacy Navigation Aliases
val DockAqua = SlimeLime
val DockIndigo = RadioactiveGrass
val DockIndigoDark = ForestGreen
val DockInk = ObsidianCanvas
val DockSky = Chartreuse
val DockLavender = SlimeLime
val DockNight = ObsidianCanvas
val DockNightSurface = DarkSurfaceSpruce
val DockNightRaised = DarkElevatedSpruce
val DockNightBorder = BorderSpruce
val DockPaper = Color(0xFFF6FBF7)
val DockPaperSurface = Color.White
val DockPaperRaised = Color(0xFFE6EFE8)
val DockPaperBorder = Color(0xFFB5C4B9)
val DockMuted = TextSecondary

// Material 3 Dark Color Scheme Mappings
val md_theme_dark_primary = SlimeLime
val md_theme_dark_onPrimary = Color(0xFF051B07)
val md_theme_dark_primaryContainer = Color(0xFF0C2B14)
val md_theme_dark_onPrimaryContainer = Chartreuse
val md_theme_dark_secondary = RadioactiveGrass
val md_theme_dark_onSecondary = Color(0xFF051B07)
val md_theme_dark_secondaryContainer = Color(0xFF0E2E18)
val md_theme_dark_onSecondaryContainer = Color(0xFFD6FFB2)
val md_theme_dark_tertiary = Chartreuse
val md_theme_dark_onTertiary = Color(0xFF071F08)
val md_theme_dark_tertiaryContainer = Color(0xFF17381C)
val md_theme_dark_onTertiaryContainer = Color(0xFFE4FFB8)
val md_theme_dark_error = Color(0xFFFF5252)
val md_theme_dark_errorContainer = Color(0xFF450A0A)
val md_theme_dark_onError = Color.White
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = ObsidianCanvas
val md_theme_dark_onBackground = TextPrimary
val md_theme_dark_surface = DarkSurfaceSpruce
val md_theme_dark_onSurface = TextPrimary
val md_theme_dark_surfaceVariant = DarkElevatedSpruce
val md_theme_dark_onSurfaceVariant = TextSecondary
val md_theme_dark_outline = BorderSpruce
val md_theme_dark_outlineVariant = HairlineSubtle

// Material 3 Light Fallback Mappings
val md_theme_light_primary = ForestGreen
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFD4F7D4)
val md_theme_light_onPrimaryContainer = Color(0xFF002206)
val md_theme_light_secondary = BrightFern
val md_theme_light_onSecondary = Color.White
val md_theme_light_secondaryContainer = Color(0xFFE2F9DE)
val md_theme_light_onSecondaryContainer = Color(0xFF0B2B0C)
val md_theme_light_tertiary = ForestGreen2
val md_theme_light_onTertiary = Color.White
val md_theme_light_tertiaryContainer = Color(0xFFCEF5CB)
val md_theme_light_onTertiaryContainer = Color(0xFF022106)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color.White
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFF6FBF7)
val md_theme_light_onBackground = Color(0xFF0E1A11)
val md_theme_light_surface = Color.White
val md_theme_light_onSurface = Color(0xFF0E1A11)
val md_theme_light_surfaceVariant = Color(0xFFE6EFE8)
val md_theme_light_onSurfaceVariant = Color(0xFF425046)
val md_theme_light_outline = Color(0xFFB5C4B9)
