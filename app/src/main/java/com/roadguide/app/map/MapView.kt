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
import android.graphics.Color
import android.widget.TextView
import android.view.Gravity
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
    // MapLibre отключён для проверки работоспособности без карты.
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Карта отключена (MapLibre)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    } catch (e: Throwable) {
        try {
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_ASSET)) { _: Style ->
                callback(map, null)
            }
        } catch (assetException: Throwable) {
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
    } catch (_: Throwable) {
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
