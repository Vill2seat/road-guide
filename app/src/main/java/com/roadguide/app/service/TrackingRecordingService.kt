package com.roadguide.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
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
import com.roadguide.app.data.source.LocationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.roadguide.app.data.local.settings.TrackingPrefs
import java.time.Instant
import java.time.ZoneId

/**
 * Foreground service for continuous location tracking.
 * Collects locations at the configured interval via FusedLocationProviderClient
 * and saves to Room.
 */
class TrackingRecordingService : Service() {

    private var trackingJob: Job? = null
    private var routeId: Long? = null
    private var currentRoute: Route? = null
    private var lastLocation: android.location.Location? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var isRecording = false
    private var lastLocationTime = 0L

    // Repositories (initialized via DataProvider)
    private var locationPointRepository: LocationPointRepository? = null
    private var routeRepository: RouteRepository? = null

    // Location source
    private var locationSource: LocationSource? = null

    override fun onCreate() {
        super.onCreate()
        // Initialize repositories via DataProvider
        val dataProvider = DataProvider.getInstance(this)
        val routeDao: RouteDao = dataProvider.routeDao
        val locationPointDao: LocationPointDao = dataProvider.locationPointDao
        routeRepository = RouteRepositoryImpl(routeDao)
        locationPointRepository = LocationPointRepositoryImpl(locationPointDao)

        // Initialize location source
        locationSource = LocationSource(this)
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

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch {
            stopRecording()
        }
        serviceJob.cancel()
    }

    private suspend fun startRecording() {
        if (isRecording) return

        val locationSource = locationSource ?: return

        // Check permissions before starting
        if (!locationSource.hasLocationPermission()) {
            // Cannot start without permissions
            return
        }

        // 1. Create a new route in Room
        val now = System.currentTimeMillis()
        val date = Instant.ofEpochMilli(now)
            .atZone(ZoneId.systemDefault())
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

        // 2. Read tracking interval from prefs
        val trackingPrefs = TrackingPrefs(this)
        val settings = trackingPrefs.preferencesFlow.first()
        val intervalSeconds = settings.intervalSeconds.coerceAtLeast(30).coerceAtMost(3600).toLong()

        // 3. Start location collection
        isRecording = true
        lastLocationTime = System.currentTimeMillis()

        // Start location updates
        locationSource.requestLocationUpdates(intervalSeconds)

        // Collect locations from the flow
        trackingJob = serviceScope.launch {
            locationSource.locationUpdates.collect { loc ->
                if (!isRecording) return@collect
                saveLocationPoint(loc)
                lastLocationTime = System.currentTimeMillis()
            }
        }

        // Start foreground service
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            0
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

        locationPointRepository?.insert(point)

        // Update route distance using last known location
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
            routeRepository?.update(updatedRoute)
        }

        lastLocation = location
    }

    private suspend fun calculateDistanceMeters(
        lat1: Double, lon1: Double, lat2: Double, lon2: Double
    ): Double {
        val R = 6371000.0 // Earth radius in meters
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

        // Stop location updates
        locationSource?.removeLocationUpdates()

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

    private fun createNotification(): android.app.Notification {
        return android.app.Notification.Builder(this, "tracking_channel")
            .setContentTitle("Road Guide")
            .setContentText("Запись локации...")
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
