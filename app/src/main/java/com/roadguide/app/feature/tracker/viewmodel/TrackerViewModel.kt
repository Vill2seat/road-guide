package com.roadguide.app.feature.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.domain.model.LocationPointSummary
import com.roadguide.app.domain.usecase.GetEnvironmentalSample
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.domain.usecase.ToggleTracking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.roadguide.app.data.local.entity.LocationPoint

class TrackerViewModel(
    private val getTrackingSettings: GetTrackingSettings,
    private val toggleTracking: ToggleTracking,
    private val getEnvironmentalSample: GetEnvironmentalSample
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

    init {
        loadTrackingState()
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

    fun toggleTracking(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val state = toggleTracking.invoke(enabled)
                _trackingEnabled.value = state.trackingEnabled
                _uiState.value = ScreenState.Success
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

    /**
     * Loads environmental data for given coordinates (called when location updates arrive).
     */
    fun loadEnvironmentalData(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val sample = getEnvironmentalSample.invoke(lat, lon)
                _environmentalSample.value = sample
            } catch (e: Exception) {
                // Silently fail — environmental data is supplementary
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
