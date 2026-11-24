package com.roadguide.app.domain.usecase

import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.entity.RouteSegment
import com.roadguide.app.domain.usecase.impl.ExposureCalculationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExposureCalculationEngineTest {

    private val segment = RouteSegment(
        routeId = 1L,
        startTime = 0L,
        endTime = 60_000L,
        activityType = ActivityType.WALKING
    )

    @Test
    fun `weather-only sample does not produce exposure score`() {
        val sample = EnvironmentalSample(
            latitude = 55.0,
            longitude = 37.0,
            timestamp = 1_000L,
            temperatureCelsius = 20.0,
            dataQuality = DataQuality.LOW
        )

        assertNull(ExposureCalculationEngine.calculatePollutionFactor(sample))
        assertNull(ExposureCalculationEngine.calculateSegmentExposure(segment, sample, 1.0))
        assertNull(ExposureCalculationEngine.calculateSegmentExposure(segment, null, 1.0))
    }

    @Test
    fun `valid pollutant or aqi measurement enables exposure score`() {
        val pollutantSample = EnvironmentalSample(
            latitude = 55.0,
            longitude = 37.0,
            timestamp = 1_000L,
            pm2_5 = 10.0,
            dataQuality = DataQuality.MEDIUM
        )
        val aqiSample = pollutantSample.copy(
            pm2_5 = null,
            usAqi = 50,
            dataQuality = DataQuality.MEDIUM
        )

        assertNotNull(ExposureCalculationEngine.calculateSegmentExposure(segment, pollutantSample, 1.0))
        assertNotNull(ExposureCalculationEngine.calculateSegmentExposure(segment, aqiSample, 1.0))
        assertTrue((ExposureCalculationEngine.calculatePollutionFactor(aqiSample) ?: 0.0) > 0.0)
    }

    @Test
    fun `pollutants use their own concentration guidelines`() {
        val sample = EnvironmentalSample(
            latitude = 55.0,
            longitude = 37.0,
            timestamp = 1_000L,
            nitrogenDioxide = 100.0,
            dataQuality = DataQuality.MEDIUM
        )

        assertEquals(1.0, ExposureCalculationEngine.calculatePollutionFactor(sample) ?: 0.0, 0.0)
    }

    @Test
    fun `out of range pollutant concentration cannot produce an exposure score`() {
        val sample = EnvironmentalSample(
            latitude = 55.0,
            longitude = 37.0,
            timestamp = 1_000L,
            pm2_5 = 100_001.0,
            dataQuality = DataQuality.MEDIUM
        )

        assertNull(ExposureCalculationEngine.calculatePollutionFactor(sample))
    }

    @Test
    fun `segment timestamps in milliseconds are converted to seconds for time factor`() {
        val sample = EnvironmentalSample(
            latitude = 55.0,
            longitude = 37.0,
            timestamp = 1_000L,
            pm2_5 = 15.0,
            dataQuality = DataQuality.MEDIUM
        )

        assertEquals(
            5.0 * 0.5 * (60.0 / 1_800.0),
            ExposureCalculationEngine.calculateSegmentExposure(segment, sample, 1.0) ?: 0.0,
            1e-9
        )
    }
}
