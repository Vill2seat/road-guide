package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.domain.model.ExposureConstants
import com.roadguide.app.data.local.entity.RouteSegment

/**
 * Implementation of [com.roadguide.app.domain.usecase.ExposureCalculation]
 *
 * Calculates segment exposure score using the formula:
 * E = K × P × T × A × R × W × S
 *
 * Spec constants:
 * - K = 5.0 (calibration)
 * - T_raw = durationSeconds / 1800.0, capped at EXPOSURE_T_MAX = 2.0
 * - Activity factors: STATIONARY=0.7, WALKING=1.0, CYCLING=1.3, RUNNING=1.5,
 *   IN_VEHICLE=0.9, PUBLIC_TRANSPORT=1.0, UNKNOWN=1.0
 * - R base = 1.0 (road source factor)
 */
object ExposureCalculationEngine {

    private val ACTIVITY_FACTORS = mapOf(
        ActivityType.STATIONARY to 0.7,
        ActivityType.WALKING to 1.0,
        ActivityType.RUNNING to 1.5,
        ActivityType.CYCLING to 1.3,
        ActivityType.IN_VEHICLE to 0.9,
        ActivityType.PUBLIC_TRANSPORT to 1.0,
        ActivityType.UNKNOWN to 1.0
    )

    /** Time factor: T = min(duration / 1800, 2.0) */
    fun calculateTimeFactor(durationSeconds: Long): Double {
        val raw = durationSeconds.toDouble() / ExposureConstants.TIME_REFERENCE_SECONDS
        return raw.coerceAtMost(ExposureConstants.EXPOSURE_T_MAX)
    }

    /** Activity factor lookup from ActivityType */
    fun getActivityFactor(activityType: ActivityType): Double {
        return ACTIVITY_FACTORS[activityType] ?: 1.0
    }

    /** Road/context factor based on geographic context */
    fun calculateRoadFactor(context: String?): Double {
        return when (context?.lowercase()) {
            "motorway", "trunk", "primary" -> 1.5
            "secondary", "tertiary" -> 1.2
            "residential", "living_street" -> 0.8
            "pedestrian", "path", "footway" -> 0.6
            "green", "park", "forest", "grass" -> 0.7
            "industrial" -> 1.4
            "commercial" -> 1.1
            else -> 1.0
        }
    }

    /** Weather factor based on wind, rain, temperature */
    fun calculateWeatherFactor(temperatureCelsius: Double?, windSpeedMs: Double?, precipitationMm: Double?): Double {
        var factor = 1.0

        // Wind: higher wind = better dispersion = lower exposure
        if (windSpeedMs != null) {
            if (windSpeedMs > 10.0) factor *= 0.8
            else if (windSpeedMs > 5.0) factor *= 0.9
        }

        // Rain: washing out effect
        if (precipitationMm != null && precipitationMm > 0.5) {
            factor *= 0.85
        }

        // Temperature: extreme cold/heat can increase exposure
        if (temperatureCelsius != null) {
            if (temperatureCelsius < -10) factor *= 1.1  // cold stress
            else if (temperatureCelsius > 35) factor *= 1.1  // heat stress
        }

        return factor.coerceIn(0.5, 1.5)
    }

    /** Pollution factor: max normalized pollutant value */
    fun calculatePollutionFactor(sample: EnvironmentalSample): Double {
        val pollutants = listOfNotNull(
            sample.pm2_5, sample.pm10, sample.nitrogenDioxide, sample.ozone,
            sample.sulphurDioxide, sample.carbonMonoxide
        ).map { it.coerceAtLeast(0.0) }

        if (pollutants.isEmpty()) return 1.0

        // Normalize each pollutant against WHO guideline thresholds
        val normalized = pollutants.map { value ->
            when {
                value <= 12.0 -> 0.5          // PM2.5 safe
                value <= 25.0 -> 0.8           // PM2.5 moderate
                value <= 50.0 -> 1.0           // PM2.5 elevated
                value <= 100.0 -> 1.5           // PM2.5 high
                else -> 2.0                     // PM2.5 very high
            }
        }

        return normalized.maxOf { it }
    }

    /**
     * Calculate segment exposure score (pre-normalization)
     * E_raw = K × P × T × A × R × W × S
     */
    fun calculateSegmentExposure(
        segment: RouteSegment,
        sample: EnvironmentalSample?,
        roadFactor: Double
    ): Double {
        val timeFactor = calculateTimeFactor(segment.endTime - segment.startTime)
        val activityFactor = getActivityFactor(segment.activityType)
        val pollutionFactor = sample?.let { calculatePollutionFactor(it) } ?: 1.0
        val weatherFactor = sample?.let {
            calculateWeatherFactor(
                it.temperatureCelsius,
                it.windSpeedMs,
                it.precipitationMm
            )
        } ?: 1.0
        val skinFactor = ExposureConstants.DEFAULT_SKIN_FACTOR
        val roadSourceFactor = ExposureConstants.EXPOSURE_ROAD_SOURCE_FACTOR

        val raw =
            ExposureConstants.EXPOSURE_CALIBRATION_K *
                pollutionFactor * timeFactor * activityFactor *
                roadSourceFactor * weatherFactor * skinFactor *
                roadFactor

        return raw.coerceIn(0.0, 200.0) // cap for numerical stability
    }

    /** Normalize raw exposure to 0-100 using sigmoid-like scaling */
    fun normalizeExposure(raw: Double): Double {
        // Sigmoid: S(x) = 100 / (1 + e^(-k(x - x0)))
        // Using k=0.1, x0=50 as reasonable defaults
        val k = 0.08
        val x0 = 45.0
        val normalized = 100.0 / (1.0 + Math.exp(-k * (raw - x0)))
        return normalized.coerceIn(0.0, 100.0)
    }
}
