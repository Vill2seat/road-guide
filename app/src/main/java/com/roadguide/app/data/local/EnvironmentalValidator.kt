package com.roadguide.app.data.local

import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.DataSource
import com.roadguide.app.data.local.entity.EnvironmentalSample

/**
 * Validates environmental samples against freshness, distance, and future-offset rules.
 * Missing env data is never treated as 0 — quality is downgraded instead.
 */
class EnvironmentalValidator(
    private val maxAgeHours: Int = ValidatorConstants.MAX_ENV_DATA_AGE_HOURS,
    private val maxDistanceKm: Double = ValidatorConstants.MAX_COORD_DISTANCE_KM,
    private val maxFutureOffsetMinutes: Int = ValidatorConstants.MAX_FUTURE_OFFSET_MINUTES
) {

    /**
     * Validates a cached sample against a requested time and location.
     * Returns the adjusted quality or null if the sample should be rejected.
     */
    fun validate(
        sample: EnvironmentalSample,
        requestLat: Double,
        requestLon: Double,
        requestTime: Long,
        now: Long = System.currentTimeMillis()
    ): DataQuality? {
        // Age check: data older than maxAgeHours -> STALE
        val ageHours = (now - sample.fetchedAt) / (1000.0 * 60 * 60)
        if (ageHours > maxAgeHours) {
            return DataQuality.STALE
        }

        // Distance check: too far away -> null (reject sample)
        val distanceKm = haversineKm(
            sample.latitude, sample.longitude,
            requestLat, requestLon
        )
        if (distanceKm > maxDistanceKm) {
            return null
        }

        // Time window check: too far from requested time -> null (reject sample)
        val sampleTimeOffsetMillis = sample.timestamp - requestTime
        val maxSampleTimeOffsetMillis = if (sample.source == DataSource.OPEN_METEO_AQ) {
            MAX_AQ_TIME_OFFSET_MS
        } else {
            maxFutureOffsetMinutes * 60_000L
        }
        if (sampleTimeOffsetMillis !in -maxSampleTimeOffsetMillis..maxSampleTimeOffsetMillis) {
            return null
        }

        // Future offset check: if the sample was fetched in the future (cyber attack etc.)
        if (sample.fetchedAt - now > maxSampleTimeOffsetMillis) {
            return DataQuality.OUT_OF_RANGE
        }

        // Weather-only samples remain useful, but cannot support air-quality scoring.
        return if (sample.hasValidAirQualityData()) sample.dataQuality else DataQuality.LOW
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    private companion object {
        const val MAX_AQ_TIME_OFFSET_MS = 30 * 60 * 1_000L
    }
}
