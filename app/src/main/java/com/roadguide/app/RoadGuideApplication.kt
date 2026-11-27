package com.roadguide.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RoadGuideApplication : Application() {
    var mapLibreInitialized: Boolean = false

    override fun onCreate() {
        super.onCreate()
    }

    fun isMapLibreInitialized(): Boolean = mapLibreInitialized
}
