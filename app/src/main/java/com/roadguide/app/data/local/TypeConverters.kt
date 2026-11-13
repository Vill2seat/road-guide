package com.roadguide.app.data.local

import androidx.room.TypeConverter
import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.entity.DataSource
import com.roadguide.app.data.local.entity.PlaceCategory
import com.roadguide.app.data.local.entity.PrivacyStatus
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import com.roadguide.app.data.local.entity.TransportType

class Converters {
    @TypeConverter fun fromRouteStatus(value: RouteStatus?): String? = value?.name
    @TypeConverter fun toRouteStatus(value: String?): RouteStatus? = value?.let { RouteStatus.valueOf(it) }

    @TypeConverter fun fromRouteType(value: RouteType?): String? = value?.name
    @TypeConverter fun toRouteType(value: String?): RouteType? = value?.let { RouteType.valueOf(it) }

    @TypeConverter fun fromActivityType(value: ActivityType?): String? = value?.name
    @TypeConverter fun toActivityType(value: String?): ActivityType? = value?.let { ActivityType.valueOf(it) }

    @TypeConverter fun fromTransportType(value: TransportType?): String? = value?.name
    @TypeConverter fun toTransportType(value: String?): TransportType? = value?.let { TransportType.valueOf(it) }

    @TypeConverter fun fromPrivacyStatus(value: PrivacyStatus?): String? = value?.name
    @TypeConverter fun toPrivacyStatus(value: String?): PrivacyStatus? = value?.let { PrivacyStatus.valueOf(it) }

    @TypeConverter fun fromPlaceCategory(value: PlaceCategory?): String? = value?.name
    @TypeConverter fun toPlaceCategory(value: String?): PlaceCategory? = value?.let { PlaceCategory.valueOf(it) }

    @TypeConverter fun fromDataSource(value: DataSource?): String? = value?.name
    @TypeConverter fun toDataSource(value: String?): DataSource? = value?.let { DataSource.valueOf(it) }

    @TypeConverter fun fromDataQuality(value: DataQuality?): String? = value?.name
    @TypeConverter fun toDataQuality(value: String?): DataQuality? = value?.let { DataQuality.valueOf(it) }
}
