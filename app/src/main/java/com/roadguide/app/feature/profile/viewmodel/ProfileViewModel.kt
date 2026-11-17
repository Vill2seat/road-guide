package com.roadguide.app.feature.profile.viewmodel

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
 * ViewModel for Profile screen.
 * Displays and manages tracking settings.
 */
class ProfileViewModel(
    private val getTrackingSettings: GetTrackingSettings,
    private val toggleTracking: ToggleTracking
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    val trackingSettings: StateFlow<TrackingSettingsState> = _trackingSettings.asStateFlow()
    private val _trackingSettings = MutableStateFlow(TrackingSettingsState())

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val state = getTrackingSettings.invoke()
                _trackingSettings.value = state
                _uiState.value = ScreenState.Success
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message)
            }
        }
    }

    /**
     * Toggle tracking enabled.
     */
    fun toggleTracking(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val state = toggleTracking.invoke(enabled)
                _trackingSettings.value = state
                _uiState.value = ScreenState.Success
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message)
            }
        }
    }
}
