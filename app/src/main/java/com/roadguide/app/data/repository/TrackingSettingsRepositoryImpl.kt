package com.roadguide.app.data.repository

import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.local.settings.TrackingPreferences
import android.content.Context
import kotlinx.coroutines.flow.Flow

class TrackingSettingsRepositoryImpl(private val trackingPrefs: TrackingPrefs) : TrackingSettingsRepository {

    override val preferencesFlow: Flow<TrackingPreferences> = trackingPrefs.preferencesFlow

    override suspend fun setTrackingEnabled(enabled: Boolean) {
        trackingPrefs.setTrackingEnabled(enabled)
    }

    override suspend fun setIntervalSeconds(seconds: Int) {
        trackingPrefs.setIntervalSeconds(seconds)
    }

    override suspend fun setUnits(units: String) {
        trackingPrefs.setUnits(units)
    }

    override suspend fun setAqiThreshold(threshold: Int) {
        trackingPrefs.setAqiThreshold(threshold)
    }
}
