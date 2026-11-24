package com.roadguide.app.feature.places.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.data.local.entity.Place
import com.roadguide.app.data.local.entity.PrivacyZone
import com.roadguide.app.data.repository.PlaceRepository
import com.roadguide.app.data.repository.PrivacyZoneRepository
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.data.repository.TrackingSettingsRepository
import com.roadguide.app.domain.usecase.AggregateDailyExposure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlacesViewModel @Inject constructor(
    private val placeRepository: PlaceRepository,
    private val privacyZoneRepository: PrivacyZoneRepository,
    private val trackingSettingsRepository: TrackingSettingsRepository,
    private val routeRepository: RouteRepository,
    private val aggregateDailyExposure: AggregateDailyExposure
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _places = MutableStateFlow<List<Place>>(emptyList())
    val places: StateFlow<List<Place>> = _places.asStateFlow()

    private val _privacyZones = MutableStateFlow<List<PrivacyZone>>(emptyList())
    val privacyZones: StateFlow<List<PrivacyZone>> = _privacyZones.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _trackingEnabled = MutableStateFlow(false)
    val trackingEnabled: StateFlow<Boolean> = _trackingEnabled.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            try {
                val settings = trackingSettingsRepository.preferencesFlow.first()
                _trackingEnabled.value = settings.trackingEnabled

                combine(
                    placeRepository.observeAll(),
                    privacyZoneRepository.observeAll()
                ) { allPlaces, allZones -> allPlaces to allZones }
                    .collect { (allPlaces, allZones) ->
                        _places.value = allPlaces
                        _privacyZones.value = allZones
                        _uiState.value = if (allPlaces.isEmpty() && allZones.isEmpty()) {
                            ScreenState.Empty
                        } else {
                            ScreenState.Success
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unable to load places")
            }
        }
    }

    fun savePlace(place: Place) {
        viewModelScope.launch {
            try {
                if (place.id == 0L) placeRepository.insert(place)
                else placeRepository.update(place)
                _actionError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.message ?: "Unable to save place"
            }
        }
    }

    fun deletePlace(id: Long) {
        viewModelScope.launch {
            try {
                placeRepository.deleteById(id)
                _actionError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.message ?: "Unable to delete place"
            }
        }
    }

    fun savePrivacyZone(zone: PrivacyZone) {
        viewModelScope.launch {
            try {
                if (zone.id == 0L) privacyZoneRepository.insert(zone)
                else privacyZoneRepository.update(zone)
                regenerateReports()
                _actionError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.message ?: "Unable to save privacy zone"
            }
        }
    }

    fun deletePrivacyZone(id: Long) {
        viewModelScope.launch {
            try {
                privacyZoneRepository.deleteById(id)
                regenerateReports()
                _actionError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.message ?: "Unable to delete privacy zone"
            }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    private suspend fun regenerateReports() {
        val dates = routeRepository.observeAll().first().map { it.date }.distinct()
        dates.forEach { date -> aggregateDailyExposure.regenerate(date) }
    }
}
