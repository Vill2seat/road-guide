package com.roadguide.app.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing, radius, and touch target tokens.
 * Derived from design/figma-export/styles/spacing.json + SVG screen measurements.
 */
object RgDimens {
    // Spacing
    val Space0 = 0.dp
    val Space1 = 4.dp
    val Space2 = 8.dp
    val Space3 = 12.dp
    val Space4 = 16.dp
    val Space5 = 20.dp
    val Space6 = 24.dp
    val Space8 = 32.dp
    val Space10 = 40.dp
    val Space12 = 48.dp
    val Space16 = 64.dp

    // Border radius
    val RadiusSmall = 8.dp
    val RadiusMedium = 12.dp
    val RadiusLarge = 16.dp
    val RadiusCard = 18.dp       // White cards in SVGs use rx=18
    val RadiusSheet = 28.dp      // Bottom sheet in SVGs uses rx=28
    val RadiusFull = 999.dp

    // Icon sizes
    val IconSmall = 16.dp
    val IconMedium = 24.dp
    val IconLarge = 32.dp
    val IconXLarge = 48.dp

    // Touch targets
    val TouchTargetMinimum = 48.dp
    val TouchTargetComfortable = 56.dp

    // Map control buttons (44×44 in SVGs)
    val MapControlSize = 44.dp
    val MapControlRadius = 18.dp

    // Drag handle
    val DragHandleWidth = 44.dp
    val DragHandleHeight = 4.dp

    // Location pill
    val LocationPillHeight = 28.dp
    val LocationPillRadius = 14.dp

    // Elevation (per shadows.json compose mapping)
    val Elevation0 = 0.dp
    val Elevation1 = 1.dp
    val Elevation2 = 3.dp
    val Elevation3 = 6.dp
    val Elevation4 = 12.dp
}
