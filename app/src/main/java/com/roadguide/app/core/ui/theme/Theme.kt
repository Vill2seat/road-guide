package com.roadguide.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Road Guide light theme built from Figma design tokens.
 *
 * Applies colors, typography, and shapes derived from:
 *   - design/figma-export/styles/colors.json
 *   - design/figma-export/styles/typography.json
 *   - design/figma-export/styles/spacing.json
 *
 * Usage:
 *   RoadGuideTheme { ... }
 */
@Composable
fun RoadGuideTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = RgColor.Primary,
        primaryContainer = RgColor.PrimaryContainer,
        secondary = RgColor.Secondary,
        secondaryContainer = RgColor.SecondaryContainer,
        background = RgColor.Background,
        surface = RgColor.Surface,
        surfaceVariant = RgColor.SurfaceVariant,
        onPrimary = Color.Black,
        onSecondary = Color.Black,
        onBackground = RgColor.OnBackground,
        onSurface = RgColor.OnSurface,
        onSurfaceVariant = RgColor.OnSurfaceVariant,
        outline = RgColor.Outline,
        error = RgColor.Error,
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RoadGuideTypography(),
        shapes = RoadGuideShapes(),
        content = content
    )
}

/**
 * Returns a Typography instance configured from Figma design tokens.
 */
fun RoadGuideTypography(): Typography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
)

/**
 * Returns a Shapes instance configured from Figma design tokens.
 */
fun RoadGuideShapes(): Shapes = Shapes(
    small = RoundedCornerShape(RgDimens.RadiusSmall),
    medium = RoundedCornerShape(RgDimens.RadiusMedium),
    large = RoundedCornerShape(RgDimens.RadiusLarge),
    extraLarge = RoundedCornerShape(RgDimens.RadiusLarge),
)