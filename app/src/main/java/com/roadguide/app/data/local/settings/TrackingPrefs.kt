package com.roadguide.app.data.local.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.trackingDataStore by preferencesDataStore(name = "tracking_prefs")

data class TrackingPreferences(
    val trackingEnabled: Boolean = true,
    val intervalSeconds: Int = 60,
    val units: String = "metric",
    val aqiThreshold: Int = 100
)

class TrackingPrefs(private val context: Context) {

    val preferencesFlow: Flow<TrackingPreferences> = context.trackingDataStore.data.map { prefs ->
        TrackingPreferences(
            trackingEnabled = prefs[TRACKING_ENABLED] ?: true,
            intervalSeconds = prefs[INTERVAL_SECONDS] ?: 60,
            units = prefs[UNITS] ?: "metric",
            aqiThreshold = prefs[AQI_THRESHOLD] ?: 100
        )
    }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.trackingDataStore.edit { it[TRACKING_ENABLED] = enabled }
    }

    suspend fun setIntervalSeconds(seconds: Int) {
        context.trackingDataStore.edit { it[INTERVAL_SECONDS] = seconds }
    }

    suspend fun setUnits(units: String) {
        context.trackingDataStore.edit { it[UNITS] = units }
    }

    suspend fun setAqiThreshold(threshold: Int) {
        context.trackingDataStore.edit { it[AQI_THRESHOLD] = threshold }
    }

    companion object {
        private val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        private val INTERVAL_SECONDS = intPreferencesKey("interval_seconds")
        private val UNITS = stringPreferencesKey("units")
        private val AQI_THRESHOLD = intPreferencesKey("aqi_threshold")
    }
}
