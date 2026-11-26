package com.roadguide.app.map

import android.content.Context
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.roadguide.app.map.model.MapPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView as NativeMapView
import org.maplibre.android.maps.Style

private const val MAP_STYLE_URI = "https://demotiles.maplibre.org/style.json"
private const val MAP_STYLE_ASSET = "file:///android_asset/map_style.json"

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    mapContentKey: Any? = null,
    onMapUpdate: (MapLibreMap) -> Unit = {},
    onMapReady: (MapLibreMap) -> Unit = {},
    onMapError: (Throwable) -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { mutableStateOf<NativeMapView?>(null) }
    val mapLibreMap = remember { mutableStateOf<MapLibreMap?>(null) }
    val isDisposed = remember { mutableStateOf(false) }
    val mapError = remember { mutableStateOf<Throwable?>(null) }
    val currentOnMapReady = rememberUpdatedState(onMapReady)
    val currentOnMapUpdate = rememberUpdatedState(onMapUpdate)
    val currentOnMapError = rememberUpdatedState(onMapError)

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { viewContext: Context ->
            var nativeView: NativeMapView? = null
            try {
                nativeView = NativeMapView(viewContext).also { nv ->
                    nv.onCreate(Bundle())
                    mapView.value = nv
                    nv.getMapAsync { map: MapLibreMap ->
                        if (isDisposed.value) return@getMapAsync
                        loadStyleWithFallback(map, viewContext) { styleMap: MapLibreMap, loadException: Throwable? ->
                            if (isDisposed.value) return@loadStyleWithFallback
                            if (loadException != null) {
                                mapError.value = loadException
                                currentOnMapError.value(loadException)
                            }
                            mapLibreMap.value = styleMap
                            try {
                                styleMap.uiSettings.isAttributionEnabled = true
                            } catch (_: Exception) {
                            }
                            currentOnMapReady.value(styleMap)
                        }
                    }
                }
            } catch (e: Exception) {
                mapError.value = e
                currentOnMapError.value(e)
            }
            nativeView as android.view.View
        },
        update = { }
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val view = mapView.value
            if (view == null || isDisposed.value) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> view.onStart()
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                Lifecycle.Event.ON_STOP -> view.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    isDisposed.value = true
                    view.onDestroy()
                    mapView.value = null
                    mapLibreMap.value = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val view = mapView.value
            if (view != null && !isDisposed.value) {
                isDisposed.value = true
                view.onDestroy()
            }
            mapView.value = null
            mapLibreMap.value = null
        }
    }

    LaunchedEffect(mapLibreMap.value, mapContentKey) {
        val map = mapLibreMap.value
        if (map != null && !isDisposed.value) {
            try {
                currentOnMapUpdate.value(map)
            } catch (_: Exception) {
            }
        }
    }

    if (mapView.value == null && mapError.value == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Loading map…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else if (mapError.value != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Map unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun loadStyleWithFallback(
    map: MapLibreMap,
    context: Context,
    callback: (MapLibreMap, Throwable?) -> Unit
) {
    try {
        map.setStyle(Style.Builder().fromUri(MAP_STYLE_URI)) { _: Style ->
            callback(map, null)
        }
    } catch (e: Exception) {
        try {
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_ASSET)) { _: Style ->
                callback(map, null)
            }
        } catch (assetException: Exception) {
            callback(map, assetException)
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
