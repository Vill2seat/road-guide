package com.roadguide.app.domain.usecase

import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.domain.model.RouteSummary
import com.roadguide.app.domain.model.TrackingSettingsState
import kotlinx.coroutines.flow.Flow

interface GetTrackingSettings {
    suspend fun invoke(): TrackingSettingsState
}

interface GetLocationPoints {
    fun invoke(): Flow<List<LocationPointSummary>>
}

interface GetRouteSummaries {
    fun invoke(): Flow<List<RouteSummary>>
}

interface ToggleTracking {
    suspend fun invoke(enabled: Boolean): TrackingSettingsState
}

interface SaveRoute {
    suspend fun invoke(summary: RouteSummary): Long
}
