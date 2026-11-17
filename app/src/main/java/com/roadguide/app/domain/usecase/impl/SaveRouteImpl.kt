package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.domain.model.RouteSummary
import com.roadguide.app.domain.usecase.SaveRoute
import com.roadguide.app.data.repository.RouteRepository

/**
 * Use case implementation for saving a route summary.
 * Wires domain contract to RouteRepository.
 */
class SaveRouteImpl(private val routeRepository: RouteRepository) : SaveRoute {

    override suspend fun invoke(summary: RouteSummary): Long {
        val route = com.roadguide.app.data.local.entity.Route(
            id = 0,
            startTime = summary.startTime,
            endTime = summary.endTime,
            distanceMeters = summary.distanceMeters,
            status = com.roadguide.app.data.local.entity.RouteStatus.COMPLETED,
            routeType = com.roadguide.app.data.local.entity.RouteType.DAILY_AUTO,
            confidence = summary.averageAqi ?: 0.0,
            date = summary.endTime?.let {
                java.time.Instant.ofEpochMilli(it)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()
            } ?: java.time.Instant.now()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
                .toString(),
            totalDurationSeconds = summary.durationSeconds,
            calculationVersion = 1
        )
        return routeRepository.insert(route)
    }
}
