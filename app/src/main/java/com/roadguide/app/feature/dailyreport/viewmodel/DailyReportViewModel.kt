package com.roadguide.app.feature.dailyreport.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.data.local.entity.DailyReport
import com.roadguide.app.domain.usecase.AggregateDailyExposure
import com.roadguide.app.domain.usecase.GetDailyReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate

@HiltViewModel
class DailyReportViewModel @Inject constructor(
    private val getDailyReport: GetDailyReport,
    private val aggregateDailyExposure: AggregateDailyExposure
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _report = MutableStateFlow<DailyReport?>(null)
    val report: StateFlow<DailyReport?> = _report.asStateFlow()

    private val _recentReports = MutableStateFlow<List<DailyReport>>(emptyList())
    val recentReports: StateFlow<List<DailyReport>> = _recentReports.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now().toString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()
    private var reportJob: Job? = null
    private var recentReportsJob: Job? = null
    private var refreshJob: Job? = null

    init {
        loadReportForDate(_selectedDate.value)
        loadRecentReports()
    }

    fun loadReportForDate(date: String) {
        if (runCatching { LocalDate.parse(date) }.isFailure) {
            _uiState.value = ScreenState.Error("Invalid report date")
            return
        }
        refreshJob?.cancel()
        _selectedDate.value = date
        reportJob?.cancel()
        reportJob = viewModelScope.launch {
            try {
                _uiState.value = ScreenState.Loading
                getDailyReport.observeByDate(date).collect { report ->
                    _report.value = report
                    _uiState.value = if (report != null) ScreenState.Success else ScreenState.Empty
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadRecentReports(limit: Int = 7) {
        recentReportsJob?.cancel()
        recentReportsJob = viewModelScope.launch {
            try {
                getDailyReport.observeRecent(limit).collect { reports ->
                    _recentReports.value = reports
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unable to load recent reports")
            }
        }
    }

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                _uiState.value = ScreenState.Loading
                aggregateDailyExposure.regenerate(_selectedDate.value)
                loadRecentReports()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unable to refresh report")
            }
        }
    }
}
