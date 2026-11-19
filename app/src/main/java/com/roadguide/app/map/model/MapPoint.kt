package com.roadguide.app.map.model

/** Data class representing a map point (latitude, longitude).
 *
 * Used as a common model across all map-related components:
 * - route points
 * - location markers
 * - geocoding results
 */
data class MapPoint(
    val latitude: Double,
    val longitude: Double
)
