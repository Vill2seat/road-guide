package com.roadguide.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class RoadGuideApplication : Application() {
    private lateinit var mapLibreInitialized: Boolean

    override fun onCreate() {
        super.onCreate()
        try {
            MapLibre.getInstance(applicationContext)
            mapLibreInitialized = true
        } catch (e: Exception) {
            Log.e("RoadGuideApp", "MapLibre initialization failed", e)
            mapLibreInitialized = false
        }
    }

    /**
     * Returns whether MapLibre initialized successfully.
     * Use this to check before using MapLibre APIs to provide clear error messages to users.
     */
    fun isMapLibreInitialized(): Boolean = mapLibreInitialized
}
