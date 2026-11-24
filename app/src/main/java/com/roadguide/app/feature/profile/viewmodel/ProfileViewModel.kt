package com.roadguide.app.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.service.TrackingServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getTrackingSettings: GetTrackingSettings,
    private val trackingServiceController: TrackingServiceController
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _trackingSettings = MutableStateFlow(TrackingSettingsState())
    val trackingSettings: StateFlow<TrackingSettingsState> = _trackingSettings.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            try {
                val state = getTrackingSettings.invoke()
                _trackingSettings.value = state
                _uiState.value = ScreenState.Success
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun toggleTracking(enabled: Boolean) {
        viewModelScope.launch {
            try {
                trackingServiceController.setTrackingEnabled(enabled)
                val state = getTrackingSettings.invoke()
                _trackingSettings.value = state
                _uiState.value = ScreenState.Success
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
