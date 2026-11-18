package com.roadguide.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExposureConstantsTest {

    @Test
    fun `exposure constants have correct values`() {
        // Verification of formula: E = K × P × T × A × R × W × S
        // Using ExposureConstants: E = K × P × T × A × R × W × S
        // K = 5.0, T_max = 2.0, R = 1.0, S = 1.0 (default skin), Screen = 1.0 (default)
        // A depends on activity (stationary = 0.7)

        val k = ExposureConstants.EXPOSURE_CALIBRATION_K
        val tMax = ExposureConstants.EXPOSURE_T_MAX
        val roadFactor = ExposureConstants.EXPOSURE_ROAD_SOURCE_FACTOR
        val defaultSkin = ExposureConstants.DEFAULT_SKIN_FACTOR
        val defaultScreen = ExposureConstants.DEFAULT_SCREEN_FACTOR
        val stationaryFactor = ExposureConstants.ACTIVITY_FACTOR_STATIONARY

        // Test 1: Maximum exposure with stationary activity
        val maxExposure = calculateExposure(1800.0, // duration = 30 min = 1800s
            pollutionLevel = 1.0, // maximum pollution level
            skinFactor = defaultSkin,
            screenFactor = defaultScreen
        )

        // E = 5.0 × 1.0 × 0.5 × 1.0 × 1.0 × 1.0 × 1.0 = 2.5
        // (T = 1800/3600 = 0.5, so T < T_max, so T = 0.5; pollutionLevel >= 0.3 so activityFactor = 1.0)
        assertEquals(2.5, maxExposure, 0.001)

        // Test 2: With higher pollution level
        val highPollution = calculateExposure(1800.0, 1.5)
        // E = 5.0 × 1.5 × 0.5 × 1.0 × 1.0 × 1.0 × 1.0 = 3.75
        assertEquals(3.75, highPollution, 0.001)

        // Test 3: With longer duration (approaches T_max)
        val longDuration = calculateExposure(3600.0, 1.0) // 1 hour = 3600s
        // T = 3600/3600 = 1.0, which is < T_max (2.0)
        // E = 5.0 × 1.0 × 1.0 × 1.0 × 1.0 × 1.0 × 1.0 = 5.0
        assertEquals(5.0, longDuration, 0.001)

        // Test 4: Verify T_max boundary
        val tMaxDuration = calculateExposure(7200.0, 1.0) // 2 hours = 7200s
        // T = min(7200/3600, 2.0) = 2.0
        // E = 5.0 × 1.0 × 2.0 × 1.0 × 1.0 × 1.0 × 1.0 = 10.0
        assertEquals(10.0, tMaxDuration, 0.001)
    }

    @Test
    fun `exposure constants are positive and finite`() {
        assertEquals(1800.0, ExposureConstants.TIME_REFERENCE_SECONDS, 0.0)
        assertEquals(2.0, ExposureConstants.EXPOSURE_T_MAX, 0.0)
        assertEquals(1.0, ExposureConstants.EXPOSURE_ROAD_SOURCE_FACTOR, 0.0)
        assertEquals(5.0, ExposureConstants.EXPOSURE_CALIBRATION_K, 0.0)
        assertEquals(1.0, ExposureConstants.DEFAULT_SKIN_FACTOR, 0.0)
        assertEquals(1.0, ExposureConstants.DEFAULT_SCREEN_FACTOR, 0.0)
        assertEquals(0.7, ExposureConstants.ACTIVITY_FACTOR_STATIONARY, 0.0)
    }

    private fun calculateExposure(
        durationSeconds: Double,
        pollutionLevel: Double,
        skinFactor: Double = ExposureConstants.DEFAULT_SKIN_FACTOR,
        screenFactor: Double = ExposureConstants.DEFAULT_SCREEN_FACTOR
    ): Double {
        val t = java.lang.Math.min(durationSeconds / 3600.0, ExposureConstants.EXPOSURE_T_MAX)
        val activityFactor = if (pollutionLevel < 0.3) {
            ExposureConstants.ACTIVITY_FACTOR_STATIONARY
        } else {
            1.0
        }

        return ExposureConstants.EXPOSURE_CALIBRATION_K *
            pollutionLevel *
            t *
            activityFactor *
            ExposureConstants.EXPOSURE_ROAD_SOURCE_FACTOR *
            skinFactor *
            screenFactor
    }
}
