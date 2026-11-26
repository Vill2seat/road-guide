package com.roadguide.app.data.source

import android.app.PendingIntent
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.google.android.gms.location.DetectedActivity
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.roadguide.app.data.local.entity.ActivityType
import kotlinx.coroutines.flow.Flow

/**
 * Wraps Google's ActivityRecognition API for detecting user activities.
 * Activities: STILL, WALKING, RUNNING, ON_BICYCLE, IN_VEHICLE, ON_FOOT, UNKNOWN
 */
class ActivityRecognitionSource(private val context: Context) {

    private val activityClient: ActivityRecognitionClient = ActivityRecognition.getClient(context)
    private var receiverRegistered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val result = ActivityRecognitionResult.extractResult(intent ?: return)
            if (result == null) return
            val mostLikely = result.mostProbableActivity
            val activityType = when {
                mostLikely.type == DetectedActivity.STILL -> ActivityType.STATIONARY
                mostLikely.type == DetectedActivity.WALKING -> ActivityType.WALKING
                mostLikely.type == DetectedActivity.RUNNING -> ActivityType.RUNNING
                mostLikely.type == DetectedActivity.ON_BICYCLE -> ActivityType.CYCLING
                mostLikely.type == DetectedActivity.IN_VEHICLE -> ActivityType.IN_VEHICLE
                mostLikely.type == DetectedActivity.ON_FOOT -> ActivityType.WALKING
                else -> ActivityType.UNKNOWN
            }
            Log.d("ARSource", "Detected: $activityType")
        }
    }

    fun startUpdates(onActivityChanged: (ActivityType) -> Unit) {
        if (!hasPermission()) {
            Log.i("ARSource", "Activity recognition permission is missing; using speed classification")
            return
        }
        val activityIntent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = "com.roadguide.app.ACTION_ACTIVITY_UPDATE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            activityClient.requestActivityUpdates(60_000, pendingIntent)
                .addOnSuccessListener { Log.d("ARSource", "registered") }
                .addOnFailureListener { Log.w("ARSource", "failed", it) }
            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter("com.roadguide.app.ACTION_ACTIVITY_UPDATE"),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        } catch (e: SecurityException) {
            Log.w("ARSource", "Activity recognition permission was revoked", e)
        }
    }

    fun stopUpdates() {
        val activityIntent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = "com.roadguide.app.ACTION_ACTIVITY_UPDATE"
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        if (hasPermission()) {
            try {
                activityClient.removeActivityUpdates(pendingIntent)
                    .addOnSuccessListener { Log.d("ARSource", "removed") }
                    .addOnFailureListener { Log.w("ARSource", "failed", it) }
            } catch (e: SecurityException) {
                Log.w("ARSource", "Activity recognition permission was revoked", e)
            }
        }
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                Log.w("ARSource", "Activity receiver was already unregistered", e)
            } finally {
                receiverRegistered = false
            }
        }
    }

    private fun hasPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
    }
}

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val result = ActivityRecognitionResult.extractResult(intent ?: return) ?: return
        val mostLikely = result.mostProbableActivity
        val activityType = when {
            mostLikely.type == DetectedActivity.STILL -> ActivityType.STATIONARY
            mostLikely.type == DetectedActivity.WALKING -> ActivityType.WALKING
            mostLikely.type == DetectedActivity.RUNNING -> ActivityType.RUNNING
            mostLikely.type == DetectedActivity.ON_BICYCLE -> ActivityType.CYCLING
            mostLikely.type == DetectedActivity.IN_VEHICLE -> ActivityType.IN_VEHICLE
            mostLikely.type == DetectedActivity.ON_FOOT -> ActivityType.WALKING
            else -> ActivityType.UNKNOWN
        }
        Log.d("ARReceiver", "Detected: $activityType (${mostLikely.type})")
    }
}