package com.roadguide.app.domain.usecase

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.domain.usecase.impl.RouteSegmentationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteSegmentationEngineTest {

    private fun point(
        time: Long,
        speed: Double,
        latitude: Double = 55.0,
        longitude: Double = 37.0
    ) = LocationPointSummary(
        id = time,
        latitude = latitude,
        longitude = longitude,
        timestamp = time,
        speedMps = speed
    )

    @Test
    fun `activity transition closes segments at observed timestamps`() {
        val points = listOf(
            point(0L, 1.2),
            point(60_000L, 1.2, latitude = 55.001),
            point(120_000L, 0.1, latitude = 55.002),
            point(180_000L, 0.1, latitude = 55.002),
            point(240_000L, 1.2, latitude = 55.003),
            point(300_000L, 1.2, latitude = 55.004)
        )

        val segments = RouteSegmentationEngine.segmentPoints(7L, 0L, points, 0.0)

        assertEquals(3, segments.size)
        assertEquals(ActivityType.WALKING, segments[0].activityType)
        assertEquals(0L, segments[0].startTime)
        assertEquals(60_000L, segments[0].endTime)
        assertEquals(ActivityType.STATIONARY, segments[1].activityType)
        assertEquals(120_000L, segments[1].startTime)
        assertEquals(180_000L, segments[1].endTime)
        assertEquals(ActivityType.WALKING, segments[2].activityType)
        assertEquals(240_000L, segments[2].startTime)
        assertEquals(300_000L, segments[2].endTime)
    }

    @Test
    fun `steady activity points are merged into one segment`() {
        val points = List(6) { index ->
            point(index * 60_000L, 1.2, latitude = 55.0 + index * 0.001)
        }

        val segments = RouteSegmentationEngine.segmentPoints(1L, 0L, points, 0.0)

        assertEquals(1, segments.size)
        assertEquals(0L, segments.single().startTime)
        assertEquals(300_000L, segments.single().endTime)
    }

    @Test
    fun `single noisy activity sample does not split a route`() {
        val points = listOf(
            point(0L, 1.2),
            point(60_000L, 1.2),
            point(120_000L, 0.1),
            point(180_000L, 1.2),
            point(240_000L, 1.2)
        )

        val segments = RouteSegmentationEngine.segmentPoints(1L, 0L, points, 0.0)

        assertEquals(1, segments.size)
        assertEquals(0L, segments.single().startTime)
        assertEquals(240_000L, segments.single().endTime)
    }

    @Test
    fun `gps gap splits segment and is excluded from distance`() {
        val points = listOf(
            point(0L, 1.2, latitude = 55.0, longitude = 37.0),
            point(60_000L, 1.2, latitude = 55.001, longitude = 37.001),
            point(600_000L, 1.2, latitude = 56.0, longitude = 38.0),
            point(660_000L, 1.2, latitude = 56.001, longitude = 38.001)
        )

        val segments = RouteSegmentationEngine.segmentPoints(1L, 0L, points, 0.0)
        val measuredDistance = RouteSegmentationEngine.recordedDistanceMeters(points)

        assertEquals(2, segments.size)
        assertEquals(60_000L, segments[0].endTime)
        assertEquals(600_000L, segments[1].startTime)
        assertTrue(segments.all { it.distanceMeters < 200.0 })
        assertTrue(measuredDistance < 500.0)
    }

    @Test
    fun `out of order points are sorted and duplicate timestamps are ignored`() {
        val points = listOf(
            point(60_000L, 1.2, latitude = 55.001),
            point(0L, 1.2),
            point(60_000L, 1.2, latitude = 55.5),
            point(120_000L, 1.2, latitude = 55.002)
        )

        val segments = RouteSegmentationEngine.segmentPoints(1L, 0L, points, 0.0)

        assertEquals(1, segments.size)
        assertEquals(0L, segments.single().startTime)
        assertEquals(120_000L, segments.single().endTime)
        assertTrue(segments.single().endLatitude < 56.0)
    }

    @Test
    fun `long stable stationary period creates a stay`() {
        val points = List(7) { index ->
            point(index * 60_000L, 0.1, latitude = 55.0, longitude = 37.0)
        }

        val analysis = RouteSegmentationEngine.analyzePoints(4L, 0L, points)

        assertEquals(1, analysis.stays.size)
        assertEquals(0L, analysis.stays.single().startTime)
        assertEquals(360_000L, analysis.stays.single().endTime)
        assertEquals(1, analysis.segments.size)
    }

    @Test
    fun `short stationary period is not a stay`() {
        val points = listOf(
            point(0L, 0.1),
            point(180_000L, 0.1)
        )

        val analysis = RouteSegmentationEngine.analyzePoints(4L, 0L, points)

        assertTrue(analysis.stays.isEmpty())
    }

    @Test
    fun `single point and short activity do not create segments`() {
        assertTrue(
            RouteSegmentationEngine.segmentPoints(
                1L,
                0L,
                listOf(point(0L, 1.2)),
                0.0
            ).isEmpty()
        )
        assertTrue(
            RouteSegmentationEngine.segmentPoints(
                1L,
                0L,
                listOf(point(0L, 1.2), point(20_000L, 1.2)),
                0.0
            ).isEmpty()
        )
    }

    @Test
    fun `points before route start are excluded`() {
        val points = listOf(
            point(0L, 1.2),
            point(60_000L, 1.2, latitude = 55.001),
            point(120_000L, 1.2, latitude = 55.002)
        )

        val segments = RouteSegmentationEngine.segmentPoints(1L, 60_000L, points, 0.0)

        assertEquals(1, segments.size)
        assertEquals(60_000L, segments.single().startTime)
        assertEquals(120_000L, segments.single().endTime)
    }
}
