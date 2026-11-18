package com.roadguide.app.feature.onboarding.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.data.source.PermissionSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the onboarding flow.
 * Tracks the current page and completion state.
 */
class OnboardingViewModel(
    private val permissionSource: PermissionSource
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = 3 // Welcome, Info, Permissions
    val totalPages: Int = _totalPages

    /**
     * Advances to the next page if not on the last one.
     */
    fun nextPage() {
        if (_currentPage.value < _totalPages - 1) {
            _currentPage.value = _currentPage.value + 1
        }
    }

    /**
     * Returns to the previous page if not on the first one.
     */
    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value = _currentPage.value - 1
        }
    }

    /**
     * Marks the onboarding flow as completed in DataStore.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            permissionSource.markOnboardingCompleted()
        }
    }
}
