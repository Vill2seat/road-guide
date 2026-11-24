package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.domain.model.RouteSummary
import com.roadguide.app.domain.usecase.GetLocationPoints
import com.roadguide.app.domain.usecase.GetRouteSummaries
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.PrivacyZoneRepository
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.data.repository.RouteSegmentRepository
import com.roadguide.app.mapper.LocationPointMapper
import com.roadguide.app.mapper.RouteMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Use case implementation for retrieving location point summaries.
 * Wires domain contract to LocationPointRepository.
 */
class GetLocationPointsImpl(
    private val locationPointRepository: LocationPointRepository,
    private val locationPointMapper: LocationPointMapper,
    private val privacyZoneRepository: PrivacyZoneRepository
) : GetLocationPoints {

    override fun invoke(): Flow<List<LocationPointSummary>> = combine(
        locationPointRepository.observeAll(),
        privacyZoneRepository.observeActiveZones()
    ) { points, activeZones ->
        PrivacyZonePolicy.filterPoints(points, activeZones, PrivacyScope.HISTORY)
            .map(locationPointMapper::toSummary)
    }
}

/**
 * Use case implementation for retrieving route summaries.
 * Wires domain contract to RouteRepository.
 */
class GetRouteSummariesImpl(
    private val routeRepository: RouteRepository,
    private val routeMapper: RouteMapper,
    private val locationPointRepository: LocationPointRepository,
    private val routeSegmentRepository: RouteSegmentRepository,
    private val privacyZoneRepository: PrivacyZoneRepository
) : GetRouteSummaries {

    override fun invoke(): Flow<List<RouteSummary>> = combine(
        routeRepository.observeAll(),
        locationPointRepository.observeAll(),
        routeSegmentRepository.observeAll(),
        privacyZoneRepository.observeActiveZones()
    ) { routes, points, segments, activeZones ->
        val hasHistoryPrivacy = activeZones.any { it.isActive && it.excludeFromHistory }
        routes.mapNotNull { route ->
            val routePoints = points
                .filter { it.routeId == route.id }
                .sortedBy { it.timestamp }
            val visiblePoints = PrivacyZonePolicy.filterPoints(
                routePoints,
                activeZones,
                PrivacyScope.HISTORY
            )
            val routeSegments = segments.filter { it.routeId == route.id }
            val visibleSegments = routeSegments.filterNot { segment ->
                PrivacyZonePolicy.excludesSegment(
                    segment,
                    routePoints,
                    activeZones,
                    PrivacyScope.HISTORY
                )
            }

            if (
                hasHistoryPrivacy &&
                visiblePoints.isEmpty() &&
                visibleSegments.isEmpty() &&
                (routePoints.isNotEmpty() || routeSegments.isNotEmpty())
            ) {
                return@mapNotNull null
            }
            if (routePoints.isEmpty() && routeSegments.isEmpty() && hasHistoryPrivacy) {
                return@mapNotNull null
            }

            val summary = routeMapper.toSummary(route)
            val visibleStart = visiblePoints.firstOrNull()?.timestamp
                ?: visibleSegments.minOfOrNull { it.startTime }
            val visibleEnd = visiblePoints.lastOrNull()?.timestamp
                ?: visibleSegments.maxOfOrNull { it.endTime }
            val distance = when {
                routePoints.isNotEmpty() -> PrivacyZonePolicy.recordedDistanceMeters(
                    routePoints,
                    activeZones
                )
                routeSegments.isNotEmpty() -> visibleSegments.sumOf { it.distanceMeters }
                else -> summary.distanceMeters
            }
            val duration = when {
                routePoints.isNotEmpty() -> PrivacyZonePolicy.recordedDurationSeconds(
                    routePoints,
                    activeZones
                )
                routeSegments.isNotEmpty() -> visibleSegments.sumOf {
                    (it.endTime - it.startTime).coerceAtLeast(0L) / 1_000L
                }
                else -> summary.durationSeconds
            }

            summary.copy(
                startTime = visibleStart ?: summary.startTime,
                endTime = if (route.endTime == null) null else visibleEnd ?: summary.endTime,
                distanceMeters = distance,
                durationSeconds = duration
            )
        }.sortedByDescending(RouteSummary::startTime)
    }
}
