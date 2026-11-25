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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.roadguide.app.map.model.MapPoint
import org.maplibre.android.MapLibre
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
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { mutableStateOf<NativeMapView?>(null) }
    val mapLibreMap = remember { mutableStateOf<MapLibreMap?>(null) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { viewContext ->
            MapLibre.getInstance(viewContext)
            NativeMapView(viewContext).also { nativeView ->
                nativeView.onCreate(Bundle())
                nativeView.getMapAsync { map ->
                    map.setStyle(Style.Builder().fromUri(MAP_STYLE_URI)) {
                        map.uiSettings.isAttributionEnabled = true
                        mapLibreMap.value = map
                        onMapReady(map)
                    }
                }
                mapView.value = nativeView
            }
        }
    )

    DisposableEffect(mapView.value, lifecycle) {
        val nativeView = mapView.value
        if (nativeView == null) {
            onDispose { }
        } else {
            when {
                lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> {
                    nativeView.onStart()
                    nativeView.onResume()
                }
                lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) -> nativeView.onStart()
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> nativeView.onStart()
                    Lifecycle.Event.ON_RESUME -> nativeView.onResume()
                    Lifecycle.Event.ON_PAUSE -> nativeView.onPause()
                    Lifecycle.Event.ON_STOP -> nativeView.onStop()
                    Lifecycle.Event.ON_DESTROY -> nativeView.onDestroy()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    nativeView.onPause()
                }
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    nativeView.onStop()
                }
                nativeView.onDestroy()
            }
        }
    }

    LaunchedEffect(mapContentKey, mapLibreMap.value) {
        mapLibreMap.value?.let(onMapUpdate)
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
    iconRes: Int = com.roadguide.app.R.drawable.ic_launcher_background
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
    iconRes: Int = com.roadguide.app.R.drawable.ic_launcher_background
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
