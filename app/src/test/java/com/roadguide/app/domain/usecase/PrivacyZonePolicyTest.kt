package com.roadguide.app.domain.usecase

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.local.entity.PrivacyStatus
import com.roadguide.app.data.local.entity.PrivacyZone
import com.roadguide.app.data.local.entity.RouteSegment
import com.roadguide.app.data.local.entity.Stay
import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.domain.usecase.impl.PrivacyScope
import com.roadguide.app.domain.usecase.impl.PrivacyZonePolicy
import com.roadguide.app.domain.usecase.impl.RouteSegmentationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyZonePolicyTest {

    @Test
    fun `history exclusion removes points and splits route edges crossing the zone`() {
        val zone = zone(excludeFromHistory = true, excludeFromExposure = false)
        val points = listOf(
            summary(0.0, -0.03, 0L),
            summary(0.0, -0.02, 60_000L),
            summary(0.0, 0.02, 120_000L),
            summary(0.0, 0.03, 180_000L)
        )

        val analysis = RouteSegmentationEngine.analyzePoints(
            routeId = 7L,
            routeStartTime = 0L,
            points = points,
            privacyZones = listOf(zone)
        )

        assertEquals(2, analysis.segments.size)
        assertTrue(analysis.segments.all { it.endTime - it.startTime == 60_000L })
        assertFalse(
            PrivacyZonePolicy.excludesPath(
                0.0, -0.03, 0.0, -0.02, listOf(zone), PrivacyScope.HISTORY
            )
        )
    }

    @Test
    fun `export retains exposure-only points but marks exposure as unavailable`() {
        val zone = zone(excludeFromHistory = false, excludeFromExposure = true)
        val points = listOf(
            point(0.0, 0.0, 1_000L),
            point(0.0, 0.03, 2_000L)
        )

        val exportRows = PrivacyZonePolicy.prepareForExport(points, listOf(zone))

        assertEquals(2, exportRows.size)
        assertFalse(exportRows.first().includeExposure)
        assertTrue(exportRows.last().includeExposure)
    }

    @Test
    fun `inactive and overlapping zones follow explicit active exclusion flags`() {
        val inactive = zone(
            isActive = false,
            excludeFromHistory = true,
            excludeFromExposure = true
        )
        val exposureOnly = zone(
            excludeFromHistory = false,
            excludeFromExposure = true
        )
        val decision = PrivacyZonePolicy.decisionAt(0.0, 0.0, listOf(inactive, exposureOnly))

        assertFalse(decision.excludeFromHistory)
        assertTrue(decision.excludeFromExposure)
    }

    @Test
    fun `exposure-only zone keeps route history but excludes crossing segment from exposure`() {
        val zone = zone(excludeFromHistory = false, excludeFromExposure = true)
        val segment = RouteSegment(
            routeId = 7L,
            startTime = 0L,
            endTime = 60_000L,
            activityType = ActivityType.WALKING,
            startLatitude = 0.0,
            startLongitude = -0.02,
            endLatitude = 0.0,
            endLongitude = 0.02
        )

        assertFalse(
            PrivacyZonePolicy.excludesSegment(
                segment,
                emptyList(),
                listOf(zone),
                PrivacyScope.HISTORY
            )
        )
        assertTrue(
            PrivacyZonePolicy.excludesSegment(
                segment,
                emptyList(),
                listOf(zone),
                PrivacyScope.EXPOSURE
            )
        )
    }

    @Test
    fun `non-public stays are excluded from history and exposure`() {
        val stay = Stay(
            routeId = 7L,
            startTime = 0L,
            endTime = 60_000L,
            privacyStatus = PrivacyStatus.PRIVATE
        )

        assertTrue(PrivacyZonePolicy.excludesStay(stay, emptyList(), PrivacyScope.HISTORY))
        assertTrue(PrivacyZonePolicy.excludesStay(stay, emptyList(), PrivacyScope.EXPOSURE))
    }

    @Test
    fun `stay whose radius overlaps a privacy zone is excluded`() {
        val zone = zone(excludeFromHistory = true, excludeFromExposure = false)
        val stay = Stay(
            routeId = 7L,
            startTime = 0L,
            endTime = 60_000L,
            centerLongitude = 0.01,
            radiusMeters = 150.0
        )

        assertTrue(
            PrivacyZonePolicy.excludesStay(stay, listOf(zone), PrivacyScope.HISTORY)
        )
    }

    @Test
    fun `invalid route coordinates fail closed when an exclusion zone applies`() {
        val zone = zone(excludeFromHistory = true, excludeFromExposure = false)

        assertTrue(
            PrivacyZonePolicy.excludesPath(
                Double.NaN,
                0.0,
                0.0,
                0.0,
                listOf(zone),
                PrivacyScope.HISTORY
            )
        )
    }

    private fun zone(
        isActive: Boolean = true,
        excludeFromHistory: Boolean,
        excludeFromExposure: Boolean
    ) = PrivacyZone(
        name = "test",
        centerLatitude = 0.0,
        centerLongitude = 0.0,
        radiusMeters = 1_000.0,
        isActive = isActive,
        excludeFromHistory = excludeFromHistory,
        excludeFromExposure = excludeFromExposure
    )

    private fun point(latitude: Double, longitude: Double, timestamp: Long) = LocationPoint(
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp,
        accuracyMeters = 5f,
        provider = "test"
    )

    private fun summary(latitude: Double, longitude: Double, timestamp: Long) =
        LocationPointSummary(
            id = timestamp,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            activityType = "walking",
            speedMps = 1.0
        )
}
