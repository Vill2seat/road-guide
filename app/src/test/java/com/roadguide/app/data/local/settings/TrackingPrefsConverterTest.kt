package com.roadguide.app.data.local.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingPrefsConverterTest {

    @Test
    fun `tracking preferences has correct defaults`() {
        val prefs = TrackingPreferences(
            trackingEnabled = true,
            intervalSeconds = 60,
            units = "metric",
            aqiThreshold = 100
        )

        assertEquals(true, prefs.trackingEnabled)
        assertEquals(60, prefs.intervalSeconds)
        assertEquals("metric", prefs.units)
        assertEquals(100, prefs.aqiThreshold)
    }

    @Test
    fun `tracking preferences defaults when constructed empty`() {
        val prefs = TrackingPreferences()

        assertEquals(false, prefs.trackingEnabled)
        assertEquals(false, prefs.trackingPaused)
        assertEquals(60, prefs.intervalSeconds)
        assertEquals("metric", prefs.units)
        assertEquals(100, prefs.aqiThreshold)
    }
}
