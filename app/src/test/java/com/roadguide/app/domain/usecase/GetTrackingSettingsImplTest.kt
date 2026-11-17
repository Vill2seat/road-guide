package com.roadguide.app.domain.usecase

import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.domain.usecase.impl.GetTrackingSettingsImpl
import com.roadguide.app.data.repository.TrackingSettingsRepository
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GetTrackingSettingsImplTest {

    private val mockTrackingSettingsRepository = object : TrackingSettingsRepository {
        override val preferencesFlow = flow {
            emit(TrackingPreferences(
                trackingEnabled = true,
                intervalSeconds = 60,
                units = "metric",
                aqiThreshold = 100
            ))
        }

        override suspend fun setTrackingEnabled(enabled: Boolean) {}
        override suspend fun setIntervalSeconds(seconds: Int) {}
        override suspend fun setUnits(units: String) {}
        override suspend fun setAqiThreshold(threshold: Int) {}
    }

    private val useCase = GetTrackingSettingsImpl(mockTrackingSettingsRepository)

    @Test
    fun `getTrackingSettings returns correct state when preferences exist`() {
        val result = useCase.invoke()

        assertTrue(result.trackingEnabled)
        assertEquals(60, result.intervalSeconds)
        assertEquals("metric", result.units)
        assertEquals(100, result.aqiThreshold)
    }
}
