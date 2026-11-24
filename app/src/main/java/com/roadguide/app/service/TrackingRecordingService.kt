package com.roadguide.app.service

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.roadguide.app.R
import com.roadguide.app.data.local.entity.LocationPoint
import com.roadguide.app.data.local.entity.PrivacyZone
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteSegment
import com.roadguide.app.data.local.entity.RouteStatus
import com.roadguide.app.data.local.entity.RouteType
import com.roadguide.app.data.local.entity.Stay
import com.roadguide.app.data.repository.EnvironmentalSampleRepository
import com.roadguide.app.data.repository.LocationPointRepository
import com.roadguide.app.data.repository.PrivacyZoneRepository
import com.roadguide.app.data.repository.RouteRepository
import com.roadguide.app.data.repository.RouteSegmentRepository
import com.roadguide.app.data.repository.StayRepository
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.domain.model.GeofenceState
import com.roadguide.app.domain.usecase.impl.RouteSegmentationEngine
import com.roadguide.app.domain.usecase.impl.PrivacyScope
import com.roadguide.app.domain.usecase.impl.PrivacyZonePolicy
import com.roadguide.app.domain.usecase.AggregateDailyExposure
import com.roadguide.app.mapper.LocationPointMapper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    @Inject lateinit var aggregateDailyExposure: AggregateDailyExposure
    @Inject lateinit var privacyZoneRepository: PrivacyZoneRepository
    @Inject lateinit var routeSegmentRepository: RouteSegmentRepository
    @Inject lateinit var stayRepository: StayRepository

    private var trackingJob: Job? = null
    private var routeId: Long? = null
    private var currentRoute: Route? = null
    private var lastLocation: android.location.Location? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var isPaused = false
    private var isRecording = false
    private var lastLocationTime = 0L

    /** Состояние геофенсинга для UI */
    private val _geofenceState = MutableStateFlow<GeofenceState>(GeofenceState())
    val geofenceState: StateFlow<GeofenceState> = _geofenceState.asStateFlow()

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
            ACTION_START_RECORDING -> startRecordingService(startId)
            ACTION_STOP_RECORDING -> {
                serviceScope.launch {
                    try {
                        stopRecording()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf(startId)
                    } catch (e: Exception) {
                        Log.e("TrackingService", "Failed to stop location recording", e)
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf(startId)
                    }
                }
            }
            ACTION_PAUSE_RECORDING -> serviceScope.launch {
                try {
                    if (isRecording) pauseTracking() else stopSelf(startId)
                } catch (e: Exception) {
                    Log.e("TrackingService", "Failed to pause location recording", e)
                }
            }
            ACTION_RESUME_RECORDING -> startRecordingService(startId, resume = true)
            null -> restoreTrackingAfterProcessRestart(startId)
        }
        return START_STICKY
    }

    private fun startRecordingService(startId: Int, resume: Boolean = false) {
        if (!locationSource.hasLocationPermission()) {
            failStart(startId, "Cannot start: location permissions are missing")
            return
        }

        try {
            updateForegroundNotification()
        } catch (e: SecurityException) {
            failStart(startId, "Cannot enter location foreground service", e)
            return
        }

        serviceScope.launch {
            try {
                if (resume && currentRoute != null) {
                    resumeTracking()
                } else if (currentRoute != null && isPaused) {
                    resumeTracking()
                } else {
                    startRecording()
                }
            } catch (e: Exception) {
                Log.e("TrackingService", "Failed to start location recording", e)
                if (e is SecurityException) {
                    try {
                        trackingPrefs.setTrackingEnabled(false)
                    } catch (preferenceError: Exception) {
                        Log.e("TrackingService", "Failed to clear tracking preference", preferenceError)
                    }
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
    }

    private fun failStart(startId: Int, message: String, error: Exception? = null) {
        if (error == null) Log.e("TrackingService", message)
        else Log.e("TrackingService", message, error)
        serviceScope.launch {
            try {
                trackingPrefs.setTrackingEnabled(false)
            } catch (preferenceError: Exception) {
                Log.e("TrackingService", "Failed to clear tracking preference", preferenceError)
            }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
        }
    }

    private fun restoreTrackingAfterProcessRestart(startId: Int) {
        if (!locationSource.hasLocationPermission()) {
            failStart(startId, "Cannot restore: location permissions are missing")
            return
        }

        try {
            updateForegroundNotification()
        } catch (e: SecurityException) {
            failStart(startId, "Cannot restore location foreground service", e)
            return
        }

        serviceScope.launch {
            try {
                val preferences = trackingPrefs.preferencesFlow.first()
                if (preferences.trackingEnabled && !preferences.trackingPaused) {
                    startRecording()
                } else {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf(startId)
                }
            } catch (e: Exception) {
                Log.e("TrackingService", "Failed to restore tracking state", e)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
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
            throw SecurityException("Location permissions are no longer granted")
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

        val existingRoute = routeRepository.observeByStatus(RouteStatus.IN_PROGRESS)
            .first()
            .maxByOrNull(Route::startTime)
            ?: routeRepository.observeByStatus(RouteStatus.PAUSED)
                .first()
                .maxByOrNull(Route::startTime)
        if (existingRoute != null) {
            val resumedRoute = existingRoute.copy(status = RouteStatus.IN_PROGRESS)
            currentRoute = resumedRoute
            routeRepository.update(resumedRoute)
            routeId = existingRoute.id
        } else {
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

            val insertedRouteId = routeRepository.insert(newRoute)
            currentRoute = newRoute.copy(id = insertedRouteId)
            routeId = insertedRouteId
        }

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
        updateForegroundNotification()
    }

    private suspend fun saveLocationPoint(location: android.location.Location) {
        val routeId = this.routeId ?: return

        val activeZones: List<PrivacyZone>
        try {
            activeZones = privacyZoneRepository.observeActiveZones().first()
        } catch (e: Exception) {
            lastLocation = null
            Log.e("TrackingService", "Privacy policy unavailable; dropping location sample", e)
            return
        }

        val insideZones = activeZones.filter { zone ->
            isInsidePrivacyZone(location.latitude, location.longitude, zone)
        }
        _geofenceState.update {
            it.copy(
                currentZones = insideZones,
                isInsideProtectedZone = insideZones.isNotEmpty()
            )
        }
        val privacyDecision = PrivacyZonePolicy.decisionAt(
            location.latitude,
            location.longitude,
            activeZones
        )

        // Если excludeFromHistory — не сохраняем точку вообще
        if (privacyDecision.excludeFromHistory) {
            lastLocation = null
            Log.d("TrackingService", "Location sample omitted by history privacy policy")
            return
        }

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
            routeId = routeId,
            excludeFromExposure = privacyDecision.excludeFromExposure
        )

        locationPointRepository.insert(point)
        Log.d(
            "TrackingService",
            "point saved, routeId=$routeId, excludeFromExposure=${privacyDecision.excludeFromExposure}"
        )

        // Adaptive GPS interval based on speed
        val speed = location.speed ?: 0f
        if (speed < 0.3f) {
            locationSource.updateInterval(300)
        } else if (speed <= 1.0f) {
            locationSource.updateInterval(30)
        } else {
            locationSource.updateInterval(10)
        }

        if (!privacyDecision.excludeFromExposure) environmentalSampleRepository.let { repo ->
            serviceScope.launch {
                try {
                    repo.fetchAndSave(
                        location.latitude,
                        location.longitude,
                        location.time
                    )
                } catch (e: Exception) {
                    Log.w("TrackingService", "env fetch failed", e)
                }
            }
        }

        currentRoute?.let { route ->
            val distanceDelta = lastLocation?.let { prev ->
                val gap = location.time - prev.time
                if (
                    gap in 1..RouteSegmentationEngine.MAX_GPS_GAP_MS &&
                    !PrivacyZonePolicy.excludesPath(
                        prev.latitude,
                        prev.longitude,
                        location.latitude,
                        location.longitude,
                        activeZones,
                        PrivacyScope.HISTORY
                    )
                ) {
                    calculateDistanceMeters(
                        prev.latitude, prev.longitude,
                        location.latitude, location.longitude
                    )
                } else 0.0
            } ?: 0.0
            val updatedDistance = route.distanceMeters + distanceDelta
            val updatedRoute = route.copy(
                distanceMeters = updatedDistance,
                totalDurationSeconds = ((System.currentTimeMillis() - route.startTime) / 1000)
                    .coerceAtLeast(0)
            )
            currentRoute = updatedRoute
            routeRepository.update(updatedRoute)
        }

        lastLocation = location
    }

    private fun calculateDistanceMeters(
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

    /**
     * Проверяет, находится ли точка внутри приват-зоны (геофенсинг).
     * Использует формулу Haversine для расчёта расстояния между двумя точками на сфере.
     */
    private fun isInsidePrivacyZone(lat: Double, lon: Double, zone: PrivacyZone): Boolean {
        val distance = calculateDistanceMeters(lat, lon, zone.centerLatitude, zone.centerLongitude)
        return distance <= zone.radiusMeters
    }

    private suspend fun stopRecording() {
        isRecording = false
        trackingJob?.cancel()
        trackingJob = null

        locationSource.removeLocationUpdates()

        // Unsubscribe from activity recognition
        try {
            activityRecognitionSource.stopUpdates()
        } catch (e: Exception) {
            // Ignore - might not have been subscribed
        }

        currentRoute?.let { route ->
            val activeZones = privacyZoneRepository.observeActiveZones().first()
            val storedPoints = locationPointRepository.observeByRoute(route.id).first()
            val points = PrivacyZonePolicy.filterPoints(
                storedPoints,
                activeZones,
                PrivacyScope.HISTORY
            )
                .map { LocationPointMapper.toSummary(it) }
                .filter { it.timestamp >= route.startTime }
            val analysis = RouteSegmentationEngine.analyzePoints(
                routeId = route.id,
                routeStartTime = route.startTime,
                points = points,
                privacyZones = activeZones
            )
            persistRouteAnalysis(route.id, analysis)

            val updatedRoute = route.copy(
                status = RouteStatus.COMPLETED,
                endTime = points.lastOrNull()?.timestamp ?: route.startTime,
                startTime = points.firstOrNull()?.timestamp ?: route.startTime,
                distanceMeters = PrivacyZonePolicy.recordedDistanceMeters(
                    storedPoints,
                    activeZones
                ),
                totalDurationSeconds = PrivacyZonePolicy.recordedDurationSeconds(
                    storedPoints,
                    activeZones
                )
            )
            currentRoute = updatedRoute
            routeRepository.update(updatedRoute)

            try {
                aggregateDailyExposure.aggregate(route.date)
            } catch (e: Exception) {
                Log.w("TrackingService", "Daily aggregation failed", e)
            }
        }

        currentRoute = null
        routeId = null
        lastLocation = null
        isPaused = false
    }

    private suspend fun persistRouteAnalysis(
        routeId: Long,
        analysis: RouteSegmentationEngine.Analysis
    ) {
        val existingSegments = routeSegmentRepository.observeByRoute(routeId).first()
            .associateBy { it.startTime }
        analysis.segments.forEach { segment ->
            val existing = existingSegments[segment.startTime]
            if (existing == null) {
                routeSegmentRepository.insert(segment)
            } else {
                routeSegmentRepository.update(segment.copy(id = existing.id))
            }
        }

        val existingStays = stayRepository.observeByRoute(routeId).first()
            .associateBy { it.startTime }
        analysis.stays.forEach { stay ->
            val existing = existingStays[stay.startTime]
            if (existing == null) {
                stayRepository.insert(stay)
            } else {
                stayRepository.update(stay.copy(id = existing.id))
            }
        }
    }

    private suspend fun pauseTracking() {
        if (!isRecording) return
        isPaused = true
        isRecording = false
        trackingJob?.cancel()
        trackingJob = null
        locationSource.removeLocationUpdates()
        currentRoute?.let { route ->
            val pausedRoute = route.copy(status = RouteStatus.PAUSED)
            currentRoute = pausedRoute
            routeRepository.update(pausedRoute)
        }
        updateForegroundNotification()
    }

    private suspend fun resumeTracking() {
        if (!locationSource.hasLocationPermission()) {
            throw SecurityException("Location permissions are no longer granted")
        }
        if (isRecording) return
        isPaused = false
        isRecording = true
        currentRoute?.let { route ->
            val resumedRoute = route.copy(status = RouteStatus.IN_PROGRESS)
            currentRoute = resumedRoute
            routeRepository.update(resumedRoute)
        }
        locationSource.requestLocationUpdates(intervalSeconds = 60)
        trackingJob = serviceScope.launch {
            locationSource.locationUpdates.collect { loc ->
                if (!isRecording) return@collect
                saveLocationPoint(loc)
                lastLocationTime = System.currentTimeMillis()
            }
        }
        updateForegroundNotification()
    }

    private fun updateForegroundNotification() {
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            createNotification(),
            serviceType
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
        const val ACTION_PAUSE_RECORDING = "com.roadguide.app.ACTION_PAUSE_RECORDING"
        const val ACTION_RESUME_RECORDING = "com.roadguide.app.ACTION_RESUME_RECORDING"

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

        fun getPauseIntent(context: Context): Intent {
            return Intent(context, TrackingRecordingService::class.java).apply {
                action = ACTION_PAUSE_RECORDING
            }
        }

        fun getResumeIntent(context: Context): Intent {
            return Intent(context, TrackingRecordingService::class.java).apply {
                action = ACTION_RESUME_RECORDING
            }
        }
    }
}
