package com.roadguide.app.data.source

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.google.android.gms.location.DetectedActivity
import android.util.Log
import com.google.android.gms.location.*
import com.roadguide.app.data.local.entity.ActivityType
import kotlinx.coroutines.flow.Flow

/**
 * Wraps Google's ActivityRecognition API for detecting user activities.
 * Activities: STILL, WALKING, RUNNING, ON_BICYCLE, IN_VEHICLE, ON_FOOT, UNKNOWN
 */
class ActivityRecognitionSource(private val context: Context) {

    private val activityClient: ActivityRecognitionClient = ActivityRecognition.getClient(context)
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
        val activityIntent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = "com.roadguide.app.ACTION_ACTIVITY_UPDATE"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        activityClient.requestActivityUpdates(60_000, pendingIntent)
            .addOnSuccessListener { Log.d("ARSource", "registered") }
            .addOnFailureListener { Log.w("ARSource", "failed", it) }
        context.registerReceiver(receiver, IntentFilter("com.roadguide.app.ACTION_ACTIVITY_UPDATE"))
    }

    fun stopUpdates() {
        val activityIntent = Intent(context, ActivityRecognitionReceiver::class.java).apply {
            action = "com.roadguide.app.ACTION_ACTIVITY_UPDATE"
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        activityClient.removeActivityUpdates(pendingIntent)
            .addOnSuccessListener { Log.d("ARSource", "removed") }
            .addOnFailureListener { Log.w("ARSource", "failed", it) }
        try { context.unregisterReceiver(receiver) } catch (_: IllegalArgumentException) {}
    }
}

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {}
}