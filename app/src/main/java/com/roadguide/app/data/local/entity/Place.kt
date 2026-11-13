package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "places",
    indices = [Index(value = ["category"]), Index(value = ["isPrivate"])]
)
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: PlaceCategory = PlaceCategory.OTHER,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double = 100.0,
    val address: String? = null,
    val isPrivate: Boolean = false,
    val userLabel: String? = null,
    val visitCount: Int = 0,
    val totalDurationSeconds: Long = 0,
    val firstVisitTime: Long = 0,
    val lastVisitTime: Long = 0,
    val environmentalStats: String? = null
)

enum class PlaceCategory { HOME, WORK, SCHOOL, GYM, PARK, SHOP, RESTAURANT, TRANSIT, OTHER }
