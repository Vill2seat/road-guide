package com.roadguide.app.data.source

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class OpenMeteoAirQualitySourceTest {

    @Test
    fun `parses the closest valid hourly values and utc timestamp`() {
        val response = """
            {
              "hourly": {
                "time": ["2026-11-24T08:00", "2026-11-24T09:00"],
                "pm2_5": [8.5, 12.5],
                "pm10": [18.0, 24.0],
                "nitrogen_dioxide": [7.1, 10.1],
                "ozone": [30.0, 40.0],
                "sulphur_dioxide": [1.0, 2.0],
                "carbon_monoxide": [120.0, 180.0],
                "us_aqi": [32, 42],
                "european_aqi": [21, 31]
              }
            }
        """.trimIndent()
        val requestedTime = LocalDateTime.parse("2026-11-24T08:15")
            .toInstant(ZoneOffset.UTC).toEpochMilli()

        val sample = OpenMeteoAirQualitySource.parseResponse(response, requestedTime)

        assertEquals(8.5, sample?.pm2_5 ?: -1.0, 0.0)
        assertEquals(32, sample?.usAqi)
        assertEquals(
            LocalDateTime.parse("2026-11-24T08:00").toInstant(ZoneOffset.UTC).toEpochMilli(),
            sample?.timestamp
        )
    }

    @Test
    fun `rejects response with only invalid air quality values`() {
        val response = """
            {
              "hourly": {
                "time": ["2026-11-24T08:00"],
                "pm2_5": [-1],
                "pm10": [null],
                "us_aqi": [900],
                "european_aqi": [-4]
              }
            }
        """.trimIndent()
        val requestedTime = LocalDateTime.parse("2026-11-24T08:00")
            .toInstant(ZoneOffset.UTC).toEpochMilli()

        assertNull(OpenMeteoAirQualitySource.parseResponse(response, requestedTime))
    }
}
