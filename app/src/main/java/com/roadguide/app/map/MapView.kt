package com.roadguide.app.map

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.markers.Marker
import org.maplibre.android.markers.MarkerOptions
import org.maplibre.android.polyline.Polyline

/**
 * Holds a reference to the native MapLibre MapView instance so that
 * lifecycle callbacks can be forwarded from the Compose lifecycle.
 */
private class MapViewHolder(
    var view: org.maplibre.android.maps.MapView? = null
) {
    /**
     * Provides access to the MapLibreMap instance.
     */
    var mapLibreMap: MapLibreMap? = null

    /**
     * Adds a location marker to the map.
     */
    fun addLocationMarker(lat: Double, lng: Double, title: String = "Текущее положение"): Marker {
        val map = mapLibreMap ?: return Marker()
        return map.addMarker(
            MarkerOptions()
                .position(LatLng(lat, lng))
                .title(title)
        )
    }

    /**
     * Adds a route polyline to the map.
     */
    fun addRoutePolyline(points: List<MapPoint>, color: Int = 0xFF6FAF8E, width: Float = 6f): Polyline {
        val map = mapLibreMap ?: return Polyline()
        val latLngs = points.map { LatLng(it.latitude, it.longitude) }
        return map.addPolyline(
            PolylineOptions()
                .addAll(latLngs)
                .color(color)
                .width(width)
        )
    }
}

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

    val holder = remember { MapViewHolder() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mv = org.maplibre.android.maps.MapView(ctx).apply {
                onCreate(Bundle())
            }
            mv.getMapAsync(object : OnMapReadyCallback {
                override fun onMapReady(mapLibreMap: MapLibreMap) {
                    mapLibreMap.setStyle("https://demotiles.maplibre.org/style.json") {
                        holder.mapLibreMap = mapLibreMap
                        onMapReady(mapLibreMap)
                    }
                }
            })
            holder.view = mv
            mv
        }
    )

    DisposableEffect(lifecycle, holder) {
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                holder.view?.let { it.onStart() }
            }

            override fun onResume(owner: LifecycleOwner) {
                holder.view?.let { it.onResume() }
            }

            override fun onPause(owner: LifecycleOwner) {
                holder.view?.let { it.onPause() }
            }

            override fun onStop(owner: LifecycleOwner) {
                holder.view?.let { it.onStop() }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                holder.view?.let { it.onDestroy() }
            }
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
            holder.view?.let { it.onLowMemory() }
            holder.view?.let { it.onDestroy() }
        }
    }
}

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

/**
 * Data class representing a route line for MapLibre.
 */
data class RouteLineData(
    val points: List<MapPoint>,
    val color: Int = 0xFF6FAF8E,
    val strokeWidth: Float = 6f
)
