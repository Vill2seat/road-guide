package com.roadguide.app.service

import com.roadguide.app.domain.model.ExposureConstants
import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import com.roadguide.app.data.local.dao.LocationPointDao
import com.roadguide.app.data.local.dao.RouteDao
import com.roadguide.app.domain.usecase.GetEnvironmentalSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Tracking pipeline.
 * Collects locations → segments/stays → exposure → daily report.
 * Exposure calculation integrates real environmental data (weather + AQI).
 */
class TrackingPipeline(
    private val locationPointDao: LocationPointDao,
    private val routeDao: RouteDao,
    private val getEnvironmentalSample: GetEnvironmentalSample
) {

    /**
     * Collects location points and creates a route skeleton.
     * Returns flow of location points for upstream processing.
     */
    fun collectLocations(startTime: Long, endTime: Long): Flow<LocationPoint> = flow {
        // Location collection from GPS/Android Location API — not yet wired.
        // Requires ACCESS_FINE_LOCATION / ACCESS_BACKGROUND_LOCATION permissions
        // and Foreground Service implementation before real emission.
    }

    /**
     * Segments locations into stays based on speed/altitude criteria.
     * This is a skeleton implementation.
     */
    suspend fun segmentLocations(points: List<LocationPoint>): List<com.roadguide.app.data.local.entity.Stay> {
        // Stay detection based on speed/altitude criteria — not yet wired.
        // Requires collected location points from collectLocations().
        return emptyList()
    }

    /**
     * Calculates exposure based on formula:
     * E = K × P × T × A × R × W × S
     *
     * Using ExposureConstants:
     * - EXPOSURE_CALIBRATION_K = 5.0
     * - EXPOSURE_T_MAX = 2.0
     * - EXPOSURE_ROAD_SOURCE_FACTOR = 1.0
     * - DEFAULT_SKIN_FACTOR = 1.0
     * - DEFAULT_SCREEN_FACTOR = 1.0
     * - ACTIVITY_FACTOR_STATIONARY = 0.7
     *
     * Parameter P (exposureLevel) comes from environmental data (AQI/pollution).
     */
    fun calculateExposure(
        durationSeconds: Long,
        exposureLevel: Double,
        skinFactor: Double = ExposureConstants.DEFAULT_SKIN_FACTOR,
        screenFactor: Double = ExposureConstants.DEFAULT_SCREEN_FACTOR
    ): Double {
        val t = (durationSeconds / 3600.0).coerceAtMost(ExposureConstants.EXPOSURE_T_MAX)
        val activityFactor = if (exposureLevel < 0.3) {
            ExposureConstants.ACTIVITY_FACTOR_STATIONARY
        } else {
            1.0
        }

        return ExposureConstants.EXPOSURE_CALIBRATION_K *
            exposureLevel *
            t *
            activityFactor *
            ExposureConstants.EXPOSURE_ROAD_SOURCE_FACTOR *
            skinFactor *
            screenFactor
    }

    /**
     * Calculates exposure for a given location by fetching real environmental data first.
     * Returns the exposure value E and the environmental sample used.
     */
    suspend fun calculateExposureForLocation(
        lat: Double,
        lon: Double,
        durationSeconds: Long,
        skinFactor: Double = ExposureConstants.DEFAULT_SKIN_FACTOR,
        screenFactor: Double = ExposureConstants.DEFAULT_SCREEN_FACTOR
    ): ExposureCalculationResult {
        val sample = getEnvironmentalSample.invoke(lat, lon)
        val exposureLevel = sample?.let { s ->
            // Derive exposure level from AQI and particulate matter
            val aqi = s.europeanAqi ?: s.usAqi ?: 0
            val pm2_5 = s.pm2_5 ?: 0.0
            val pm10 = s.pm10 ?: 0.0

            // Normalize AQI (0-500 → 0.0-1.0)
            val aqiLevel = (aqi.coerceIn(0, 500) / 500.0)
            // PM2.5 adjustment (0-100 µg/m³ → 0.0-1.0)
            val pmAdjustment = (pm2_5.coerceIn(0.0, 100.0) / 100.0) * 0.3

            (aqiLevel * 0.7 + pmAdjustment).coerceIn(0.0, 1.0)
        } ?: 0.0

        val exposureValue = calculateExposure(
            durationSeconds = durationSeconds,
            exposureLevel = exposureLevel,
            skinFactor = skinFactor,
            screenFactor = screenFactor
        )

        return ExposureCalculationResult(
            exposureValue = exposureValue,
            exposureLevel = exposureLevel,
            environmentalSample = sample,
            dataQuality = sample?.dataQuality
        )
    }

    /**
     * Creates a daily report with normalized 0-100 score.
     */
    suspend fun createDailyReport(
        date: String,
        exposureValue: Double
    ): com.roadguide.app.data.local.entity.DailyReport {
        return com.roadguide.app.data.local.entity.DailyReport(
            date = date,
            totalScore = exposureValue.coerceIn(0.0, 100.0),
            confidence = 0.0,
            coverage = 0.0,
            totalDistanceMeters = 0.0,
            totalDurationSeconds = 0,
            walkingDurationSeconds = 0,
            transportDurationSeconds = 0,
            stationaryDurationSeconds = 0,
            visitedPlacesCount = 0,
            topContributorsJson = "[]",
            bestPeriodJson = null,
            worstPeriodJson = null,
            recommendationsJson = "[]",
            reportVersion = 1,
            generatedAt = System.currentTimeMillis(),
            isRegenerated = false
        )
    }
}

/**
 * Result of exposure calculation with environmental context.
 */
data class ExposureCalculationResult(
    val exposureValue: Double,
    val exposureLevel: Double,
    val environmentalSample: EnvironmentalSample?,
    val dataQuality: DataQuality?
)
