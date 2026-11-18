package com.roadguide.app.feature.dailyreport.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.data.local.entity.DailyReport
import com.roadguide.app.domain.usecase.GetDailyReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyReportViewModel @Inject constructor(
    private val getDailyReport: GetDailyReport
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val uiState: StateFlow<ScreenState> = _uiState.asStateFlow()

    private val _report = MutableStateFlow<DailyReport?>(null)
    val report: StateFlow<DailyReport?> = _report.asStateFlow()

    private val _recentReports = MutableStateFlow<List<DailyReport>>(emptyList())
    val recentReports: StateFlow<List<DailyReport>> = _recentReports.asStateFlow()

    private val _selectedDate = MutableStateFlow<String?>(null)
    val selectedDate: StateFlow<String?> = _selectedDate.asStateFlow()

    init {
        loadRecentReports()
    }

    fun loadReportForDate(date: String) {
        _selectedDate.value = date
        viewModelScope.launch {
            try {
                _uiState.value = ScreenState.Loading
                val report = getDailyReport.invoke(date)
                _report.value = report
                _uiState.value = if (report != null) ScreenState.Success else ScreenState.Empty
            } catch (e: Exception) {
                _uiState.value = ScreenState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadRecentReports(limit: Int = 7) {
        viewModelScope.launch {
            try {
                val reports = getDailyReport.observeRecent(limit).first()
                _recentReports.value = reports
            } catch (e: Exception) {
            }
        }
    }

    fun refresh() {
        _selectedDate.value?.let { loadReportForDate(it) }
    }
}
