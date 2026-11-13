package com.roadguide.app.data.local.settings

import androidx.datastore.preferences.core.BooleanPref
import androidx.datastore.preferences.core.Pref
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFactory
import androidx.datastore.rxjava3.
import com.roadguide.app.domain.model.ExposureConstants
import io.reactivex.rxjava3.core.Observable

object TrackingPrefs {

    const val TRACKING_ENABLED = "tracking_enabled"
    const val INTERVAL_SECONDS = "interval_seconds"
    const val UNITS = "units"
    const val AQI_THRESHOLD = "aqi_threshold"

    class TrackingEnabled : BooleanPref(DEFAULT = true)
    class IntervalSeconds : IntPref(DEFAULT = 60)
    class Units : StringPref(DEFAULT = "metric")
        override val values = listOf("metric", "imperial")
    class AqiThreshold : IntPref(DEFAULT = 100)

    object Factory : PreferencesFactory {
        override fun create(preferences: Preferences): TrackingPrefs = TrackingPrefs(
            TRACKING_ENABLED to preferences.getOrElse(TRACKING_ENABLED) { TrackingEnabled.defaultValue() },
            INTERVAL_SECONDS to preferences.getOrElse(INTERVAL_SECONDS) { IntervalSeconds.defaultValue() },
            UNITS to preferences.getOrElse(UNITS) { Units.defaultValue() },
            AQI_THRESHOLD to preferences.getOrElse(AQI_THRESHOLD) { AqiThreshold.defaultValue() }
        )
    }

    data class Defaults(
        val trackingEnabled: Boolean = true,
        val intervalSeconds: Int = 60,
        val units: String = "metric",
        val aqiThreshold: Int = 100
    )

    val defaultObservables: Observable<TrackingPrefs>
        = Observable.just(TrackingPrefs(
            TRACKING_ENABLED to true,
            INTERVAL_SECONDS to 60,
            UNITS to "metric",
            AQI_THRESHOLD to 100
        ))
}
