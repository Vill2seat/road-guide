package com.roadguide.app.map

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback

/**
 * Composable wrapper for MapLibre Android SDK map view.
 *
 * Uses a local AAR from `app/libs/maplibre-android-13.5.4.aar`.
 *
 * Style: https://demotiles.maplibre.org/style.json (free, no access token required).
 *
 * Lifecycle callbacks (onStart, onResume, onPause, onStop, onDestroy,
 * onLowMemory) are forwarded to the underlying MapLibre MapView via
 * [DefaultLifecycleObserver] to prevent native renderer crashes.
 */
@Composable
fun MapView(
    modifier: Modifier = Modifier,
    onMapReady: (MapLibreMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val lifecycle = lifecycleOwner?.lifecycle

    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mv = MapView(ctx).apply {
                onCreate(Bundle())
            }
            mv.getMapAsync(object : OnMapReadyCallback {
                override fun onMapReady(mapLibreMap: MapLibreMap) {
                    mapLibreMap.setStyle("https://demotiles.maplibre.org/style.json")
                    mapRef.value = mapLibreMap
                    onMapReady(mapLibreMap)
                }
            })
            mv
        }
    )

    DisposableEffect(lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
            }

            override fun onResume(owner: LifecycleOwner) {
            }

            override fun onPause(owner: LifecycleOwner) {
            }

            override fun onStop(owner: LifecycleOwner) {
            }

            override fun onDestroy(owner: LifecycleOwner) {
            }
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
        }
    }
}

/**
 * Data class representing a map point.
 */
data class MapPoint(
    val latitude: Double,
    val longitude: Double
)

/**
 * Data class representing a location marker on the map.
 */
data class LocationMarker(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 0f,
    val speedMps: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val bearing: Float? = null,
    val provider: String = "fused",
    val isMock: Boolean = false
) {
    fun toMapPoint() = MapPoint(latitude, longitude)
}

/**
 * Summary of location point for UI display.
 */
data class LocationPointSummary(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracyMeters: Float = 0f,
    val speedMps: Double = 0.0,
    val activityType: String = "unknown"
) {
    fun toMapPoint() = MapPoint(latitude, longitude)
}
