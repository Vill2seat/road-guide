package com.roadguide.app.data.source

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Source of location updates from Android FusedLocationProviderClient.
 *
 * Provides:
 * - [requestLocationUpdates] — start receiving location updates
 * - [getLastLocation] — get last known location
 * - [locationUpdates] — SharedFlow of Location objects for collection
 * - [hasLocationPermission] — check ACCESS_FINE_LOCATION / ACCESS_BACKGROUND_LOCATION
 *
 * LocationRequest parameters match tracking requirements:
 * - Priority.PRIORITY_HIGH_ACCURACY for precise GPS
 * - Interval from TrackingPrefs (default 60s)
 * - Smallest displacement 5m to filter noise
 */
class LocationSource(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val _locationUpdates = MutableSharedFlow<Location>(replay = 1, extraBufferCapacity = 10)
    val locationUpdates: SharedFlow<Location> = _locationUpdates

    private val locationMutex = Mutex()
    private var locationCallback: LocationCallback? = null
    private var isUpdating = false

    /**
     * Checks if both foreground and background location permissions are granted.
     */
    fun hasLocationPermission(): Boolean {
        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        return hasForegroundLocationPermission() && background
    }

    /**
     * Checks foreground location access, which is sufficient for a user-initiated route lookup.
     */
    fun hasForegroundLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Starts receiving location updates.
     * Interval defaults to 60 seconds; smallestDisplacement 5 meters.
     */
    @Suppress("MissingPermission")
    suspend fun requestLocationUpdates(intervalSeconds: Long = 60) {
        locationMutex.withLock {
            if (isUpdating) return

            if (!hasLocationPermission()) {
                throw IllegalStateException("Location permissions not granted")
            }

            val locationRequest = LocationRequest.create().apply {
                interval = intervalSeconds * 1000
                fastestInterval = (intervalSeconds * 1000 / 2).coerceAtLeast(10_000)
                priority = Priority.PRIORITY_HIGH_ACCURACY
            }

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        try {
                            _locationUpdates.tryEmit(loc)
                        } catch (e: Exception) {
                            // Flow buffer full, skip
                        }
                    }
                }
            }

            locationCallback = callback

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
            isUpdating = true
        }
    }

    /**
     * Stops receiving location updates.
     */
    suspend fun removeLocationUpdates() {
        locationMutex.withLock {
            if (!isUpdating) return
            locationCallback?.let { cb ->
                fusedLocationClient.removeLocationUpdates(cb)
                locationCallback = null
            }
            isUpdating = false
        }
    }

    /**
     * Gets last known location (may be null if never collected).
     */
    @Suppress("MissingPermission")
    suspend fun getLastLocation(): Location? {
        if (!hasForegroundLocationPermission()) return null
        return Tasks.await(fusedLocationClient.lastLocation)
    }

    /**
     * Requests a fresh foreground location. The blocking Tasks bridge runs off the main thread.
     */
    @Suppress("MissingPermission")
    suspend fun getCurrentLocation(): Location? = withContext(Dispatchers.IO) {
        if (!hasForegroundLocationPermission()) return@withContext null
        val cancellation = CancellationTokenSource()
        try {
            Tasks.await(
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellation.token
                ),
                15,
                TimeUnit.SECONDS
            )
        } finally {
            cancellation.cancel()
        }
    }

    /**
     * Updates the location update interval.
     * Removes existing updates and starts with the new interval.
     */
    suspend fun updateInterval(intervalSeconds: Long) {
        removeLocationUpdates()
        requestLocationUpdates(intervalSeconds)
    }
}
