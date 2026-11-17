package com.roadguide.app.data.repository

import com.roadguide.app.data.local.settings.TrackingPreferences
import kotlinx.coroutines.flow.Flow

interface TrackingSettingsRepository {
    val preferencesFlow: Flow<TrackingPreferences>

    suspend fun setTrackingEnabled(enabled: Boolean)

    suspend fun setIntervalSeconds(seconds: Int)

    suspend fun setUnits(units: String)

    suspend fun setAqiThreshold(threshold: Int)
}
