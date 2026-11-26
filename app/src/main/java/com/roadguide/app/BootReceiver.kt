package com.roadguide.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.service.TrackingRecordingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val trackingPrefs = TrackingPrefs(appContext)
                val permissionSource = PermissionSource(appContext)

                // Use firstOrNull instead of first to avoid NoSuchElementException on empty DataStore
                val preferences = trackingPrefs.preferencesFlow.take(1).firstOrNull()
                    ?: run {
                        Log.d(TAG, "No preferences found after reboot; not restoring service")
                        pendingResult.finish()
                        return@launch
                    }
                if (!preferences.trackingEnabled || preferences.trackingPaused) {
                    Log.d(TAG, "Tracking is disabled or paused; not restoring service")
                    return@launch
                }
                if (!permissionSource.hasLocationPermission()) {
                    Log.w(TAG, "Location permissions are missing; not restoring service")
                    return@launch
                }
                ContextCompat.startForegroundService(
                    context,
                    TrackingRecordingService.getStartIntent(context)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore tracking after reboot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
