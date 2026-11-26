package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.data.repository.TrackingSettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take

class GetTrackingSettingsImpl(private val trackingSettingsRepository: TrackingSettingsRepository) : GetTrackingSettings {

    override suspend fun invoke(): TrackingSettingsState {
        val prefs = trackingSettingsRepository.preferencesFlow.take(1).firstOrNull()
            ?: return TrackingSettingsState(
                trackingEnabled = false,
                intervalSeconds = 60,
                units = "metric",
                aqiThreshold = 100
            )
        return TrackingSettingsState(
            trackingEnabled = prefs.trackingEnabled,
            intervalSeconds = prefs.intervalSeconds,
            units = prefs.units,
            aqiThreshold = prefs.aqiThreshold
        )
    }
}
