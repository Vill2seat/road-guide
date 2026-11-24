package com.roadguide.app.data.local

import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.DataSource
import com.roadguide.app.data.local.entity.EnvironmentalSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EnvironmentalValidatorTest {

    @Test
    fun `weather-only sample is downgraded to low quality`() {
        val timestamp = 1_000_000L
        val sample = EnvironmentalSample(
            latitude = 55.0,
            longitude = 37.0,
            timestamp = timestamp,
            temperatureCelsius = 18.0,
            dataQuality = DataQuality.HIGH,
            fetchedAt = timestamp
        )

        val quality = EnvironmentalValidator().validate(
            sample = sample,
            requestLat = sample.latitude,
            requestLon = sample.longitude,
            requestTime = timestamp,
            now = timestamp
        )

        assertEquals(DataQuality.LOW, quality)
    }

    @Test
    fun `valid pollutant measurement retains validated quality`() {
        val timestamp = 1_000_000L
        val sample = EnvironmentalSample(
            latitude = 55.0,
            longitude = 37.0,
            timestamp = timestamp,
            pm2_5 = 8.0,
            dataQuality = DataQuality.MEDIUM,
            fetchedAt = timestamp
        )

        val quality = EnvironmentalValidator().validate(
            sample = sample,
            requestLat = sample.latitude,
            requestLon = sample.longitude,
            requestTime = timestamp,
            now = timestamp
        )

        assertEquals(DataQuality.MEDIUM, quality)
    }

    @Test
    fun `hourly air quality sample is accepted within its past data window`() {
        val timestamp = 1_000_000L
        val sample = EnvironmentalSample(
            latitude = 55.0,
            longitude = 37.0,
            timestamp = timestamp,
            pm2_5 = 8.0,
            source = DataSource.OPEN_METEO_AQ,
            dataQuality = DataQuality.MEDIUM,
            fetchedAt = timestamp
        )

        assertEquals(
            DataQuality.MEDIUM,
            EnvironmentalValidator().validate(
                sample = sample,
                requestLat = sample.latitude,
                requestLon = sample.longitude,
                requestTime = timestamp + 25 * 60_000L,
                now = timestamp
            )
        )
        assertNull(
            EnvironmentalValidator().validate(
                sample = sample.copy(source = DataSource.OPEN_METEO),
                requestLat = sample.latitude,
                requestLon = sample.longitude,
                requestTime = timestamp + 25 * 60_000L,
                now = timestamp
            )
        )
    }

    @Test
    fun `hourly air quality sample outside the model time window is rejected`() {
        val timestamp = 1_000_000L
        val sample = EnvironmentalSample(
            latitude = 55.0,
            longitude = 37.0,
            timestamp = timestamp,
            pm2_5 = 8.0,
            source = DataSource.OPEN_METEO_AQ,
            dataQuality = DataQuality.MEDIUM,
            fetchedAt = timestamp
        )

        assertNull(
            EnvironmentalValidator().validate(
                sample = sample,
                requestLat = sample.latitude,
                requestLon = sample.longitude,
                requestTime = timestamp + 31 * 60_000L,
                now = timestamp
            )
        )
    }
}
