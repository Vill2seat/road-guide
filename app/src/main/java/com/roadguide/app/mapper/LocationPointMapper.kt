package com.roadguide.app.mapper

import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.data.local.entity.LocationPoint

/**
 * Mapper for converting Room entity to domain model.
 * No Room annotations in domain layer.
 */
object LocationPointMapper {

    fun toSummary(entity: LocationPoint): LocationPointSummary {
        return LocationPointSummary(
            id = entity.id,
            latitude = entity.latitude,
            longitude = entity.longitude,
            timestamp = entity.timestamp,
            activityType = "walking",
            speedMps = entity.speedMetersPerSecond?.toDouble() ?: 0.0,
            altitudeMeters = entity.altitude ?: 0.0
        )
    }
}
