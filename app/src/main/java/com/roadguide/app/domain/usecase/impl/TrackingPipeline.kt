package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.data.source.LocationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core tracking pipeline that orchestrates:
 * 1. Location collection from [LocationSource]
 * 2. Movement classification
 * 3. Route segmentation (segments + stays)
 * 4. Exposure calculation
 * 5. Daily report generation
 */
@Singleton
class TrackingPipeline @Inject constructor(
    private val locationSource: LocationSource,
    private val locationPointRepository: LocationPointRepository,
    private val routeRepository: RouteRepository
) {
    private val _event = MutableStateFlow<PipelineEvent>(PipelineEvent.Idle)
    val event: Flow<PipelineEvent> = _event.stateIn(
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        started = SharingStarted.WhileSubscribed(),
        initialValue = PipelineEvent.Idle
    )

    private var currentRouteId: Long = 0

    suspend fun startTracking(routeId: Long = 0): Route? {
        currentRouteId = routeId
        locationSource.requestLocationUpdates(intervalSeconds = 10)
        _event.value = PipelineEvent.Started
        return null
    }

    suspend fun stopTracking(): Route? {
        locationSource.removeLocationUpdates()
        _event.value = PipelineEvent.Stopped
        return null
    }

    fun onLocationReceived(point: LocationPoint) {
        _event.value = PipelineEvent.LocationReceived
    }

    fun pauseTracking() {
        _event.value = PipelineEvent.Paused
    }

    fun resumeTracking() {
        _event.value = PipelineEvent.Resumed
    }

    enum class PipelineEvent {
        Idle,
        Started,
        Stopped,
        Paused,
        Resumed,
        LocationReceived,
        RouteCompleted,
        ExposureCalculated
    }
}
