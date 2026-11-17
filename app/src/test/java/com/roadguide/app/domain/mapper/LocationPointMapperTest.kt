package com.roadguide.app.domain.mapper

import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.mapper.LocationPointMapper
import com.roadguide.app.data.local.entity.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationPointMapperTest {

    @Test
    fun `toSummary maps location point entity to domain model correctly`() {
        val entity = LocationPoint(
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

        val summary = LocationPointMapper.toSummary(entity)

        assertEquals(1L, summary.id)
        assertEquals(55.7558, summary.latitude, 0.0)
        assertEquals(37.6173, summary.longitude, 0.0)
        assertEquals(1000000000L, summary.timestamp)
        assertEquals("walking", summary.activityType)
        assertEquals(2.5, summary.speedMps, 0.0)
        assertEquals(100.0, summary.altitudeMeters, 0.0)
    }

    @Test
    fun `toSummary handles nullable fields correctly`() {
        val entity = LocationPoint(
            id = 2L,
            latitude = 56.0,
            longitude = 38.0,
            timestamp = 2000000000L,
            accuracyMeters = 10f,
            provider = "network"
        )

        val summary = LocationPointMapper.toSummary(entity)

        assertEquals(2L, summary.id)
        assertEquals(56.0, summary.latitude, 0.0)
        assertEquals(38.0, summary.longitude, 0.0)
        assertEquals(2000000000L, summary.timestamp)
        assertEquals("walking", summary.activityType)
        assertEquals(0.0, summary.speedMps, 0.0)
        assertEquals(0.0, summary.altitudeMeters, 0.0)
    }
}
