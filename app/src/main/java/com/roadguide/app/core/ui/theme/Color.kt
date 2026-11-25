package com.roadguide.app.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens derived from Figma screen SVGs in design/figma-export/road_guide/.
 * Light theme only (v1 — no dark mode per ADR-20241112-010).
 */
object RgColor {
    // Primary brand green (from SVG fills: #317854)
    val Primary = Color(0xFF317854)
    val PrimaryContainer = Color(0xFFE7F2EB)

    // Secondary blue
    val Secondary = Color(0xFF5A8FCF)
    val SecondaryContainer = Color(0xFFDCECF3)

    // Surfaces
    val Background = Color(0xFFF7F9F7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF0F2F0)

    // Text
    val OnBackground = Color(0xFF26352D)
    val OnSurface = Color(0xFF26352D)
    val OnSurfaceVariant = Color(0xFF596B61)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnPrimaryContainer = Color(0xFF317854)

    // Dividers, outlines, drag handle (#DEE7E0 in SVGs)
    val Outline = Color(0xFFDEE7E0)
    val OutlineVariant = Color(0xFFD0D8D3)

    // Warning / amber (from SVG: bg #F8EFCE, text #816822)
    val WarningContainer = Color(0xFFF8EFCE)
    val OnWarningContainer = Color(0xFF816822)

    // Exposure status colors (data-driven only)
    val ExposureGood = Color(0xFF8FC9A8)
    val ExposureModerate = Color(0xFFE7D18B)
    val ExposureHigh = Color(0xFFE8A77F)
    val ExposureVeryHigh = Color(0xFFD98787)

    // State colors
    val Error = Color(0xFFD98787)
    val Warning = Color(0xFFE8A77F)
    val Success = Color(0xFF8FC9A8)
    val Info = Color(0xFF5A8FCF)

    // Map colors (from colors.json map tokens)
    val MapLand = Color(0xFFF7F9F7)
    val MapWater = Color(0xFFDCECF3)
    val MapPark = Color(0xFFE7F2EB)
    val MapMajorRoad = Color(0xFFB8C5BC)
    val MapMinorRoad = Color(0xFFE8EDE8)
    val MapBuilding = Color(0xFFF0F2F0)

    // Drag handle color
    val DragHandle = Color(0xFFDEE7E0)
}
