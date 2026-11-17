package com.roadguide.app.data.local

import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.EnvironmentalSample
import kotlin.math.abs

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
        // Age check
        val ageHours = (now - sample.fetchedAt) / (1000.0 * 60 * 60)
        if (ageHours > maxAgeHours) {
            return DataQuality.STALE
        }

        // Distance check (haversine approximation)
        val distanceKm = haversineKm(
            sample.latitude, sample.longitude,
            requestLat, requestLon
        )
        if (distanceKm > maxDistanceKm) {
            return null // too far away
        }

        // Time window check
        val timeDiffMinutes = abs(sample.timestamp - requestTime) / (1000.0 * 60)
        if (timeDiffMinutes > maxFutureOffsetMinutes * 2) {
            return null // too far from requested time
        }

        // Future offset check
        val futureOffset = (sample.fetchedAt - now) / (1000.0 * 60)
        if (futureOffset > maxFutureOffsetMinutes) {
            return DataQuality.OUT_OF_RANGE
        }

        // Return sample's own quality if it passes all checks
        return sample.dataQuality
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
}
