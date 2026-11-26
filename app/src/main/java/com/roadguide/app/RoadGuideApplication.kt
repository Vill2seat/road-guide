package com.roadguide.app

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class RoadGuideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            MapLibre.getInstance(applicationContext)
        } catch (e: Exception) {
            Log.e("RoadGuideApp", "MapLibre initialization failed", e)
        }
    }
}
