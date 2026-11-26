package com.roadguide.app.map

import android.content.Context
import com.roadguide.app.map.model.MapPoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/** Helper object for rendering map elements (markers, polylines) using MapLibre SDK. */
object RenderHelper {

    /** Draws a route polyline on the map using native MapLibre Polyline. */
    fun addRoutePolyline(
        mapLibreMap: MapLibreMap,
        routePoints: List<MapPoint>,
        color: Int = -8947314,
        strokeWidth: Float = 6f
    ) {
        mapLibreMap.addRoutePolyline(routePoints, color, strokeWidth)
    }

    /** Draws a location marker (current position) on the map. */
    fun addLocationMarker(
        mapLibreMap: MapLibreMap,
        context: Context,
        latitude: Double,
        longitude: Double,
        iconRes: Int = com.roadguide.app.R.drawable.ic_launcher_background
    ) {
        mapLibreMap.addLocationMarker(context, latitude, longitude, iconRes)
    }

    /** Updates the camera to focus on a list of points. */
    fun focusCameraOnPoints(
        mapLibreMap: MapLibreMap,
        points: List<MapPoint>,
        padding: Int = 100
    ) {
        mapLibreMap.focusCameraOnPoints(points, padding)
    }
}
