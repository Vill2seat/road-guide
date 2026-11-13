package com.roadguide.app.data.local

import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import com.roadguide.app.data.local.entity.PlaceCategory
import com.roadguide.app.data.local.entity.PrivacyZoneType
import androidx.room.TypeConverter

object TypeConverters {

    @TypeConverter
    fun fromRouteStatus(value: RouteStatus): String =
        value.name

    @TypeConverter
    fun toRouteStatus(name: String): RouteStatus =
        RouteStatus.valueOf(name)

    @TypeConverter
    fun fromRouteType(value: RouteType): String =
        value.name

    @TypeConverter
    fun toRouteType(name: String): RouteType =
        RouteType.valueOf(name)

    @TypeConverter
    fun fromPlaceCategory(value: PlaceCategory): String =
        value.name

    @TypeConverter
    fun toPlaceCategory(name: String): PlaceCategory =
        PlaceCategory.valueOf(name)

    @TypeConverter
    fun fromPrivacyZoneType(value: PrivacyZoneType): String =
        value.name

    @TypeConverter
    fun toPrivacyZoneType(name: String): PrivacyZoneType =
        PrivacyZoneType.valueOf(name)

    @TypeConverter
    fun fromBoolean(value: Boolean): Int =
        if (value) 1 else 0

    @TypeConverter
    fun toBoolean(value: Int): Boolean =
        value == 1
}
