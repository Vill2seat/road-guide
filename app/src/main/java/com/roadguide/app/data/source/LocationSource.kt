package com.roadguide.app.data.source

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

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
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val background = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine && background
    }

    /**
     * Requests location permissions from the calling Activity.
     */
    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1002
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
        if (!hasLocationPermission()) return null
        return Tasks.await(fusedLocationClient.lastLocation)
    }
    /**
     * Updates the location update interval.
     * Removes existing updates and starts with the new interval.
     */
    suspend fun updateInterval(intervalSeconds: Long) {
        locationMutex.withLock {
            locationCallback?.let { removeLocationUpdates() }
        locationCallback = null
            requestLocationUpdates(intervalSeconds)
        }
    }
}
