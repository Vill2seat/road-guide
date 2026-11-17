package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.domain.usecase.ToggleTracking
import com.roadguide.app.data.repository.TrackingSettingsRepository

/**
 * Use case implementation for toggling tracking enabled state.
 * Wires domain contract to TrackingSettingsRepository.
 */
class ToggleTrackingImpl(private val trackingSettingsRepository: TrackingSettingsRepository) : ToggleTracking {

    override suspend fun invoke(enabled: Boolean): TrackingSettingsState {
        trackingSettingsRepository.setTrackingEnabled(enabled)
        return TrackingSettingsState(
            trackingEnabled = enabled,
            intervalSeconds = 60,
            units = "metric",
            aqiThreshold = 100
        )
    }
}
