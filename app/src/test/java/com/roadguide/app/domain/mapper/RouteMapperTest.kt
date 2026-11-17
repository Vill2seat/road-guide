package com.roadguide.app.domain.mapper

import com.roadguide.app.domain.model.RouteSummary
import com.roadguide.app.mapper.RouteMapper
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RouteMapperTest {

    @Test
    fun `toSummary maps route entity to domain model correctly`() {
        val entity = Route(
            id = 1L,
            startTime = 1000L,
            endTime = 5000L,
            distanceMeters = 1500.0,
            status = RouteStatus.COMPLETED,
            routeType = RouteType.DAILY_AUTO,
            confidence = 0.95,
            date = "2026-01-01",
            totalDurationSeconds = 3600L
        )

        val summary = RouteMapper.toSummary(entity)

        assertEquals(1L, summary.id)
        assertEquals(1000L, summary.startTime)
        assertEquals(5000L, summary.endTime)
        assertEquals(1500.0, summary.distanceMeters, 0.0)
        assertEquals(3600L, summary.durationSeconds)
        assertEquals("completed", summary.status)
        assertEquals("daily_auto", summary.routeType)
    }

    @Test
    fun `toSummary maps route entity with null end time`() {
        val entity = Route(
            id = 2L,
            startTime = 2000L,
            endTime = null,
            distanceMeters = 1000.0,
            status = RouteStatus.IN_PROGRESS,
            routeType = RouteType.MANUAL_TRACKING,
            confidence = 0.8,
            date = "2026-01-02",
            totalDurationSeconds = 1800L
        )

        val summary = RouteMapper.toSummary(entity)

        assertEquals(2L, summary.id)
        assertEquals(2000L, summary.startTime)
        assertEquals(null, summary.endTime)
        assertEquals(1000.0, summary.distanceMeters, 0.0)
        assertEquals("in_progress", summary.status)
        assertEquals("daily_auto", summary.routeType)
    }
}
