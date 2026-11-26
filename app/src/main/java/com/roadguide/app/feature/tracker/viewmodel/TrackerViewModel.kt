package com.roadguide.app.feature.tracker.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.data.local.entity.DailyReport
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.repository.EnvironmentalSampleRepository
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.PrivacyZoneRepository
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.domain.usecase.impl.PrivacyScope
import com.roadguide.app.domain.usecase.impl.PrivacyZonePolicy
import com.roadguide.app.domain.usecase.GetDailyReport
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.service.TrackingServiceController
import com.roadguide.app.map.model.MapPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val getTrackingSettings: GetTrackingSettings,
    private val trackingServiceController: TrackingServiceController,
    private val trackingPrefs: TrackingPrefs,
    private val environmentalSampleRepository: EnvironmentalSampleRepository,
    private val locationSource: LocationSource,
    private val locationPointRepository: LocationPointRepository,
    private val routeRepository: RouteRepository,
    private val privacyZoneRepository: PrivacyZoneRepository,
    private val getDailyReport: GetDailyReport
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _trackingEnabled = MutableStateFlow(false)
    val trackingEnabled: StateFlow<Boolean> = _trackingEnabled.asStateFlow()

    private val _currentRoutePoints = MutableStateFlow<List<LocationPointSummary>>(emptyList())
    val currentRoutePoints: StateFlow<List<LocationPointSummary>> = _currentRoutePoints.asStateFlow()

    private val _todayRoute = MutableStateFlow<Route?>(null)
    val todayRoute: StateFlow<Route?> = _todayRoute.asStateFlow()

    private val _routeLineChunks = MutableStateFlow<List<List<MapPoint>>>(emptyList())
    val routeLineChunks: StateFlow<List<List<MapPoint>>> = _routeLineChunks.asStateFlow()

    private val _environmentalSample = MutableStateFlow<EnvironmentalSample?>(null)
    val environmentalSample: StateFlow<EnvironmentalSample?> = _environmentalSample.asStateFlow()

    private val _dailyReport = MutableStateFlow<DailyReport?>(null)
    val dailyReport: StateFlow<DailyReport?> = _dailyReport.asStateFlow()

    private val _locationUpdates = MutableStateFlow<Location?>(null)
    val locationUpdates: StateFlow<Location?> = _locationUpdates.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    init {
        loadTrackingState()
        observeTodayData()
        observeLatestEnvironment()
        observeTodayReport()
        collectLocationUpdates()
    }

    fun loadTrackingState() {
        viewModelScope.launch {
            try {
                val state = getTrackingSettings.invoke()
                val preferences = trackingPrefs.preferencesFlow.first()
                _uiState.value = ScreenState.Success
                _trackingEnabled.value = state.trackingEnabled
                _isPaused.value = preferences.trackingPaused
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun observeTodayData() {
        viewModelScope.launch {
            try {
                combine(
                    combine(
                        routeRepository.observeByStatus(RouteStatus.IN_PROGRESS),
                        routeRepository.observeByStatus(RouteStatus.PAUSED)
                    ) { inProgress, paused -> inProgress + paused },
                    routeRepository.getByDate(LocalDate.now().toString())
                ) { activeRoutes, todayRoutes ->
                    (activeRoutes + todayRoutes).distinctBy(Route::id)
                }.combine(
                    combine(
                        locationPointRepository.observeAll(),
                        privacyZoneRepository.observeActiveZones()
                    ) { points, zones -> points to zones }
                ) { routes, pointData ->
                val (allPoints, activeZones) = pointData
                val route = routes.maxByOrNull(Route::startTime)
                val routePoints = route?.let { activeRoute ->
                    allPoints.asSequence()
                        .filter { it.routeId == activeRoute.id }
                        .sortedBy { it.timestamp }
                        .toList()
                }.orEmpty()
                val visiblePoints = PrivacyZonePolicy.filterPoints(
                    routePoints,
                    activeZones,
                    PrivacyScope.HISTORY
                )
                val routeLines = mutableListOf<List<MapPoint>>()
                var currentLine = mutableListOf<MapPoint>()
                var previousPoint: LocationPoint? = null
                visiblePoints.forEach { point ->
                    val previous = previousPoint
                    if (
                        previous != null &&
                        PrivacyZonePolicy.excludesPath(
                            previous.latitude,
                            previous.longitude,
                            point.latitude,
                            point.longitude,
                            activeZones,
                            PrivacyScope.HISTORY
                        )
                    ) {
                        if (currentLine.size >= 2) routeLines += currentLine.toList()
                        currentLine = mutableListOf()
                    }
                    currentLine += MapPoint(point.latitude, point.longitude)
                    previousPoint = point
                }
                if (currentLine.size >= 2) routeLines += currentLine.toList()

                val summaries = visiblePoints.map { point ->
                    LocationPointSummary(
                        id = point.id,
                        latitude = point.latitude,
                        longitude = point.longitude,
                        timestamp = point.timestamp,
                        activityType = point.speedMetersPerSecond?.let { speed ->
                            when {
                                speed < 0.5f -> "stationary"
                                speed < 2.0f -> "walking"
                                speed < 6.0f -> "running"
                                else -> "cycling"
                            }
                        } ?: "unknown",
                        speedMps = point.speedMetersPerSecond?.toDouble() ?: 0.0
                    )
                }
                val visibleRoute = route?.let { storedRoute ->
                    if (
                        routePoints.isEmpty() &&
                        activeZones.any { it.isActive && it.excludeFromHistory }
                    ) {
                        null
                    } else if (routePoints.isNotEmpty()) {
                        storedRoute.copy(
                            distanceMeters = PrivacyZonePolicy.recordedDistanceMeters(
                                routePoints,
                                activeZones
                            ),
                            totalDurationSeconds = PrivacyZonePolicy.recordedDurationSeconds(
                                routePoints,
                                activeZones
                            )
                        )
                    } else {
                        storedRoute
                    }
                }
                    Triple(visibleRoute, summaries, routeLines)
                }.collect { (route, points, lines) ->
                    _todayRoute.value = route
                    _currentRoutePoints.value = points
                    _routeLineChunks.value = lines
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("TrackerViewModel", "Failed to observe today's tracker data", e)
                _uiState.value = ScreenState.Error("Не удалось открыть данные трекера")
            }
        }
    }

    private fun observeLatestEnvironment() {
        viewModelScope.launch {
            try {
                environmentalSampleRepository.getLatest().collect { sample ->
                    _environmentalSample.value = sample
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("TrackerViewModel", "Failed to observe latest environment", e)
                _environmentalSample.value = null
            }
        }
    }

    private fun observeTodayReport() {
        viewModelScope.launch {
            try {
                getDailyReport.observeByDate(LocalDate.now().toString()).collect { report ->
                    _dailyReport.value = report
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("TrackerViewModel", "Failed to observe today's report", e)
                _dailyReport.value = null
            }
        }
    }

    private fun collectLocationUpdates() {
        viewModelScope.launch {
            try {
                combine(
                    locationSource.locationUpdates,
                    privacyZoneRepository.observeActiveZones()
                ) { location, activeZones ->
                    val decision = PrivacyZonePolicy.decisionAt(
                        location.latitude,
                        location.longitude,
                        activeZones
                    )
                    location.takeUnless { decision.excludeFromHistory }
                }.collect { location ->
                    _locationUpdates.value = location
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _locationUpdates.value = null
                _uiState.value = ScreenState.Error(e.message ?: "Privacy status unavailable")
            }
        }
    }

    fun toggleTracking(enabled: Boolean) {
        viewModelScope.launch {
            try {
                trackingServiceController.setTrackingEnabled(enabled)
                val state = getTrackingSettings.invoke()
                _trackingEnabled.value = state.trackingEnabled
                _isPaused.value = false
                _uiState.value = ScreenState.Success
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun startTracking() {
        toggleTracking(true)
    }

    fun stopTracking() {
        toggleTracking(false)
    }

    fun pauseTracking() {
        viewModelScope.launch {
            try {
                trackingServiceController.pauseTracking()
                _isPaused.value = true
                _uiState.value = ScreenState.Success
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unable to pause tracking")
            }
        }
    }

    fun resumeTracking() {
        viewModelScope.launch {
            try {
                trackingServiceController.resumeTracking()
                _isPaused.value = false
                _uiState.value = ScreenState.Success
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unable to resume tracking")
            }
        }
    }

}
