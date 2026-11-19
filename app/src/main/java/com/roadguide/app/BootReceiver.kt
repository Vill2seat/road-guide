package com.roadguide.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, checking tracking state")
            // Check if tracking was enabled and restart service
            // This will be handled by the MainActivity's PermissionGate logic
            // which checks permissionSource.hasCompletedOnboarding()
        }
    }
}