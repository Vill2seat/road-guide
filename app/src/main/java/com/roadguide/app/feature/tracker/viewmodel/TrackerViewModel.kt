package com.roadguide.app.feature.tracker.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.repository.EnvironmentalSampleRepository
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.domain.usecase.GetEnvironmentalSample
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.domain.usecase.ToggleTracking
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackerViewModel @Inject constructor(
    private val getTrackingSettings: GetTrackingSettings,
    private val toggleTracking: ToggleTracking,
    private val getEnvironmentalSample: GetEnvironmentalSample,
    private val environmentalSampleRepository: EnvironmentalSampleRepository,
    private val locationSource: LocationSource
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _trackingEnabled = MutableStateFlow(false)
    val trackingEnabled: StateFlow<Boolean> = _trackingEnabled.asStateFlow()

    private val _recordingState = MutableStateFlow(RecordingState.IDLE)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _currentRoutePoints = MutableStateFlow<List<LocationPointSummary>>(emptyList())
    val currentRoutePoints: StateFlow<List<LocationPointSummary>> = _currentRoutePoints.asStateFlow()

    private val _environmentalSample = MutableStateFlow<EnvironmentalSample?>(null)
    val environmentalSample: StateFlow<EnvironmentalSample?> = _environmentalSample.asStateFlow()

    private val _internetAvailableToday = MutableStateFlow<Boolean?>(null)
    val internetAvailableToday: StateFlow<Boolean?> = _internetAvailableToday.asStateFlow()

    private val _locationUpdates = MutableStateFlow<Location?>(null)
    val locationUpdates: StateFlow<Location?> = _locationUpdates.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    init {
        loadTrackingState()
        loadInternetAvailability()
        collectLocationUpdates()
    }

    fun loadTrackingState() {
        viewModelScope.launch {
            try {
                val state = getTrackingSettings.invoke()
                _uiState.value = ScreenState.Success
                _trackingEnabled.value = state.trackingEnabled
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadInternetAvailability() {
        viewModelScope.launch {
            try {
                val available = environmentalSampleRepository.wasInternetAvailableToday()
                _internetAvailableToday.value = available
            } catch (e: Exception) {
                _internetAvailableToday.value = null
            }
        }
    }

    private fun collectLocationUpdates() {
        viewModelScope.launch {
            locationSource.locationUpdates.collect { location ->
                _locationUpdates.value = location
                // Only process points while recording
                if (_recordingState.value == RecordingState.RECORDING) {
                    processLocationPoint(
                        LocationPoint(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timestamp = location.time,
                            accuracyMeters = location.accuracy,
                            speedMetersPerSecond = location.speed.toFloat(),
                            bearing = location.bearing.toFloat(),
                            altitude = location.altitude,
                            provider = location.provider ?: "fused",
                            isMock = false,
                            qualityFlags = 0,
                            routeId = null,
                            collectionDiagnostics = null
                        )
                    )
                }
            }
        }
    }

    fun toggleTracking(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val state = toggleTracking.invoke(enabled)
                _trackingEnabled.value = state.trackingEnabled
                _uiState.value = ScreenState.Success
                if (enabled) {
                    _recordingState.value = RecordingState.RECORDING
                } else {
                    _recordingState.value = RecordingState.IDLE
                }
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun startTracking() {
        _recordingState.value = RecordingState.RECORDING
        _uiState.value = ScreenState.Success
    }

    fun stopTracking() {
        _recordingState.value = RecordingState.IDLE
        _uiState.value = ScreenState.Success
    }

    fun processLocationPoint(point: LocationPoint) {
        if (_recordingState.value != RecordingState.RECORDING) return

        _currentRoutePoints.value = _currentRoutePoints.value + LocationPointSummary(
            id = 0L,
            latitude = point.latitude,
            longitude = point.longitude,
            timestamp = point.timestamp,
            speedMps = point.speedMetersPerSecond?.toDouble() ?: 0.0,
            activityType = activityTypeToString(point)
        )
    }

    fun pauseTracking() {
        _isPaused.value = true
    }

    fun resumeTracking() {
        _isPaused.value = false
    }

    fun loadEnvironmentalData(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val sample = getEnvironmentalSample.invoke(lat, lon)
                _environmentalSample.value = sample
            } catch (e: Exception) {
            }
        }
    }

    private fun activityTypeToString(point: LocationPoint): String {
        val speed = point.speedMetersPerSecond ?: return "unknown"
        return when {
            speed < 0.5f -> "stationary"
            speed < 2.0f -> "walking"
            speed < 6.0f -> "running"
            else -> "cycling"
        }
    }
}

enum class RecordingState {
    IDLE,
    RECORDING,
    PAUSED
}
