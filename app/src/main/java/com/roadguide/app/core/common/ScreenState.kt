package com.roadguide.app.core.common

/**
 * Base sealed interface for all UI states used across feature screens.
 * Each feature screen can define its own state extending this pattern.
 */
sealed interface ScreenState {
    data object Empty : ScreenState
    data object Loading : ScreenState
    data class Error(val message: String) : ScreenState
}

/**
 * Generic Result type for use-case / repository layer contracts.
 */
sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
}