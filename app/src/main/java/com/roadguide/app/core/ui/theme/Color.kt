package com.roadguide.app.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design tokens exported from `design/figma-export/styles/colors.json`.
 * Light theme only (v1 — no dark mode per ADR-20241112-010).
 */
object RgColor {
    // Semantic colors
    val Primary = Color(0xFF6FAF8E)
    val PrimaryContainer = Color(0xFFE7F2EB)
    val Secondary = Color(0xFF5A8FCF)
    val SecondaryContainer = Color(0xFFDCECF3)
    val Background = Color(0xFFF7F9F7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF0F2F0)
    val OnBackground = Color(0xFF26352D)
    val OnSurface = Color(0xFF26352D)
    val OnSurfaceVariant = Color(0xFF7B8981)
    val Outline = Color(0xFFD0D8D3)

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
}