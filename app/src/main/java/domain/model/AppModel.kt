package com.roadguide.app.domain.model

data class TrackingSettingsState(
    val trackingEnabled: Boolean = false,
    val intervalSeconds: Int = 60,
    val units: String = "metric",
    val aqiThreshold: Int = 100
)

data class RouteSummary(
    val id: Long,
    val startTime: Long,
    val endTime: Long?,
    val distanceMeters: Double,
    val averageAqi: Double?,
    val durationSeconds: Long,
    val status: String = "completed",
    val routeType: RouteTab? = null
)

data class LocationPointSummary(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val activityType: String = "unknown",
    val speedMps: Double = 0.0,
    val altitudeMeters: Double = 0.0
)
