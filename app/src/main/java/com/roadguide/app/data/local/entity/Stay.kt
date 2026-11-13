package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stays",
    indices = [
        Index(value = ["routeId"]),
        Index(value = ["startTime"]),
        Index(value = ["placeId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Route::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Place::class,
            parentColumns = ["id"],
            childColumns = ["placeId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Stay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val startTime: Long,
    val endTime: Long,
    val centerLatitude: Double = 0.0,
    val centerLongitude: Double = 0.0,
    val radiusMeters: Double = 0.0,
    val placeId: Long? = null,
    val confidence: Double = 0.0,
    val locationLabel: String? = null,
    val privacyStatus: PrivacyStatus = PrivacyStatus.PUBLIC,
    val isIndoor: Boolean = false,
    val indoorConfidence: Double = 0.0
)

enum class PrivacyStatus { PUBLIC, PRIVATE, EXCLUDED }
