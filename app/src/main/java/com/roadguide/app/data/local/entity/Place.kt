package com.roadguide.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PlaceCategory { HOME, WORK, FAVORITE, CUSTOM }

@Entity(
    tableName = "places",
    indices = [Index(value = ["category"]), Index(value = ["visitCount"])]
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: PlaceCategory = PlaceCategory.CUSTOM,
    val visitCount: Int = 0,
    val isPrivate: Boolean = false,
    val lastVisit: Long = 0
)
