package com.roadguide.app.feature.timeline.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.data.local.entity.ActivityType
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.PrivacyZoneRepository
import com.roadguide.app.data.repository.RouteSegmentRepository
import com.roadguide.app.data.repository.StayRepository
import com.roadguide.app.domain.usecase.impl.PrivacyScope
import com.roadguide.app.domain.usecase.impl.PrivacyZonePolicy
import com.roadguide.app.domain.model.RouteTimeline
import com.roadguide.app.domain.model.TimelineEvent
import com.roadguide.app.domain.usecase.GetRouteSummaries
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val getRouteSummaries: GetRouteSummaries,
    private val routeSegmentRepository: RouteSegmentRepository,
    private val stayRepository: StayRepository,
    private val locationPointRepository: LocationPointRepository,
    private val privacyZoneRepository: PrivacyZoneRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _timelines = MutableStateFlow<List<RouteTimeline>>(emptyList())
    val timelines: StateFlow<List<RouteTimeline>> = _timelines.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ScreenState.Loading
            try {
                combine(
                    getRouteSummaries.invoke(),
                    routeSegmentRepository.observeAll(),
                    stayRepository.observeAll(),
                    locationPointRepository.observeAll(),
                    privacyZoneRepository.observeActiveZones()
                ) { routes, segments, stays, points, activeZones ->
                    routes.filter { route ->
                        Instant.ofEpochMilli(route.startTime)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate() == _selectedDate.value
                    }.map { route ->
                        val routePoints = points.filter { it.routeId == route.id }
                        val routeSegments = segments.filter {
                            it.routeId == route.id &&
                                !PrivacyZonePolicy.excludesSegment(
                                    it,
                                    routePoints,
                                    activeZones,
                                    PrivacyScope.HISTORY
                                )
                        }
                        val routeStays = stays.filter {
                            it.routeId == route.id &&
                                !PrivacyZonePolicy.excludesStay(
                                    it,
                                    activeZones,
                                    PrivacyScope.HISTORY
                                )
                        }
                        val stayEvents = routeStays.map { stay ->
                            TimelineEvent(
                                id = "stay-${stay.id}",
                                startTime = stay.startTime,
                                endTime = stay.endTime,
                                activityType = ActivityType.STATIONARY.name,
                                distanceMeters = 0.0,
                                isStay = true
                            )
                        }
                        val activityEvents = routeSegments.mapNotNull { segment ->
                            val activityType =
                                segment.correctedActivityType ?: segment.activityType
                            val overlapsStay = routeStays.any { stay ->
                                segment.startTime <= stay.endTime &&
                                    stay.startTime <= segment.endTime
                            }
                            if (activityType == ActivityType.STATIONARY && overlapsStay) {
                                null
                            } else {
                                TimelineEvent(
                                    id = "segment-${segment.id}",
                                    startTime = segment.startTime,
                                    endTime = segment.endTime,
                                    activityType = activityType.name,
                                    distanceMeters = segment.distanceMeters,
                                    isStay = false
                                )
                            }
                        }
                        RouteTimeline(
                            route = route,
                            events = (activityEvents + stayEvents).sortedBy { it.startTime }
                        )
                    }.sortedByDescending { it.route.startTime }
                }.collect { timelines ->
                    _timelines.value = timelines
                    _uiState.value = ScreenState.Success
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun shiftDate(days: Long) {
        val nextDate = _selectedDate.value.plusDays(days)
        if (nextDate.isAfter(LocalDate.now())) return
        _selectedDate.value = nextDate
        loadData()
    }

    fun selectToday() {
        if (_selectedDate.value == LocalDate.now()) return
        _selectedDate.value = LocalDate.now()
        loadData()
    }
}
