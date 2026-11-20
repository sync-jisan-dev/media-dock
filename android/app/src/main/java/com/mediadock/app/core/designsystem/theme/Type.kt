package com.mediadock.app.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import com.mediadock.app.R
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold)
)

/**
 * Strict Typography Scale from Compound / ElementX Design System Tokens.
 *
 * Allowed sizes: 11, 13, 15, 17 (body) and 20, 24, 28, 32 (headings).
 * Line heights: 1.5 for body, 1.25 for headings.
 * Allowed weights: 400 (Normal), 500 (Medium), 600 (SemiBold).
 * Optical tracking: em scale per token spec.
 */
val Typography = Typography(
    displayLarge = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.0216).em),
    displayMedium = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 35.sp, letterSpacing = (-0.0209).em),
    displaySmall = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.0195).em),
    headlineLarge = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 35.sp, letterSpacing = (-0.0209).em),
    headlineMedium = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.0195).em),
    headlineSmall = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = (-0.0166).em),
    titleLarge = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = (-0.0166).em),
    titleMedium = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 25.5.sp, letterSpacing = (-0.0128).em),
    titleSmall = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.5.sp, letterSpacing = (-0.0088).em),
    bodyLarge = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 25.5.sp, letterSpacing = (-0.0128).em),
    bodyMedium = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.5.sp, letterSpacing = (-0.0088).em),
    bodySmall = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.5.sp, letterSpacing = (-0.0031).em),
    labelLarge = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.5.sp, letterSpacing = (-0.0088).em),
    labelMedium = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 19.5.sp, letterSpacing = (-0.0031).em),
    labelSmall = TextStyle(fontFamily = PoppinsFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.5.sp, letterSpacing = 0.0048.em)
)
