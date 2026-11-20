package com.roadguide.app.domain.usecase.impl

import android.location.Location
import android.location.LocationManager

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
import com.roadguide.app.data.repository.PrivacyZoneRepository
import com.roadguide.app.data.source.ActivityRecognitionSource
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.domain.usecase.impl.MovementDetectionEngine
import com.roadguide.app.domain.model.ExposureConstants
import com.roadguide.app.domain.usecase.impl.QualityFlags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingPipeline @Inject constructor(
    private val locationSource: LocationSource,
    private val activityRecognitionSource: ActivityRecognitionSource,
    private val locationPointRepository: LocationPointRepository,
    private val routeRepository: RouteRepository,
    private val environmentalSampleRepository: EnvironmentalSampleRepository,
    private val routeSegmentRepository: RouteSegmentRepository,
    private val exposureResultRepository: ExposureResultRepository,
    private val privacyZoneRepository: PrivacyZoneRepository
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
                // Check if point is inside a privacy zone
                val activeZones = privacyZoneRepository.observeActiveZones().firstOrNull() as? List<com.roadguide.app.data.local.entity.PrivacyZone>

                val location = android.location.Location(LocationManager.GPS_PROVIDER)
                location.latitude = point.latitude
                location.longitude = point.longitude

                if (activeZones != null && isLocationInPrivacyZone(location, activeZones)) {
                    // Skip this point - it's inside a privacy zone
                    return@launch
                }

                locationPointRepository.insert(point)

                val sample = environmentalSampleRepository.fetchAndSave(point.latitude, point.longitude)

                val envSample = sample

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
                    pollution = envSample?.let { ExposureCalculationEngine.calculatePollutionFactor(it) } ?: 1.0,
                    durationMinutes = 1,
                    activityLevel = ExposureCalculationEngine.getActivityFactor(classifyActivity(point)),
                    confidence = 0.8
                )
                exposureResultRepository.insert(exposureResult)

                _event.value = PipelineEvent.ExposureCalculated
            } catch (e: Exception) {
            }
        }
    }

    private fun classifyActivity(point: LocationPoint): com.roadguide.app.data.local.entity.ActivityType {
        // Use MovementDetectionEngine for more accurate classification
        // that considers speed patterns, not just current speed
        val speed = point.speedMetersPerSecond ?: 0f
        return when {
            speed < 0.5f -> com.roadguide.app.data.local.entity.ActivityType.STATIONARY
            speed < 2.0f -> com.roadguide.app.data.local.entity.ActivityType.WALKING
            speed < 6.0f -> com.roadguide.app.data.local.entity.ActivityType.RUNNING
            else -> com.roadguide.app.data.local.entity.ActivityType.CYCLING
        }
    }

    private fun isLocationInPrivacyZone(
        location: android.location.Location,
        zones: List<com.roadguide.app.data.local.entity.PrivacyZone>
    ): Boolean {
        for (zone in zones) {
            val distance = calculateDistanceMeters(
                location.latitude, location.longitude,
                zone.centerLatitude, zone.centerLongitude
            )
            if (distance <= zone.radiusMeters) {
                return true
            }
        }
        return false
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
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
