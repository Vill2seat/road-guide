package com.roadguide.app.feature.history.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.domain.model.RouteSummary
import com.roadguide.app.domain.usecase.GetRouteSummaries
import com.roadguide.app.domain.usecase.GetTrackingSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getRouteSummaries: GetRouteSummaries,
    private val getTrackingSettings: GetTrackingSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _reports = MutableStateFlow<List<RouteSummary>>(emptyList())
    val reports: StateFlow<List<RouteSummary>> = _reports.asStateFlow()

    private val _trackingEnabled = MutableStateFlow(false)
    val trackingEnabled: StateFlow<Boolean> = _trackingEnabled.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val settings = getTrackingSettings.invoke()
                _trackingEnabled.value = settings.trackingEnabled

                val reports = getRouteSummaries.invoke().take(1).firstOrNull() ?: emptyList()
                _reports.value = reports
                _uiState.value = ScreenState.Success
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
