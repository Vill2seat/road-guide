package com.roadguide.app.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.entity.RouteSegment
import com.roadguide.app.domain.usecase.impl.ExposureCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExposureCalculationIntegrationTest {

    @Test
    fun exposureUsesSegmentDurationActivityAndValidatedAirQuality() {
        val segment = RouteSegment(
            routeId = 1L,
            startTime = 0L,
            endTime = 1_800_000L,
            activityType = ActivityType.STATIONARY
        )
        val sample = EnvironmentalSample(
            latitude = 55.0,
            longitude = 37.0,
            timestamp = 900_000L,
            pm2_5 = 15.0,
            dataQuality = DataQuality.MEDIUM
        )

        val actual = ExposureCalculationEngine.calculateSegmentExposure(
            segment = segment,
            sample = sample,
            roadFactor = 1.0
        )

        assertEquals(1.75, actual ?: 0.0, 0.001)
    }

    @Test
    fun exposureTimeFactorIsCappedForLongSegments() {
        val segment = RouteSegment(
            routeId = 1L,
            startTime = 0L,
            endTime = 7_200_000L,
            activityType = ActivityType.STATIONARY
        )
        val sample = EnvironmentalSample(
            latitude = 55.0,
            longitude = 37.0,
            timestamp = 3_600_000L,
            pm2_5 = 15.0,
            dataQuality = DataQuality.MEDIUM
        )

        val actual = ExposureCalculationEngine.calculateSegmentExposure(
            segment = segment,
            sample = sample,
            roadFactor = 1.0
        )

        assertEquals(3.5, actual ?: 0.0, 0.001)
    }
}
