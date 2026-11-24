package com.roadguide.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre
import org.maplibre.android.RenderingEngine
import org.maplibre.android.WellKnownTileServer

@HiltAndroidApp
class RoadGuideApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(
            this,
            null,
            WellKnownTileServer.MapLibre,
            RenderingEngine.Type.OPENGL
        )
    }
}
