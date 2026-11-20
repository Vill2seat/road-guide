package com.roadguide.app.service

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.roadguide.app.R
import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import com.roadguide.app.data.repository.EnvironmentalSampleRepository
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.data.source.LocationSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.source.ActivityRecognitionSource
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class TrackingRecordingService : Service() {

    @Inject lateinit var locationPointRepository: LocationPointRepository
    @Inject lateinit var routeRepository: RouteRepository
    @Inject lateinit var environmentalSampleRepository: EnvironmentalSampleRepository
    @Inject lateinit var locationSource: LocationSource
    @Inject lateinit var trackingPrefs: TrackingPrefs
    @Inject lateinit var activityRecognitionSource: ActivityRecognitionSource

    private var trackingJob: Job? = null
    private var routeId: Long? = null
    private var currentRoute: Route? = null
    private var lastLocation: android.location.Location? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var isPaused = false
    private var isRecording = false
    private var lastLocationTime = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tracking_channel",
                "Road Guide Tracking",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Used for background location tracking notifications"
            channel.enableLights(false)
            channel.enableVibration(false)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                serviceScope.launch {
                    if (locationSource.hasLocationPermission()) {
                        startForeground(NOTIFICATION_ID, createNotification())
                        startRecording()
                    }
                }
            }
            ACTION_STOP_RECORDING -> {
                serviceScope.launch { stopRecording() }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        serviceJob.cancel()
    }

    private suspend fun startRecording() {
        if (isRecording) return

        val locationSource = this.locationSource

        if (!locationSource.hasLocationPermission()) {
            // Cannot start recording without location permissions
            return
        }

        // Subscribe to activity recognition updates
        try {
            activityRecognitionSource.startUpdates { activityType ->
                // Activity type detected - will be used for next segment classification
                // Store for use in saveLocationPoint if needed
            }
        } catch (e: Exception) {
            // Graceful degradation - Google Play Services not available
            // Continue with speed-based classification only
            // Log warning but don't crash
        }

        val now = System.currentTimeMillis()
        val date = Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        val newRoute = Route(
            id = 0,
            startTime = now,
            endTime = null,
            distanceMeters = 0.0,
            status = RouteStatus.IN_PROGRESS,
            routeType = RouteType.DAILY_AUTO,
            confidence = 0.0,
            date = date,
            totalDurationSeconds = 0,
            calculationVersion = 1
        )

        currentRoute = newRoute
        routeId = routeRepository.insert(newRoute)

        val intervalSeconds: Long = try {
            withTimeout(5000) {
                trackingPrefs.preferencesFlow.first().intervalSeconds.coerceAtLeast(30).coerceAtMost(3600).toLong()
            }
        } catch (e: Exception) {
            60L
        }

        isRecording = true
        lastLocationTime = System.currentTimeMillis()

        locationSource.requestLocationUpdates(intervalSeconds)

        trackingJob = serviceScope.launch {
            locationSource.locationUpdates.collect { loc ->
                if (!isRecording) return@collect
                saveLocationPoint(loc)
                lastLocationTime = System.currentTimeMillis()
            }
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            4
        )
    }

    private suspend fun saveLocationPoint(location: android.location.Location) {
        val routeId = this.routeId ?: return

        val point = LocationPoint(
            id = 0,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = location.time,
            accuracyMeters = location.accuracy,
            speedMetersPerSecond = location.speed.coerceAtLeast(0f),
            bearing = location.bearing.coerceAtLeast(0f),
            altitude = location.altitude.coerceAtLeast(0.0),
            provider = location.provider ?: "fused",
            isMock = false,
            routeId = routeId
        )

        locationPointRepository.insert(point)
        Log.d("TrackingService", "point saved, routeId=$routeId")

        environmentalSampleRepository.let { repo ->
            serviceScope.launch {
                try {
                    repo.fetchAndSave(location.latitude, location.longitude)
                } catch (e: Exception) {
                    Log.w("TrackingService", "env fetch failed", e)
                }
            }
        }

        currentRoute?.let { route ->
            val distanceDelta = lastLocation?.let { prev ->
                calculateDistanceMeters(
                    prev.latitude, prev.longitude,
                    location.latitude, location.longitude
                )
            } ?: 0.0
            val updatedDistance = route.distanceMeters + distanceDelta
            val updatedRoute = route.copy(
                distanceMeters = updatedDistance,
                totalDurationSeconds = (System.currentTimeMillis() - route.startTime) / 1000
            )
            currentRoute = updatedRoute
            routeRepository.update(updatedRoute)
        }

        lastLocation = location
    }

    private suspend fun calculateDistanceMeters(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double
    ): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    private suspend fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        trackingJob?.cancel()

        locationSource.removeLocationUpdates()

        // Unsubscribe from activity recognition
        try {
            activityRecognitionSource.stopUpdates()
        } catch (e: Exception) {
            // Ignore - might not have been subscribed
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)

        currentRoute?.let { route ->
            val updatedRoute = route.copy(
                status = RouteStatus.COMPLETED,
                endTime = System.currentTimeMillis()
            )
            currentRoute = updatedRoute
            routeRepository.update(updatedRoute)
        }

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private suspend fun pauseTracking() {
        isPaused = true
        isRecording = false
        locationSource.removeLocationUpdates()
        locationSource.updateInterval(300)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            4
        )
    }

    private suspend fun resumeTracking() {
        isPaused = false
        isRecording = true
        locationSource.requestLocationUpdates(intervalSeconds = 60)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            4
        )
    }

    private fun createNotification(): android.app.Notification {
        val text = when {
            isPaused -> getString(R.string.notification_paused)
            isRecording -> getString(R.string.notification_recording)
            else -> getString(R.string.notification_stopped)
        }
        return android.app.Notification.Builder(this, "tracking_channel")
            .setContentTitle("Road Guide")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setOngoing(isRecording)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_RECORDING = "com.roadguide.app.ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.roadguide.app.ACTION_STOP_RECORDING"

        fun getStartIntent(context: Context): Intent {
            return Intent(context, TrackingRecordingService::class.java).apply {
                action = ACTION_START_RECORDING
            }
        }

        fun getStopIntent(context: Context): Intent {
            return Intent(context, TrackingRecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
        }
    }
}
