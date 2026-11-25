package com.roadguide.app.map

import android.content.Context
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.roadguide.app.map.model.MapPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView as NativeMapView
import org.maplibre.android.maps.Style

private const val MAP_STYLE_URI = "https://demotiles.maplibre.org/style.json"

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    mapContentKey: Any? = null,
    onMapUpdate: (MapLibreMap) -> Unit = {},
    onMapReady: (MapLibreMap) -> Unit = {}
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { mutableStateOf<NativeMapView?>(null) }
    val mapLibreMap = remember { mutableStateOf<MapLibreMap?>(null) }
    val isDisposed = remember { mutableStateOf(false) }
    val currentOnMapReady = rememberUpdatedState(onMapReady)
    val currentOnMapUpdate = rememberUpdatedState(onMapUpdate)

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { viewContext ->
            NativeMapView(viewContext).also { nativeView ->
                nativeView.onCreate(Bundle())
                mapView.value = nativeView
                nativeView.getMapAsync { map ->
                    if (isDisposed.value) return@getMapAsync
                    try {
                        map.setStyle(Style.Builder().fromUri(MAP_STYLE_URI)) {
                            if (isDisposed.value) return@setStyle
                            map.uiSettings.isAttributionEnabled = true
                            mapLibreMap.value = map
                            currentOnMapReady.value(map)
                        }
                    } catch (_: Exception) {
                        mapLibreMap.value = map
                        currentOnMapReady.value(map)
                    }
                }
            }
        },
        update = { }
    )

    val binding = remember {
        MapViewLifecycleBinding(
            mapView = mapView.value!!,
            lifecycle = lifecycle,
            isDisposed = isDisposed
        )
    }

    DisposableEffect(lifecycle) {
        lifecycle.addObserver(binding)
        onDispose {
            lifecycle.removeObserver(binding)
            binding.dispose()
        }
    }

    LaunchedEffect(mapLibreMap.value) {
        mapLibreMap.value?.let { map ->
            if (!isDisposed.value) {
                currentOnMapUpdate.value(map)
            }
        }
    }

    if (mapView.value == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Loading map…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private class MapViewLifecycleBinding(
    private val mapView: NativeMapView,
    private val lifecycle: Lifecycle,
    private val isDisposed: androidx.compose.runtime.MutableState<Boolean>
) : DefaultLifecycleObserver, ComponentCallbacks2 {
    private var isStarted = false
    private var isResumed = false

    init {
        lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        if (isDisposed.value || isStarted) return
        mapView.onStart()
        isStarted = true
    }

    override fun onResume(owner: LifecycleOwner) {
        if (isDisposed.value) return
        if (!isStarted) onStart(owner)
        if (!isResumed) {
            mapView.onResume()
            isResumed = true
        }
    }

    override fun onPause(owner: LifecycleOwner) = pauseIfNeeded()

    override fun onStop(owner: LifecycleOwner) {
        pauseIfNeeded()
        stopIfNeeded()
    }

    override fun onDestroy(owner: LifecycleOwner) = dispose()

    override fun onConfigurationChanged(newConfig: Configuration) = Unit

    override fun onLowMemory() {
        if (!isDisposed.value) mapView.onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        if (!isDisposed.value && level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            mapView.onLowMemory()
        }
    }

    fun dispose() {
        if (isDisposed.value) return
        isDisposed.value = true
        lifecycle.removeObserver(this)
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

fun MapLibreMap.focusCameraOnPoints(points: List<MapPoint>, padding: Int = 100) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(points.first().latitude, points.first().longitude),
                15.0
            ),
            500
        )
        return
    }
    val bounds = org.maplibre.android.geometry.LatLngBounds.Builder().apply {
        points.forEach { include(LatLng(it.latitude, it.longitude)) }
    }.build()
    animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding), 500)
}

fun MapLibreMap.addLocationMarker(
    context: Context,
    latitude: Double,
    longitude: Double,
    iconRes: Int = com.roadguide.app.R.drawable.ic_launcher_foreground
) {
    val latLng = LatLng(latitude, longitude)
    getMarkers().filter { it.title == "location_marker" }.forEach { removeMarker(it) }
    try {
        val icon = org.maplibre.android.annotations.IconFactory.getInstance(context)
            .fromResource(iconRes)
        addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(latLng)
                .icon(icon)
                .title("location_marker")
        )
    } catch (_: Exception) {
        addMarker(
            org.maplibre.android.annotations.MarkerOptions()
                .position(latLng)
                .title("location_marker")
        )
    }
}

fun MapLibreMap.removeLocationMarker() {
    getMarkers().filter { it.title == "location_marker" }.forEach { removeMarker(it) }
}

fun MapLibreMap.addRouteEndpointMarker(
    context: Context,
    title: String,
    latitude: Double,
    longitude: Double,
    iconRes: Int = com.roadguide.app.R.drawable.ic_launcher_foreground
) {
    getMarkers().filter { it.title == title }.forEach { removeMarker(it) }
    val icon = org.maplibre.android.annotations.IconFactory.getInstance(context)
        .fromResource(iconRes)
    addMarker(
        org.maplibre.android.annotations.MarkerOptions()
            .position(LatLng(latitude, longitude))
            .icon(icon)
            .title(title)
    )
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
        addPolyline(
            org.maplibre.android.annotations.PolylineOptions()
                .addAll(latLngs)
                .color(color)
                .width(strokeWidth)
        )
    }
}