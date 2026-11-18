package com.roadguide.app.map

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback

/**
 * Composable wrapper for MapLibre Android SDK map view.
 *
 * Uses a local AAR from `app/libs/maplibre-android-13.5.4.aar`
 * (maven.maplibre.org is DNS-unreachable in this environment).
 *
 * Style: https://demotiles.maplibre.org/style.json (free, no access token required).
 */
@Composable
fun MapView(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val mapView = MapView(context).apply {
                onCreate(Bundle())
            }
            mapView.getMapAsync(object : OnMapReadyCallback {
                override fun onMapReady(mapLibreMap: MapLibreMap) {
                    mapLibreMap.setStyle("https://demotiles.maplibre.org/style.json")
                }
            })
            mapView
        }
    )
}
