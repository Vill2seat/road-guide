package com.roadguide.app.domain.usecase

import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.domain.model.RouteSummary
import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.domain.model.RouteTab
import com.roadguide.app.data.repository.TrackingSettingsRepository
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.RouteRepository

import kotlinx.coroutines.flow.Flow

/** Use case for retrieving current tracking settings from preferences. */
interface GetTrackingSettings {
    fun invoke(): TrackingSettingsState
}

/** Use case for retrieving a flow of location point summaries. */
interface GetLocationPoints {
    fun invoke(): Flow<List<LocationPointSummary>>
}

/** Use case for retrieving a flow of route summaries. */
interface GetRouteSummaries {
    fun invoke(): Flow<List<RouteSummary>>
}

/** Use case for toggling tracking enabled state. */
interface ToggleTracking {
    suspend fun invoke(enabled: Boolean): TrackingSettingsState
}

/** Use case for saving a route summary. */
interface SaveRoute {
    suspend fun invoke(summary: RouteSummary): Long
}