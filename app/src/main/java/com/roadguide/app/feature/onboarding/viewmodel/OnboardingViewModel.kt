package com.roadguide.app.feature.onboarding.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadguide.app.data.source.PermissionSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    val permissionSource: PermissionSource
) : ViewModel() {

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = 3
    val totalPages: Int = _totalPages

    fun nextPage() {
        if (_currentPage.value < _totalPages - 1) {
            _currentPage.value = _currentPage.value + 1
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value = _currentPage.value - 1
        }
    }

    fun completeOnboarding(): Deferred<Unit> {
        return viewModelScope.async(Dispatchers.IO) {
            permissionSource.markOnboardingCompleted()
        }
    }
}
