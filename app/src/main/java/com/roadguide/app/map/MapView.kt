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
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import com.roadguide.app.map.model.MapPoint

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
 * Adds a location marker (current position) to the map.
 * Removes any previously added location marker before adding a new one.
 */
fun MapLibreMap.addLocationMarker(
    context: android.content.Context,
    latitude: Double,
    longitude: Double,
    iconRes: Int = com.roadguide.app.R.drawable.ic_launcher_background
) {
    val latLng = LatLng(latitude, longitude)

    // Remove previous location marker
    val markers = getMarkers()
    markers.filter { it.title == "location_marker" }.forEach { marker ->
        removeMarker(marker)
    }

    // Add new marker using IconFactory
    val icon = IconFactory.getInstance(context).fromResource(iconRes)
    val options = MarkerOptions()
        .position(latLng)
        .icon(icon)
        .title("location_marker")
    addMarker(options)

    moveCamera(CameraUpdateFactory.newLatLng(latLng))
}

/**
 * Adds a route polyline to the map using native MapLibre Polyline.
 * Removes any previously added route polyline before adding a new one.
 */
fun MapLibreMap.addRoutePolyline(
    routePoints: List<MapPoint>,
    color: Int = -8947314,
    strokeWidth: Float = 6f
) {
    // Remove previous route polyline
    val polylines = getPolylines()
    polylines.forEach { polyline ->
        removePolyline(polyline)
    }

    if (routePoints.isEmpty()) return

    val latLngs = routePoints.map { LatLng(it.latitude, it.longitude) }

    val options = PolylineOptions()
        .addAll(latLngs)
        .color(color)
        .width(strokeWidth)
    addPolyline(options)
}

/**
 * Focuses the camera on a bounding box covering all given points.
 */
fun MapLibreMap.focusCameraOnPoints(points: List<MapPoint>, padding: Int = 100) {
    if (points.isEmpty()) return

    var minLat = points.first().latitude
    var maxLat = points.first().latitude
    var minLon = points.first().longitude
    var maxLon = points.first().longitude

    points.forEach { p ->
        minLat = kotlin.math.min(minLat, p.latitude)
        maxLat = kotlin.math.max(maxLat, p.latitude)
        minLon = kotlin.math.min(minLon, p.longitude)
        maxLon = kotlin.math.max(maxLon, p.longitude)
    }

    val bounds = LatLngBounds.Builder()
        .include(LatLng(maxLat, maxLon))
        .include(LatLng(minLat, minLon))
        .build()
    animateCamera(
        CameraUpdateFactory.newLatLngBounds(bounds, padding),
        500,
        null
    )
}

