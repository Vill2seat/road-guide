package com.roadguide.app.data.local.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackingPrefsConverterTest {

    @Test
    fun `tracking preferences serializes to string`() {
        val prefs = TrackingPreferences(
            enabled = true,
            intervalSeconds = 60,
            units = "metric",
            aqiThreshold = 100
        )

        val json = prefs.toJson()
        println("JSON: $json")
        // JSON should contain all fields
        assert(json.contains("enabled"))
        assert(json.contains("interval_seconds"))
        assert(json.contains("units"))
        assert(json.contains("aqi_threshold"))
    }

    @Test
    fun `tracking preferences deserializes from string`() {
        val json = "{\"enabled\":false,\"interval_seconds\":30,\"units\":\"imperial\",\"aqi_threshold\":150}"
        val prefs = TrackingPreferences.fromJson(json)

        assertEquals(false, prefs.enabled)
        assertEquals(30, prefs.intervalSeconds)
        assertEquals("imperial", prefs.units)
        assertEquals(150, prefs.aqiThreshold)
    }

    @Test
    fun `tracking preferences defaults when json is empty`() {
        val prefs = TrackingPreferences.fromJson("")
        assertEquals(false, prefs.enabled)
        assertEquals(60, prefs.intervalSeconds)
        assertEquals("metric", prefs.units)
        assertEquals(100, prefs.aqiThreshold)
    }
}
