package com.roadguide.app.map

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import com.roadguide.app.map.model.MapPoint

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    mapContentKey: Any? = null,
    onMapUpdate: (MapLibreMap) -> Unit = {},
    onMapReady: (MapLibreMap) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = context as? LifecycleOwner
    val lifecycle = lifecycleOwner?.lifecycle

    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }
    val mapViewState = remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mv = MapView(ctx).apply {
                onCreate(Bundle())
            }
            mapViewState.value = mv
            mv.getMapAsync(object : OnMapReadyCallback {
                override fun onMapReady(mapLibreMap: MapLibreMap) {
                    try {
                        mapLibreMap.setStyle("https://demotiles.maplibre.org/style.json")
                    } catch (_: Exception) { }
                    mapRef.value = mapLibreMap
                    try { onMapReady(mapLibreMap) } catch (_: Exception) { }
                }
            })
            mv
        }
    )

    DisposableEffect(lifecycle, mapViewState.value) {
        val mv = mapViewState.value
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { mv?.onStart() }
            override fun onResume(owner: LifecycleOwner) { mv?.onResume() }
            override fun onPause(owner: LifecycleOwner) { mv?.onPause() }
            override fun onStop(owner: LifecycleOwner) { mv?.onStop() }
            override fun onDestroy(owner: LifecycleOwner) { mv?.onDestroy() }
        }
        lifecycle?.addObserver(observer)
        mv?.onStart()
        mv?.onResume()
        onDispose {
            mv?.onPause()
            mv?.onStop()
            mv?.onDestroy()
            lifecycle?.removeObserver(observer)
        }
    }

    LaunchedEffect(mapRef.value, mapContentKey) {
        mapRef.value?.let { map ->
            try { onMapUpdate(map) } catch (_: Exception) { }
        }
    }
}

fun MapLibreMap.addLocationMarker(
    context: android.content.Context,
    latitude: Double,
    longitude: Double,
    iconRes: Int = com.roadguide.app.R.drawable.ic_launcher_background
) {
    val latLng = LatLng(latitude, longitude)
    getMarkers().filter { it.title == "location_marker" }.forEach { removeMarker(it) }
    try {
        val icon = IconFactory.getInstance(context).fromResource(iconRes)
        addMarker(MarkerOptions().position(latLng).icon(icon).title("location_marker"))
    } catch (_: Exception) { }
    moveCamera(CameraUpdateFactory.newLatLng(latLng))
}

fun MapLibreMap.removeLocationMarker() {
    getMarkers().filter { it.title == "location_marker" }.forEach { removeMarker(it) }
}

fun MapLibreMap.addRouteEndpointMarker(
    context: Context,
    title: String,
    latitude: Double,
    longitude: Double,
    iconRes: Int = com.roadguide.app.R.drawable.ic_launcher_background
) {
    getMarkers().filter { it.title == title }.forEach { removeMarker(it) }
    try {
        val icon = IconFactory.getInstance(context).fromResource(iconRes)
        addMarker(MarkerOptions().position(LatLng(latitude, longitude)).icon(icon).title(title))
    } catch (_: Exception) { }
}

fun MapLibreMap.addRoutePolyline(
    routePoints: List<MapPoint>,
    color: Int = -8947314,
    strokeWidth: Float = 6f
) {
    addRoutePolylines(listOf(routePoints), color, strokeWidth)
}

fun MapLibreMap.addRoutePolylines(
    routeLines: List<List<MapPoint>>,
    color: Int = -8947314,
    strokeWidth: Float = 6f
) {
    getPolylines().forEach { removePolyline(it) }
    routeLines.filter { it.size >= 2 }.forEach { routePoints ->
        val latLngs = routePoints.map { LatLng(it.latitude, it.longitude) }
        addPolyline(PolylineOptions().addAll(latLngs).color(color).width(strokeWidth))
    }
}

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
    try {
        val bounds = LatLngBounds.Builder()
            .include(LatLng(maxLat, maxLon))
            .include(LatLng(minLat, minLon))
            .build()
        animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding), 500, null)
    } catch (_: Exception) { }
}