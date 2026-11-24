package com.roadguide.app.data.local.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.trackingDataStore by preferencesDataStore(name = "tracking_prefs")

data class TrackingPreferences(
    val trackingEnabled: Boolean = false,
    val trackingPaused: Boolean = false,
    val intervalSeconds: Int = 60,
    val units: String = "metric",
    val aqiThreshold: Int = 100
)

class TrackingPrefs(private val context: Context) {

    private var internetCache: Boolean? = null
    private var internetCacheTs: Long = 0
    private val internetCacheTtlMs = 60_000L // 1 минута

    val preferencesFlow: Flow<TrackingPreferences> = context.trackingDataStore.data.map { prefs ->
        TrackingPreferences(
            trackingEnabled = prefs[TRACKING_ENABLED] ?: false,
            trackingPaused = prefs[TRACKING_PAUSED] ?: false,
            intervalSeconds = prefs[INTERVAL_SECONDS] ?: 60,
            units = prefs[UNITS] ?: "metric",
            aqiThreshold = prefs[AQI_THRESHOLD] ?: 100
        )
    }

    suspend fun setTrackingEnabled(enabled: Boolean) {
        context.trackingDataStore.edit {
            it[TRACKING_ENABLED] = enabled
            if (!enabled) it[TRACKING_PAUSED] = false
        }
    }

    suspend fun setTrackingPaused(paused: Boolean) {
        context.trackingDataStore.edit { it[TRACKING_PAUSED] = paused }
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

    /**
     * Marks that internet was available today.
     * Resets the flag if the stored date is different from today.
     */
    suspend fun markInternetAvailable() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        context.trackingDataStore.edit { prefs ->
            val storedDate = prefs[INTERNET_AVAILABLE_DATE]
            if (storedDate != today) {
                prefs[INTERNET_AVAILABLE_DATE] = today
                prefs[INTERNET_AVAILABLE_TODAY] = true
            } else {
                prefs[INTERNET_AVAILABLE_TODAY] = true
            }
        }
        internetCache = true
        internetCacheTs = System.currentTimeMillis()
    }

    /**
     * Returns true if internet was available at least once today.
     * Resets the flag if the stored date is different from today.
     * Cached in memory for 1 minute to avoid repeated DataStore reads.
     */
    suspend fun wasInternetAvailableToday(): Boolean {
        val now = System.currentTimeMillis()
        if (internetCache != null && now - internetCacheTs < internetCacheTtlMs) {
            return internetCache!!
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val result = context.trackingDataStore.data.first().let { prefs ->
            val storedDate = prefs[INTERNET_AVAILABLE_DATE]
            val available = prefs[INTERNET_AVAILABLE_TODAY] ?: false
            if (storedDate != today) {
                false
            } else {
                available
            }
        }

        internetCache = result
        internetCacheTs = now
        return result
    }

    companion object {
        private val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        private val TRACKING_PAUSED = booleanPreferencesKey("tracking_paused")
        private val INTERVAL_SECONDS = intPreferencesKey("interval_seconds")
        private val UNITS = stringPreferencesKey("units")
        private val AQI_THRESHOLD = intPreferencesKey("aqi_threshold")
        private val INTERNET_AVAILABLE_TODAY = booleanPreferencesKey("internet_available_today")
        private val INTERNET_AVAILABLE_DATE = stringPreferencesKey("internet_available_date")
    }
}
