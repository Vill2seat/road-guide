package com.roadguide.app.domain.usecase.impl

import android.location.Location
import android.location.LocationManager
import android.util.Log

import com.roadguide.app.data.local.entity.ActivityType
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
import com.roadguide.app.data.repository.StayRepository
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val privacyZoneRepository: PrivacyZoneRepository,
    private val stayRepository: StayRepository
) {
    private val _event = MutableStateFlow<PipelineEvent>(PipelineEvent.Idle)
    val event: Flow<PipelineEvent> = _event.stateIn(
        scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
        started = SharingStarted.WhileSubscribed(),
        initialValue = PipelineEvent.Idle
    )

    private val pipelineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val processingMutex = Mutex()
    private var processingJob: Job? = null

    private var currentRouteId: Long = 0
    private var currentSegmentSampleCount = 0
    private var currentSegmentPointsWithEnv = 0
    private var lastActivityType: com.roadguide.app.data.local.entity.ActivityType? = null
    private var previousLocationPoint: LocationPoint? = null
    private var currentSegmentStartTime: Long? = null

    suspend fun startTracking(routeId: Long = 0): Route? {
        currentRouteId = routeId
        previousLocationPoint = null
        currentSegmentSampleCount = 0
        currentSegmentPointsWithEnv = 0
        lastActivityType = null
        currentSegmentStartTime = null
        locationSource.requestLocationUpdates(intervalSeconds = 10)
        _event.value = PipelineEvent.Started
        return null
    }

    suspend fun stopTracking(): Route? {
        locationSource.removeLocationUpdates()
        processingJob?.cancel()
        processingJob = null
        previousLocationPoint = null
        currentSegmentStartTime = null
        _event.value = PipelineEvent.Stopped
        return null
    }

    fun onLocationReceived(point: LocationPoint) {
        _event.value = PipelineEvent.LocationReceived
        processLocationPoint(point)
    }

    private fun processLocationPoint(point: LocationPoint) {
        processingJob = pipelineScope.launch {
            processingMutex.withLock { processLocationPointLocked(point) }
        }
    }

    private suspend fun processLocationPointLocked(point: LocationPoint) {
        try {
            val activeZones = privacyZoneRepository.observeActiveZones().firstOrNull()
                ?: run {
                    Log.e("TrackingPipeline", "Privacy policy unavailable; dropping location sample")
                    previousLocationPoint = null
                    return
                }
            val location = Location(LocationManager.GPS_PROVIDER).apply {
                latitude = point.latitude
                longitude = point.longitude
            }

            val privacyDecision = PrivacyZonePolicy.decisionAt(
                location.latitude,
                location.longitude,
                activeZones
            )
            if (privacyDecision.excludeFromHistory) {
                previousLocationPoint = null
                currentSegmentSampleCount = 0
                currentSegmentPointsWithEnv = 0
                lastActivityType = null
                currentSegmentStartTime = null
                return
            }

            val storedPoint = point.copy(
                excludeFromExposure = point.excludeFromExposure ||
                    privacyDecision.excludeFromExposure
            )
            locationPointRepository.insert(storedPoint)
            val previousPoint = previousLocationPoint
            if (previousPoint == null) {
                previousLocationPoint = storedPoint
                lastActivityType = classifyActivity(storedPoint)
                currentSegmentStartTime = storedPoint.timestamp
                return
            }

            val intervalMillis = storedPoint.timestamp - previousPoint.timestamp
            if (intervalMillis < MIN_SAMPLE_INTERVAL_MS) {
                Log.w("TrackingPipeline", "ignoring non-increasing location timestamp")
                return
            }
            val crossesHistoryZone = PrivacyZonePolicy.excludesPath(
                previousPoint.latitude,
                previousPoint.longitude,
                storedPoint.latitude,
                storedPoint.longitude,
                activeZones,
                PrivacyScope.HISTORY
            )
            if (crossesHistoryZone) {
                previousLocationPoint = storedPoint
                currentSegmentSampleCount = 0
                currentSegmentPointsWithEnv = 0
                lastActivityType = classifyActivity(storedPoint)
                currentSegmentStartTime = storedPoint.timestamp
                return
            }
            previousLocationPoint = storedPoint
            val activityType = classifyActivity(storedPoint)
            if (intervalMillis > MAX_SAMPLE_INTERVAL_MS) {
                lastActivityType = activityType
                currentSegmentSampleCount = 0
                currentSegmentPointsWithEnv = 0
                currentSegmentStartTime = storedPoint.timestamp
                Log.w("TrackingPipeline", "exposure interval skipped after a GPS gap")
                return
            }

            if (lastActivityType != activityType) {
                lastActivityType = activityType
                currentSegmentSampleCount = 0
                currentSegmentPointsWithEnv = 0
                currentSegmentStartTime = previousPoint.timestamp
            }
            currentSegmentSampleCount++

            val excludeIntervalFromExposure = storedPoint.excludeFromExposure ||
                PrivacyZonePolicy.excludesPath(
                    previousPoint.latitude,
                    previousPoint.longitude,
                    storedPoint.latitude,
                    storedPoint.longitude,
                    activeZones,
                    PrivacyScope.EXPOSURE
                )
            val envSample = if (excludeIntervalFromExposure) {
                null
            } else {
                environmentalSampleRepository.fetchAndSave(
                    storedPoint.latitude,
                    storedPoint.longitude,
                    storedPoint.timestamp
                )
            }
            val hasValidAirQuality = ExposureCalculationEngine
                .hasSufficientAirQualityData(envSample)
            if (!hasValidAirQuality) {
                Log.w(
                    "TrackingPipeline",
                    if (excludeIntervalFromExposure) {
                        "Exposure omitted by privacy policy"
                    } else {
                        "Air-quality data insufficient"
                    }
                )
            }
            if (hasValidAirQuality) currentSegmentPointsWithEnv++

            val segment = RouteSegment(
                routeId = currentRouteId,
                startTime = previousPoint.timestamp,
                endTime = storedPoint.timestamp,
                activityType = activityType,
                distanceMeters = calculateDistanceMeters(
                    previousPoint.latitude,
                    previousPoint.longitude,
                    storedPoint.latitude,
                    storedPoint.longitude
                ),
                startLatitude = previousPoint.latitude,
                startLongitude = previousPoint.longitude,
                endLatitude = storedPoint.latitude,
                endLongitude = storedPoint.longitude,
                confidence = MovementDetectionEngine.calculateConfidence(
                    activityType,
                    currentSegmentSampleCount
                )
            )
            val segmentId = routeSegmentRepository.insert(segment)

            val rawExposureScore = ExposureCalculationEngine.calculateSegmentExposure(
                segment = segment,
                sample = envSample,
                roadFactor = 1.0
            )
            val pollutionFactor = envSample?.let {
                ExposureCalculationEngine.calculatePollutionFactor(it)
            }
            if (rawExposureScore != null && pollutionFactor != null) {
                val normalizedScore = ExposureCalculationEngine.normalizeExposure(rawExposureScore)
                val coverage = currentSegmentPointsWithEnv.toDouble() /
                    currentSegmentSampleCount * 100.0
                exposureResultRepository.insert(
                    com.roadguide.app.data.local.entity.ExposureResult(
                        segmentId = segmentId,
                        routeId = currentRouteId,
                        exposureScore = normalizedScore,
                        rawExposureScore = rawExposureScore,
                        pollution = pollutionFactor,
                        durationMinutes = kotlin.math.ceil(intervalMillis / 60_000.0)
                            .toInt()
                            .coerceAtLeast(1),
                        activityLevel = ExposureCalculationEngine.getActivityFactor(activityType),
                        confidence = segment.confidence,
                        coverage = coverage
                    )
                )
                Log.d("TrackingPipeline", "exposure=$normalizedScore")
            } else {
                Log.w("TrackingPipeline", "exposure omitted: valid air-quality data unavailable")
            }

            val speed = storedPoint.speedMetersPerSecond ?: 0f
            if (speed < 0.5 && currentSegmentSampleCount >= 5) {
                val stayStartTime = currentSegmentStartTime ?: previousPoint.timestamp
                val stayEndTime = storedPoint.timestamp
                val stayDurationSeconds = (stayEndTime - stayStartTime) / 1_000L
                val stay = com.roadguide.app.data.local.entity.Stay(
                    routeId = currentRouteId,
                    startTime = stayStartTime,
                    endTime = stayEndTime,
                    centerLatitude = storedPoint.latitude,
                    centerLongitude = storedPoint.longitude,
                    confidence = MovementDetectionEngine.calculateConfidence(
                        ActivityType.STATIONARY,
                        currentSegmentSampleCount
                    )
                )
                val stayId = stayRepository.insert(stay)
                val envSampleForStay = if (excludeIntervalFromExposure) {
                    null
                } else {
                    environmentalSampleRepository.fetchAndSave(
                        storedPoint.latitude,
                        storedPoint.longitude,
                        stayEndTime
                    )
                }
                val staySegment = RouteSegment(
                    routeId = currentRouteId,
                    startTime = stayStartTime,
                    endTime = stayEndTime,
                    activityType = ActivityType.STATIONARY,
                    distanceMeters = 0.0,
                    startLatitude = storedPoint.latitude,
                    startLongitude = storedPoint.longitude,
                    endLatitude = storedPoint.latitude,
                    endLongitude = storedPoint.longitude,
                    confidence = stay.confidence
                )
                val stayRawExposureScore = ExposureCalculationEngine.calculateSegmentExposure(
                    segment = staySegment,
                    sample = envSampleForStay,
                    roadFactor = 1.0
                )
                val stayPollutionFactor = envSampleForStay?.let {
                    ExposureCalculationEngine.calculatePollutionFactor(it)
                }
                if (stayRawExposureScore != null && stayPollutionFactor != null) {
                    exposureResultRepository.insert(
                        com.roadguide.app.data.local.entity.ExposureResult(
                            segmentId = segmentId,
                            routeId = currentRouteId,
                            stayId = stayId,
                            exposureScore = ExposureCalculationEngine.normalizeExposure(
                                stayRawExposureScore
                            ),
                            rawExposureScore = stayRawExposureScore,
                            pollution = stayPollutionFactor,
                            durationMinutes = kotlin.math.ceil(stayDurationSeconds / 60.0)
                                .toInt()
                                .coerceAtLeast(1),
                            activityLevel = ExposureCalculationEngine.getActivityFactor(
                                ActivityType.STATIONARY
                            ),
                            confidence = staySegment.confidence,
                            coverage = currentSegmentPointsWithEnv.toDouble() /
                                currentSegmentSampleCount * 100.0
                        )
                    )
                } else {
                    Log.w("TrackingPipeline", "stay exposure omitted: valid air-quality data unavailable")
                }
                _event.value = PipelineEvent.StayDetected
            }
        } catch (e: Exception) {
            Log.e("TrackingPipeline", "Failed to process location point", e)
        }
    }

    private fun classifyActivity(
        point: LocationPoint,
        freshActivityType: com.roadguide.app.data.local.entity.ActivityType? = null
    ): com.roadguide.app.data.local.entity.ActivityType {
        // Use ActivityRecognitionSource data if available and fresh (< 30 seconds)
        val speed = point.speedMetersPerSecond ?: 0f

        val activityType = when {
            freshActivityType != null && freshActivityType != com.roadguide.app.data.local.entity.ActivityType.STATIONARY -> freshActivityType
            speed < 0.5f -> com.roadguide.app.data.local.entity.ActivityType.STATIONARY
            speed < 2.0f -> com.roadguide.app.data.local.entity.ActivityType.WALKING
            speed < 6.0f -> com.roadguide.app.data.local.entity.ActivityType.RUNNING
            else -> com.roadguide.app.data.local.entity.ActivityType.CYCLING
        }

        return activityType
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

    private companion object {
        const val MIN_SAMPLE_INTERVAL_MS = 1_000L
        const val MAX_SAMPLE_INTERVAL_MS = 5 * 60 * 1_000L
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
        ExposureCalculated,
        StayDetected
    }
}
