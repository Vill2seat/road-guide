package com.roadguide.app.feature.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.domain.usecase.ToggleTracking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Tracker screen.
 * Exposes tracking state and handles start/stop actions.
 */
class TrackerViewModel(
    private val getTrackingSettings: GetTrackingSettings,
    private val toggleTracking: ToggleTracking
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _trackingEnabled = MutableStateFlow(false)
    val trackingEnabled: StateFlow<Boolean> = _trackingEnabled.asStateFlow()

    init {
        loadTrackingState()
    }

    private fun loadTrackingState() {
        viewModelScope.launch {
            try {
                val state = getTrackingSettings.invoke()
                _uiState.value = ScreenState.Success
                _trackingEnabled.value = state.trackingEnabled
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message)
            }
        }
    }

    /**
     * Toggle tracking enabled state.
     * @param enabled New tracking state
     */
    fun toggleTracking(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val state = toggleTracking.invoke(enabled)
                _trackingEnabled.value = state.trackingEnabled
                _uiState.value = ScreenState.Success
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message)
            }
        }
    }

    /**
     * Action for user to start tracking.
     */
    fun startTracking() {
        toggleTracking(true)
    }

    /**
     * Action for user to stop tracking.
     */
    fun stopTracking() {
        toggleTracking(false)
    }
}
