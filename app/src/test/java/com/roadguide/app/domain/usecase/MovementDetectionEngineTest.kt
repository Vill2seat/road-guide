package com.roadguide.app.domain.usecase

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.domain.usecase.impl.MovementDetectionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementDetectionEngineTest {

    private fun point(speedMps: Double, timestamp: Long) = LocationPointSummary(
        id = 0L,
        latitude = 55.0,
        longitude = 37.0,
        timestamp = timestamp,
        speedMps = speedMps
    )

    @Test
    fun emptyListReturnsUnknown() {
        assertEquals(ActivityType.UNKNOWN, MovementDetectionEngine.classify(emptyList()))
    }

    @Test
    fun stationaryDetectedWithLowSpeed() {
        val points = (1..5).map { point(0.3, it * 1000L) }
        assertEquals(ActivityType.STATIONARY, MovementDetectionEngine.classify(points))
        assertTrue(MovementDetectionEngine.detectStationary(points))
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