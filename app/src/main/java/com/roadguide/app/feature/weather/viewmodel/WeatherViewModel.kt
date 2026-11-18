package com.roadguide.app.feature.weather.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.domain.usecase.GetEnvironmentalSample
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val getEnvironmentalSample: GetEnvironmentalSample
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _sample = MutableStateFlow<EnvironmentalSample?>(null)
    val sample: StateFlow<EnvironmentalSample?> = _sample.asStateFlow()

    private val _lat = MutableStateFlow(0.0)
    val lat: StateFlow<Double> = _lat.asStateFlow()

    private val _lon = MutableStateFlow(0.0)
    val lon: StateFlow<Double> = _lon.asStateFlow()

    init {
        loadEnvironmentalData(55.7558, 37.6173)
    }

    fun loadEnvironmentalData(lat: Double, lon: Double) {
        _lat.value = lat
        _lon.value = lon
        viewModelScope.launch {
            try {
                _uiState.value = ScreenState.Loading
                val sample = getEnvironmentalSample.invoke(lat, lon)
                _sample.value = sample
                _uiState.value = if (sample != null) ScreenState.Success else ScreenState.Error("No data available")
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refresh() {
        loadEnvironmentalData(_lat.value, _lon.value)
    }
}
