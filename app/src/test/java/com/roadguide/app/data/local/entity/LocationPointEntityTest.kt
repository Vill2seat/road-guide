package com.roadguide.app.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationPointEntityTest {

    @Test
    fun `location point entity has correct values`() {
        val point = LocationPoint(
            id = 1L,
            latitude = 55.7558,
            longitude = 37.6173,
            timestamp = 1000000000L,
            accuracyMeters = 5f,
            speedMetersPerSecond = 2.5f,
            bearing = 90f,
            altitude = 100.0,
            provider = "fused",
            isMock = false,
            qualityFlags = 0,
            routeId = 1L,
            collectionDiagnostics = "test"
        )

        assertEquals(1L, point.id)
        assertEquals(55.7558, point.latitude, 0.0)
        assertEquals(37.6173, point.longitude, 0.0)
        assertEquals(1000000000L, point.timestamp)
        assertEquals(5f, point.accuracyMeters, 0.0f)
        assertEquals(2.5f, point.speedMetersPerSecond ?: 0f, 0.0f)
        assertEquals(90f, point.bearing ?: 0f, 0.0f)
        assertEquals(100.0, point.altitude ?: 0.0, 0.0)
        assertEquals("fused", point.provider)
        assertEquals(false, point.isMock)
        assertEquals(0, point.qualityFlags)
        assertEquals(1L, point.routeId)
        assertEquals("test", point.collectionDiagnostics)
    }

    @Test
    fun `location point has nullable defaults`() {
        val point = LocationPoint(
            id = 0L,
            latitude = 0.0,
            longitude = 0.0,
            timestamp = 0L,
            accuracyMeters = 0f,
            provider = "mock"
        )

        assertEquals(0f, point.accuracyMeters, 0.0f)
        assertEquals(null, point.speedMetersPerSecond)
        assertEquals(null, point.bearing)
        assertEquals(null, point.altitude)
        assertEquals(0, point.qualityFlags)
        assertEquals(null, point.routeId)
    }
}
