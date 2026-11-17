package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.data.repository.TrackingSettingsRepository
import kotlinx.coroutines.flow.first

class GetTrackingSettingsImpl(private val trackingSettingsRepository: TrackingSettingsRepository) : GetTrackingSettings {

    override suspend fun invoke(): TrackingSettingsState {
        val prefs = trackingSettingsRepository.preferencesFlow.first()
        return TrackingSettingsState(
            trackingEnabled = prefs.trackingEnabled,
            intervalSeconds = prefs.intervalSeconds,
            units = prefs.units,
            aqiThreshold = prefs.aqiThreshold
        )
    }
}
