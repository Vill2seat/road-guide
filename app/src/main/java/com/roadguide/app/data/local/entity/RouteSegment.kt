package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_segments",
    indices = [
        Index(value = ["routeId"]),
        Index(value = ["startTime"]),
        Index(value = ["activityType"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RouteSegment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val startTime: Long,
    val endTime: Long,
    val activityType: ActivityType = ActivityType.UNKNOWN,
    val transportType: TransportType? = null,
    val distanceMeters: Double = 0.0,
    val startLatitude: Double = 0.0,
    val startLongitude: Double = 0.0,
    val endLatitude: Double = 0.0,
    val endLongitude: Double = 0.0,
    val geometryReference: String? = null,
    val confidence: Double = 0.0,
    val isUserCorrected: Boolean = false,
    val correctedActivityType: ActivityType? = null,
    val correctedTransportType: TransportType? = null
)

enum class ActivityType { UNKNOWN, STATIONARY, WALKING, RUNNING, CYCLING, IN_VEHICLE, PUBLIC_TRANSPORT }
enum class TransportType { NONE, WALKING, CYCLING, CAR, MOTORCYCLE, BUS, TRAM, TRAIN, SUBWAY, FERRY, OTHER }
