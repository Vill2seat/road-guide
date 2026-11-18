package com.roadguide.app.data.source

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.permissionDataStore by preferencesDataStore(name = "permission_prefs")

/**
 * Data source for runtime permissions.
 *
 * Handles checking and requesting:
 * - ACCESS_FINE_LOCATION
 * - ACCESS_BACKGROUND_LOCATION
 * - POST_NOTIFICATIONS (Android 13+)
 *
 * Persists "permissions requested" flag in DataStore so the permission onboarding
 * is shown only once (on first launch or when permissions are missing).
 */
class PermissionSource(private val context: Context) {

    /**
     * Checks if all required permissions are granted.
     */
    fun hasAllPermissions(): Boolean {
        return hasLocationPermission() && hasNotificationsPermission()
    }

    /**
     * Checks if location permissions are granted (foreground + background).
     */
    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val background = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine && background
    }

    /**
     * Checks if notification permission is granted (always true on Android < 13).
     */
    fun hasNotificationsPermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns true if the user has already been asked for permissions (even if denied).
     * Used to skip the onboarding on subsequent launches.
     */
    fun hasBeenAsked(): Flow<Boolean> {
        return context.permissionDataStore.data.map {
            it[PERMISSIONS_ASKED] ?: false
        }
    }

    /**
     * Marks that permissions have been requested at least once.
     */
    suspend fun markAsAsked() {
        context.permissionDataStore.edit { it[PERMISSIONS_ASKED] = true }
    }

    /**
     * Returns true if the user has completed the onboarding flow.
     */
    fun hasCompletedOnboarding(): Flow<Boolean> {
        return context.permissionDataStore.data.map {
            it[ONBOARDING_COMPLETED] ?: false
        }
    }

    /**
     * Marks the onboarding flow as completed.
     */
    suspend fun markOnboardingCompleted() {
        context.permissionDataStore.edit { it[ONBOARDING_COMPLETED] = true }
    }

    /**
     * Requests all missing permissions from the calling Activity.
     * The caller must register an ActivityResultLauncher for
     * [RequestMultiplePermissions] to handle the result.
     */
    fun requestAllPermissions(activity: Activity): java.util.ArrayList<String> {
        val missing = mutableListOf<String>()

        if (!hasLocationPermission()) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
            missing.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (!hasNotificationsPermission()) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missing.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }

        return java.util.ArrayList(missing)
    }

    companion object {
        const val PERMISSION_REQUEST_CODE = 1003
        private val PERMISSIONS_ASKED = booleanPreferencesKey("permissions_asked")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
