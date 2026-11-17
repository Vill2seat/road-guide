package com.roadguide.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.location.Location
import androidx.core.app.ServiceCompat
import com.roadguide.app.data.local.DataProvider
import com.roadguide.app.data.local.dao.LocationPointDao
import com.roadguide.app.data.local.dao.RouteDao
import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.LocationPointRepositoryImpl
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.data.repository.RouteRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Foreground service for continuous location tracking.
 * Collects locations at the configured interval and saves to Room.
 */
class TrackingRecordingService : Service() {

    private var trackingJob: Job? = null
    private var routeId: Long? = null
    private var currentRoute: Route? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var isRecording = false
    private var lastLocationTime = 0L

    // Repositories (initialized via DataProvider)
    private var locationPointRepository: LocationPointRepository? = null
    private var routeRepository: RouteRepository? = null

    override fun onCreate() {
        super.onCreate()
        // Initialize repositories via DataProvider
        val dataProvider = DataProvider.getInstance(this)
        val routeDao: RouteDao = dataProvider.routeDao
        val locationPointDao: LocationPointDao = dataProvider.locationPointDao
        routeRepository = RouteRepositoryImpl(routeDao)
        locationPointRepository = LocationPointRepositoryImpl(locationPointDao)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                serviceScope.launch { startRecording() }
            }
            ACTION_STOP_RECORDING -> {
                serviceScope.launch { stopRecording() }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun startRecording() {
        if (isRecording) return

        // 1. Create a new route in Room
        val now = System.currentTimeMillis()
        val date = java.time.Instant.ofEpochMilli(now)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        currentRoute = Route(
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

        routeId = routeRepository?.insert(currentRoute!!) ?: 0

        // 2. Start location collection
        isRecording = true
        lastLocationTime = System.currentTimeMillis()

        // Check tracking settings interval
        val intervalSeconds = 60 // DEFAULT - would come from TrackingPrefs

        trackingJob = serviceScope.launch {
            while (isRecording) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastLocationTime >= intervalSeconds * 1000) {
                    emitLocationPoint()
                    lastLocationTime = currentTime
                }
                kotlinx.coroutines.delay(100) // Poll interval
            }
        }

        // Start foreground service
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            ""
        )
    }

    private suspend fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        trackingJob?.cancel()

        // Finalize the route
        currentRoute?.let { route ->
            val updatedRoute = route.copy(
                status = RouteStatus.COMPLETED,
                endTime = System.currentTimeMillis()
            )
            currentRoute = updatedRoute
            routeRepository?.update(updatedRoute)
        }

        // Stop foreground service
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private suspend fun emitLocationPoint() {
        // In a real implementation, get the most recent location from the location client
        // For now, emit a mock location
        val location = Location("mock")
        location.latitude = 55.7558 // Example: Moscow
        location.longitude = 37.6173
        location.time = System.currentTimeMillis()

        val point = LocationPoint(
            id = 0,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = location.time,
            accuracyMeters = 10f,
            speedMetersPerSecond = location.speed?.toFloat(),
            bearing = location.bearing?.toFloat(),
            altitude = location.altitude,
            provider = "fused",
            isMock = true,
            routeId = routeId
        )

        locationPointRepository?.insert(point)
    }

    private fun createNotification(): android.app.Notification {
        return android.app.Notification.Builder(this, "tracking_channel")
            .setContentTitle("Road Guide")
            .setContentText("Recording location...")
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setOngoing(true)
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
