package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteSegment
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import com.roadguide.app.data.repository.EnvironmentalSampleRepository
import com.roadguide.app.data.repository.ExposureResultRepository
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.data.repository.RouteSegmentRepository
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.domain.model.ExposureConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingPipeline @Inject constructor(
    private val locationSource: LocationSource,
    private val locationPointRepository: LocationPointRepository,
    private val routeRepository: RouteRepository,
    private val environmentalSampleRepository: EnvironmentalSampleRepository,
    private val routeSegmentRepository: RouteSegmentRepository,
    private val exposureResultRepository: ExposureResultRepository
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
        processLocationPoint(point)
    }

    private fun processLocationPoint(point: LocationPoint) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                locationPointRepository.insert(point)

                val sample = environmentalSampleRepository.fetchAndSave(point.latitude, point.longitude)

                val segment = RouteSegment(
                    routeId = currentRouteId,
                    startTime = point.timestamp,
                    endTime = point.timestamp,
                    activityType = classifyActivity(point),
                    distanceMeters = 0.0,
                    startLatitude = point.latitude,
                    startLongitude = point.longitude,
                    endLatitude = point.latitude,
                    endLongitude = point.longitude,
                    confidence = 0.5
                )
                val segmentId = routeSegmentRepository.insert(segment)

                val exposureScore = ExposureCalculationEngine.calculateSegmentExposure(
                    segment = segment,
                    sample = sample,
                    roadFactor = 1.0
                )
                val normalizedScore = ExposureCalculationEngine.normalizeExposure(exposureScore)

                val exposureResult = com.roadguide.app.data.local.entity.ExposureResult(
                    segmentId = segmentId,
                    routeId = currentRouteId,
                    exposureScore = normalizedScore,
                    pollution = sample?.let { ExposureCalculationEngine.calculatePollutionFactor(it) } ?: 1.0,
                    durationMinutes = 1,
                    activityLevel = ExposureCalculationEngine.getActivityFactor(classifyActivity(point)),
                    confidence = 0.5
                )
                exposureResultRepository.insert(exposureResult)

                _event.value = PipelineEvent.ExposureCalculated
            } catch (e: Exception) {
            }
        }
    }

    private fun classifyActivity(point: LocationPoint): com.roadguide.app.data.local.entity.ActivityType {
        val speed = point.speedMetersPerSecond ?: 0f
        return when {
            speed < 0.5f -> com.roadguide.app.data.local.entity.ActivityType.STATIONARY
            speed < 2.0f -> com.roadguide.app.data.local.entity.ActivityType.WALKING
            speed < 6.0f -> com.roadguide.app.data.local.entity.ActivityType.RUNNING
            else -> com.roadguide.app.data.local.entity.ActivityType.CYCLING
        }
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
