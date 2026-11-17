package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.domain.model.RouteSummary
import com.roadguide.app.domain.usecase.GetLocationPoints
import com.roadguide.app.domain.usecase.GetRouteSummaries
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.mapper.LocationPointMapper
import com.roadguide.app.mapper.RouteMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case implementation for retrieving location point summaries.
 * Wires domain contract to LocationPointRepository.
 */
class GetLocationPointsImpl(
    private val locationPointRepository: LocationPointRepository,
    private val locationPointMapper: com.roadguide.app.mapper.LocationPointMapper
) : GetLocationPoints {

    override fun invoke(): Flow<List<LocationPointSummary>> = locationPointRepository.observeAll()
        .map { points -> points.map { locationPointMapper.toSummary(it) } }
}

/**
 * Use case implementation for retrieving route summaries.
 * Wires domain contract to RouteRepository.
 */
class GetRouteSummariesImpl(
    private val routeRepository: RouteRepository,
    private val routeMapper: RouteMapper
) : GetRouteSummaries {

    override fun invoke(): Flow<List<RouteSummary>> = routeRepository.observeAll()
        .map { routes -> routes.map { routeMapper.toSummary(it) } }
}
