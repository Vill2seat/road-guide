package com.roadguide.app.feature.timeline.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.domain.model.RouteSummary
import com.roadguide.app.domain.usecase.GetRouteSummaries
import com.roadguide.app.domain.usecase.GetTrackingSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Timeline screen.
 * Displays route summaries from repository.
 */
class TimelineViewModel(
    private val getRouteSummaries: GetRouteSummaries,
    private val getTrackingSettings: GetTrackingSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _routes = MutableStateFlow<List<RouteSummary>>(emptyList())
    val routes: StateFlow<List<RouteSummary>> = _routes.asStateFlow()

    private val _trackingEnabled = MutableStateFlow(false)
    val trackingEnabled: StateFlow<Boolean> = _trackingEnabled.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val settings = getTrackingSettings.invoke()
                _trackingEnabled.value = settings.trackingEnabled

                val routes = getRouteSummaries.invoke().first()
                _routes.value = routes
                _uiState.value = ScreenState.Success
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message)
            }
        }
    }
}
