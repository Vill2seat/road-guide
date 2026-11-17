package com.roadguide.app.mapper

import com.roadguide.app.domain.model.RouteSummary
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import java.time.Instant
import java.time.ZoneId

/**
 * Mapper for converting Room entity to domain model.
 * No Room annotations in domain layer.
 */
object RouteMapper {

    fun toSummary(entity: Route): RouteSummary {
        val date = Instant.ofEpochMilli(entity.startTime)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        val routeType = when (entity.routeType) {
            RouteType.DAILY_AUTO -> "daily_auto"
            RouteType.MANUAL_TRACKING -> "manual_tracking"
            RouteType.PLANNED_ROUTE -> "planned_route"
        }

        return RouteSummary(
            id = entity.id,
            startTime = entity.startTime,
            endTime = entity.endTime,
            distanceMeters = entity.distanceMeters,
            averageAqi = null,
            durationSeconds = entity.totalDurationSeconds,
            status = entity.status.name.lowercase(),
            routeType = routeType
        )
    }
}
