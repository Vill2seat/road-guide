package com.roadguide.app

import android.app.Application
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class RoadGuideApplication : Application() {
    var mapLibreInitialized: Boolean = false

    override fun onCreate() {
        super.onCreate()
        try {
            MapLibre.getInstance(applicationContext)
            mapLibreInitialized = true
        } catch (e: Throwable) {
            Log.e("RoadGuideApp", "MapLibre initialization failed", e)
            mapLibreInitialized = false
            Toast.makeText(applicationContext, "Map loading failed", Toast.LENGTH_LONG).show()
        }
    }

    /** Returns whether MapLibre initialized successfully.
     * Use this to check before using MapLibre APIs to provide clear error messages to users.
     */
    fun isMapLibreInitialized(): Boolean = mapLibreInitialized
}
