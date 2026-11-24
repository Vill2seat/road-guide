package com.roadguide.app.domain.usecase

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.domain.usecase.impl.MovementDetectionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementDetectionEngineTest {

    private fun point(
        speedMps: Double,
        timestamp: Long,
        latitude: Double = 55.0,
        longitude: Double = 37.0
    ) = LocationPointSummary(
        id = 0L,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        speedMps = speedMps
    )

    @Test
    fun emptyListReturnsUnknown() {
        assertEquals(ActivityType.UNKNOWN, MovementDetectionEngine.classify(emptyList()))
    }

    @Test
    fun stationaryDetectedWithLowSpeed() {
        val points = (0..5).map { point(0.3, it * 60_000L) }
        assertEquals(ActivityType.STATIONARY, MovementDetectionEngine.classify(points))
        assertTrue(MovementDetectionEngine.detectStationary(points))
    }

    @Test
    fun stationaryRequiresMinimumDuration() {
        val points = (0..4).map { point(0.3, it * 1000L) }

        assertFalse(MovementDetectionEngine.detectStationary(points))
        assertTrue(MovementDetectionEngine.detectStationary(points, thresholdMs = 4_000L))
    }

    @Test
    fun stationaryCanBeDetectedAtTheConfiguredSamplingInterval() {
        val points = listOf(point(0.1, 0L), point(0.1, 300_000L))

        assertTrue(MovementDetectionEngine.detectStationary(points))
    }

    @Test
    fun stationaryRequiresStableLocationAndNoLongGpsGap() {
        val scattered = listOf(
            point(0.1, 0L),
            point(0.1, 60_000L, latitude = 55.002),
            point(0.1, 120_000L)
        )
        val gap = listOf(
            point(0.1, 0L),
            point(0.1, 400_000L)
        )

        assertFalse(MovementDetectionEngine.detectStationary(scattered, thresholdMs = 120_000L))
        assertFalse(MovementDetectionEngine.detectStationary(gap, thresholdMs = 120_000L))
    }

    @Test
    fun walkingDetectedWithModerateSpeed() {
        val points = (1..5).map { point(1.5, it * 1000L) }
        assertEquals(ActivityType.WALKING, MovementDetectionEngine.classify(points))
    }

    @Test
    fun runningDetectedWithSpeedBetween2And6() {
        val points = (1..5).map { point(4.0, it * 1000L) }
        assertEquals(ActivityType.RUNNING, MovementDetectionEngine.classify(points))
    }

    @Test
    fun cyclingDetectedWithHighSpeedAndAcceleration() {
        val points = listOf(
            point(7.0, 1000L),
            point(9.0, 1200L),
            point(7.0, 1400L),
            point(10.0, 1600L),
            point(8.0, 1800L),
            point(11.0, 2000L),
            point(9.0, 2200L),
            point(12.0, 2400L)
        )
        assertEquals(ActivityType.CYCLING, MovementDetectionEngine.classify(points))
    }

    @Test
    fun cyclingDetectedAtSteadyCyclingSpeed() {
        val points = (1..8).map { point(8.0, it * 10_000L) }

        assertEquals(ActivityType.CYCLING, MovementDetectionEngine.classify(points))
    }

    @Test
    fun inVehicleDetectedWithStableHighSpeed() {
        val points = (1..10).map { point(15.0, it * 1000L) }
        assertEquals(ActivityType.IN_VEHICLE, MovementDetectionEngine.classify(points))
    }

    @Test
    fun publicTransportDetectedWithAlternatingStops() {
        val points = mutableListOf<LocationPointSummary>()
        var t = 1000L
        repeat(3) { points.add(point(12.0, t)); t += 1000 }
        repeat(4) { points.add(point(0.2, t)); t += 10000 }
        repeat(3) { points.add(point(12.0, t)); t += 1000 }
        repeat(4) { points.add(point(0.2, t)); t += 10000 }
        repeat(3) { points.add(point(12.0, t)); t += 1000 }
        assertEquals(ActivityType.PUBLIC_TRANSPORT, MovementDetectionEngine.classify(points))
    }

    @Test
    fun detectStationaryReturnsFalseForMovingPoints() {
        val points = (1..5).map { point(2.0, it * 1000L) }
        assertFalse(MovementDetectionEngine.detectStationary(points))
    }

    @Test
    fun calculateConfidenceScalesWithSampleCount() {
        val low = MovementDetectionEngine.calculateConfidence(ActivityType.WALKING, 2)
        val high = MovementDetectionEngine.calculateConfidence(ActivityType.WALKING, 20)
        assertTrue(high >= low)
    }
}