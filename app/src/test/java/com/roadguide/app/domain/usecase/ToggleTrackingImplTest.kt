package com.roadguide.app.domain.usecase

import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.domain.usecase.impl.ToggleTrackingImpl
import com.roadguide.app.data.repository.TrackingSettingsRepository
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToggleTrackingImplTest {

    private val mockTrackingSettingsRepository = object : TrackingSettingsRepository {
        override val preferencesFlow = flow {
            emit(TrackingPreferences(
                trackingEnabled = false,
                intervalSeconds = 60,
                units = "metric",
                aqiThreshold = 100
            ))
        }

        override suspend fun setTrackingEnabled(enabled: Boolean) {
            println("setTrackingEnabled called with: $enabled")
        }

        override suspend fun setIntervalSeconds(seconds: Int) {}
        override suspend fun setUnits(units: String) {}
        override suspend fun setAqiThreshold(threshold: Int) {}
    }

    private val useCase = ToggleTrackingImpl(mockTrackingSettingsRepository)

    @Test
    fun `toggleTracking enables when called with true`() = kotlinx.coroutines.runBlocking {
        val result = useCase.invoke(true)

        assertTrue(result.trackingEnabled)
        assertEquals(60, result.intervalSeconds)
        assertEquals("metric", result.units)
        assertEquals(100, result.aqiThreshold)
    }

    @Test
    fun `toggleTracking disables when called with false`() = kotlinx.coroutines.runBlocking {
        val result = useCase.invoke(false)

        assertFalse(result.trackingEnabled)
        assertEquals(60, result.intervalSeconds)
        assertEquals("metric", result.units)
        assertEquals(100, result.aqiThreshold)
    }
}
