package com.roadguide.app.map

import android.content.Context
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView as MapLibreAndroidMapView
import org.maplibre.android.maps.OnMapReadyCallback
import com.roadguide.app.map.model.MapPoint

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    mapContentKey: Any? = null,
    onMapUpdate: (MapLibreMap) -> Unit = {},
    onMapReady: (MapLibreMap) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }
    val lifecycleBinding = remember { mutableStateOf<MapViewLifecycleBinding?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mv = MapLibreAndroidMapView(ctx).apply {
                onCreate(Bundle())
            }
            lifecycleBinding.value = MapViewLifecycleBinding(
                mapView = mv,
                lifecycle = lifecycleOwner.lifecycle,
                context = ctx
            )
            mv.getMapAsync(object : OnMapReadyCallback {
                override fun onMapReady(mapLibreMap: MapLibreMap) {
                    mapLibreMap.setStyle(STYLE_URL) {
                        mapRef.value = mapLibreMap
                        onMapReady(mapLibreMap)
                    }
                }
            })
            mv
        },
        onRelease = {
            lifecycleBinding.value?.dispose()
            lifecycleBinding.value = null
            mapRef.value = null
        }
    )

    LaunchedEffect(mapRef.value, mapContentKey) {
        mapRef.value?.let(onMapUpdate)
    }
}

private const val STYLE_URL = "https://demotiles.maplibre.org/style.json"

private class MapViewLifecycleBinding(
    private val mapView: MapLibreAndroidMapView,
    private val lifecycle: Lifecycle,
    private val context: Context
) : DefaultLifecycleObserver, ComponentCallbacks2 {
    private var isStarted = false
    private var isResumed = false
    private var isDisposed = false

    init {
        context.registerComponentCallbacks(this)
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isDisposed || isStarted) return
        mapView.onStart()
        isStarted = true
    }

    override fun onResume(owner: LifecycleOwner) {
        if (isDisposed) return
        if (!isStarted) onStart(owner)
        if (!isResumed) {
            mapView.onResume()
            isResumed = true
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        pauseIfNeeded()
    }

    override fun onStop(owner: LifecycleOwner) {
        pauseIfNeeded()
        stopIfNeeded()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        dispose()
    }

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    override fun onLowMemory() {
        if (!isDisposed) mapView.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        if (!isDisposed && level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            mapView.onLowMemory()
        }
    }

    fun dispose() {
        if (isDisposed) return
        isDisposed = true
        lifecycle.removeObserver(this)
        context.unregisterComponentCallbacks(this)
        pauseIfNeeded()
        stopIfNeeded()
        mapView.onDestroy()
    }

    private fun pauseIfNeeded() {
        if (isResumed) {
            mapView.onPause()
            isResumed = false
        }
    }

    private fun stopIfNeeded() {
        if (isStarted) {
            mapView.onStop()
            isStarted = false
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