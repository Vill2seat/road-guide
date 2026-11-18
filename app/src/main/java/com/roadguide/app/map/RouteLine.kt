package com.roadguide.app.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.roadguide.app.map.model.MapPoint
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.markers.Marker
import org.maplibre.android.markers.MarkerOptions
import org.maplibre.android.polyline.Polyline

/**
 * RouteLine now uses MapLibre SDK Polyline for proper geographic rendering.
 * The line moves with map pan/zoom because it's attached to MapLibre coordinates,
 * unlike Compose Canvas which uses screen pixel coordinates.
 */
@Composable
fun RouteLine(
    routePoints: List<MapPoint>,
    modifier: Modifier = Modifier,
    color: Int = 0xFF6FAF8E,
    strokeWidth: Float = 6f
) {
    LaunchedEffect(routePoints) {
        routePoints.firstOrNull()?.let { point ->
            // Build path from route points (for future progress animation)
        }
    }

    // RouteLine is rendered via MapPolyline on the MapView side
    // No Canvas needed - the line will pan/zoom correctly
}

/**
 * Centralized MapPolyline renderer.
 * Called from MapView or composables that have access to MapLibreMap.
 */
fun renderRoutePolyline(
    mapLibreMap: org.maplibre.android.maps.MapLibreMap?,
    routePoints: List<MapPoint>,
    color: Int = 0xFF6FAF8E,
    width: Float = 6f
): Polyline {
    val map = mapLibreMap ?: return Polyline()
    val latLngs = routePoints.map { LatLng(it.latitude, it.longitude) }
    return map.addPolyline(
        org.maplibre.android.polyline.PolylineOptions()
            .addAll(latLngs)
            .color(color)
            .width(width)
    )
}

fun renderLocationMarker(
    mapLibreMap: org.maplibre.android.maps.MapLibreMap?,
    lat: Double,
    lng: Double,
    title: String = "Текущее положение"
): Marker {
    val map = mapLibreMap ?: return Marker()
    return map.addMarker(
        MarkerOptions()
            .position(LatLng(lat, lng))
            .title(title)
    )
}
