package com.roadguide.app.service

import android.content.Context
import androidx.core.content.ContextCompat
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.source.PermissionSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackingPrefs: TrackingPrefs,
    private val permissionSource: PermissionSource
) {

    suspend fun setTrackingEnabled(enabled: Boolean) {
        if (enabled) {
            requireTrackingPermission()
            trackingPrefs.setTrackingEnabled(true)
            trackingPrefs.setTrackingPaused(false)
            try {
                ContextCompat.startForegroundService(
                    context,
                    TrackingRecordingService.getStartIntent(context)
                )
            } catch (e: RuntimeException) {
                trackingPrefs.setTrackingEnabled(false)
                throw e
            }
        } else {
            trackingPrefs.setTrackingEnabled(false)
            context.startService(TrackingRecordingService.getStopIntent(context))
        }
    }

    suspend fun pauseTracking() {
        trackingPrefs.setTrackingPaused(true)
        try {
            context.startService(TrackingRecordingService.getPauseIntent(context))
        } catch (e: RuntimeException) {
            trackingPrefs.setTrackingPaused(false)
            throw e
        }
    }

    suspend fun resumeTracking() {
        requireTrackingPermission()
        trackingPrefs.setTrackingEnabled(true)
        trackingPrefs.setTrackingPaused(false)
        try {
            ContextCompat.startForegroundService(
                context,
                TrackingRecordingService.getResumeIntent(context)
            )
        } catch (e: RuntimeException) {
            trackingPrefs.setTrackingPaused(true)
            throw e
        }
    }

    private fun requireTrackingPermission() {
        check(permissionSource.hasLocationPermission()) {
            "Foreground and background location permissions are required for tracking"
        }
    }
}
