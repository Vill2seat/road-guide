package com.roadguide.app.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class RouteEntityTest {

    @Test
    fun `route entity has correct defaults`() {
        val route = Route(
            id = 1L,
            startTime = 1000L,
            distanceMeters = 1500.0,
            status = RouteStatus.COMPLETED,
            routeType = RouteType.DAILY_AUTO,
            confidence = 0.95,
            date = "2026-01-01",
            totalDurationSeconds = 3600L
        )

        assertEquals(1L, route.id)
        assertEquals(1000L, route.startTime)
        assertEquals(1500.0, route.distanceMeters, 0.0)
        assertEquals(RouteStatus.COMPLETED, route.status)
        assertEquals(RouteType.DAILY_AUTO, route.routeType)
        assertEquals(0.95, route.confidence, 0.0)
        assertEquals("2026-01-01", route.date)
        assertEquals(3600L, route.totalDurationSeconds)
        assertEquals(1, route.calculationVersion)
    }

    @Test
    fun `route entity defaults`() {
        val route = Route(
            id = 0L,
            startTime = 0L,
            date = "2026-01-01"
        )

        assertEquals(0L, route.id)
        assertEquals(0L, route.startTime)
        assertEquals(0.0, route.distanceMeters, 0.0)
        assertEquals(RouteStatus.IN_PROGRESS, route.status)
        assertEquals(RouteType.DAILY_AUTO, route.routeType)
        assertEquals(0.0, route.confidence, 0.0)
        assertEquals(0L, route.totalDurationSeconds)
        assertEquals(1, route.calculationVersion)
    }
}
