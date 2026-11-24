package com.roadguide.app.feature.dailyreport

import kotlin.math.roundToInt

internal enum class ExposureScoreBand {
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH
}

internal enum class DataQualityBand {
    VERY_LOW,
    LOW,
    MODERATE,
    HIGH
}

internal fun exposureScoreBand(score: Double): ExposureScoreBand = when {
    !score.isFinite() || score >= 75.0 -> ExposureScoreBand.VERY_HIGH
    score >= 50.0 -> ExposureScoreBand.HIGH
    score >= 25.0 -> ExposureScoreBand.MODERATE
    else -> ExposureScoreBand.LOW
}

internal fun dataQualityBand(percent: Double): DataQualityBand = when {
    !percent.isFinite() || percent < 20.0 -> DataQualityBand.VERY_LOW
    percent < 50.0 -> DataQualityBand.LOW
    percent < 80.0 -> DataQualityBand.MODERATE
    else -> DataQualityBand.HIGH
}

internal fun formatDailyPercentage(percent: Double): String {
    val safePercent = if (percent.isFinite()) percent.coerceIn(0.0, 100.0) else 0.0
    return "${safePercent.roundToInt()}%"
}
