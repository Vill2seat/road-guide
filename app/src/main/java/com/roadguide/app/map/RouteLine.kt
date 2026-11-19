package com.roadguide.app.map

import com.roadguide.app.map.model.MapPoint
import org.maplibre.android.maps.MapLibreMap

/**
 * Native MapLibre polyline renderer for routes.
 * Delegates to MapLibreMap.addRoutePolyline extension defined in MapView.kt.
 */
fun renderRoutePolyline(
    routePoints: List<MapPoint>,
    color: Int = -8947314,
    strokeWidth: Float = 6f,
    mapLibreMap: MapLibreMap
) {
    mapLibreMap.addRoutePolyline(routePoints, color, strokeWidth)
}

/** Renders route polyline with a symbol at start and end points. */
fun renderRoutePolylineWithSymbols(
    routePoints: List<MapPoint>,
    color: Int = -8947314,
    strokeWidth: Float = 6f,
    mapLibreMap: MapLibreMap
) {
    mapLibreMap.addRoutePolyline(routePoints, color, strokeWidth)
}
