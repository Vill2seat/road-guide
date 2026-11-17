package com.roadguide.app.feature.places.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.data.local.DataProvider
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.repository.PlaceRepository
import com.roadguide.app.data.repository.TrackingSettingsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel for Places screen.
 * Displays places from repository.
 */
class PlacesViewModel(
    private val placeRepository: PlaceRepository,
    private val context: android.content.Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _places = MutableStateFlow<List<com.roadguide.app.data.local.entity.Place>>(emptyList())
    val places: StateFlow<List<com.roadguide.app.data.local.entity.Place>> = _places.asStateFlow()

    private val _trackingEnabled = MutableStateFlow(false)
    val trackingEnabled: StateFlow<Boolean> = _trackingEnabled.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            try {
                val dataProvider = DataProvider.getInstance(context)
                val trackingPrefsRepository = TrackingSettingsRepositoryImpl(dataProvider.trackingPrefs)
                val settings = trackingPrefsRepository.preferencesFlow.first()
                _trackingEnabled.value = settings.trackingEnabled

                val places = placeRepository.observePublicPlaces().first()
                _places.value = places
                _uiState.value = ScreenState.Success
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
