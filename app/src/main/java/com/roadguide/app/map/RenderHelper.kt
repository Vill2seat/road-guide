package com.roadguide.app.map

import com.roadguide.app.map.model.MapPoint

/**
 * RenderHelper provides utilities for rendering map elements
 * that require access to the MapLibreMap instance.
 *
 * In a real app, these functions would be called from MapView
 * or from ViewModels that have access to the map.
 */
object RenderHelper {

    /**
     * Renders a route polyline on the map.
     * Called from MapView or ViewModels with access to MapLibreMap.
     */
    fun renderRoutePolyline(
        mapLibreMap: org.maplibre.android.maps.MapLibreMap?,
        routePoints: List<MapPoint>,
        color: Int = 0xFF6FAF8E,
        width: Float = 6f
    ): org.maplibre.android.polyline.Polyline {
        return mapLibreMap?.addPolyline(
            org.maplibre.android.polyline.PolylineOptions()
                .addAll(
                    routePoints.map { org.maplibre.android.geometry.LatLng(it.latitude, it.longitude) }
                )
                .color(color)
                .width(width)
        ) ?: org.maplibre.android.polyline.Polyline()
    }

    /**
     * Renders a location marker on the map.
     */
    fun renderLocationMarker(
        mapLibreMap: org.maplibre.android.maps.MapLibreMap?,
        lat: Double,
        lng: Double,
        title: String = "Текущее положение"
    ): org.maplibre.android.markers.Marker {
        return mapLibreMap?.addMarker(
            org.maplibre.android.markers.MarkerOptions()
                .position(org.maplibre.android.geometry.LatLng(lat, lng))
                .title(title)
        ) ?: org.maplibre.android.markers.Marker()
    }
}
