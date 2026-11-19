package com.roadguide.app.map.model

/**
 * Data class representing a location marker on the map.
 * Wraps location + metadata for UI/composables.
 */
data class LocationMarker(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float = 0f,
    val speedMps: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val bearing: Float? = null,
    val provider: String = "fused",
    val isMock: Boolean = false
) {
    /** Create MapPoint for map rendering */
    fun toMapPoint(): MapPoint = MapPoint(latitude, longitude)
}

/**
 * Summary of location point for UI display.
 */
data class LocationPointSummary(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracyMeters: Float = 0f,
    val speedMps: Double = 0.0,
    val activityType: String = "unknown"
) {
    fun toMapPoint() = MapPoint(latitude, longitude)
}
