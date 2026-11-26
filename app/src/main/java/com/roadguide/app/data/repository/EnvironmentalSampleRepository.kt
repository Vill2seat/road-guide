package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.EnvironmentalSampleDao
import com.roadguide.app.data.local.EnvironmentalValidator
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.entity.DataQuality
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.source.EnvironmentalDataFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log

interface EnvironmentalSampleRepository {
    suspend fun insert(sample: EnvironmentalSample): Long

    fun observeAll(): Flow<List<EnvironmentalSample>>

    fun getLatest(): Flow<EnvironmentalSample?>

    suspend fun deleteOlderThan(cutoff: Long): Int

    /**
     * Fetches environmental data for given coordinates from remote sources,
     * validates quality, and persists to local database.
     * Returns the saved sample or null if no data could be fetched.
     */
    suspend fun fetchAndSave(
        lat: Double,
        lon: Double,
        requestedTime: Long = System.currentTimeMillis()
    ): EnvironmentalSample?

    /**
     * Returns true if internet was available at least once today.
     */
    suspend fun wasInternetAvailableToday(): Boolean
}

class EnvironmentalSampleRepositoryImpl(
    private val dao: EnvironmentalSampleDao,
    private val fetcher: EnvironmentalDataFetcher,
    private val trackingPrefs: TrackingPrefs,
    private val validator: EnvironmentalValidator
) :
    EnvironmentalSampleRepository {

    private val fetchMutex = Mutex()

    override suspend fun insert(sample: EnvironmentalSample): Long = dao.insert(sample)

    override fun observeAll(): Flow<List<EnvironmentalSample>> = dao.observeAll()

    override fun getLatest(): Flow<EnvironmentalSample?> = dao.observeLatest()

    override suspend fun deleteOlderThan(cutoff: Long): Int = dao.deleteOlderThan(cutoff)

    override suspend fun fetchAndSave(
        lat: Double,
        lon: Double,
        requestedTime: Long
    ): EnvironmentalSample? = fetchMutex.withLock {
        val now = System.currentTimeMillis()
        val cached = dao.findNearest(
            lat = lat,
            lon = lon,
            time = requestedTime,
            delta = CACHE_COORDINATE_DELTA_DEGREES,
            window = CACHE_SAMPLE_TIME_WINDOW_MS
        ).take(1).firstOrNull()
        if (cached != null && now - cached.fetchedAt in 0L..CACHE_MAX_AGE_MS) {
            val quality = validator.validate(
                sample = cached,
                requestLat = lat,
                requestLon = lon,
                requestTime = requestedTime,
                now = now
            )
            if (quality != null && quality != DataQuality.STALE &&
                quality != DataQuality.OUT_OF_RANGE && quality != DataQuality.UNKNOWN
            ) {
                return@withLock cached.copy(dataQuality = quality)
            }
        }

        val sample = fetcher.fetch(lat, lon, requestedTime)
        if (sample == null) {
            Log.d("EnvSampleRepo", "no valid sample for requested location and time")
            return@withLock null
        }

        val quality = validator.validate(
            sample = sample,
            requestLat = lat,
            requestLon = lon,
            requestTime = requestedTime,
            now = System.currentTimeMillis()
        )
        if (quality == null || quality == DataQuality.STALE ||
            quality == DataQuality.OUT_OF_RANGE
        ) {
            Log.w("EnvSampleRepo", "fetched sample rejected by validation")
            return@withLock null
        }

        val validatedSample = sample.copy(dataQuality = quality)
        val id = dao.insert(validatedSample)
        Log.d("EnvSampleRepo", "saved environmental sample id=$id")
        validatedSample.copy(id = id)
    }

    override suspend fun wasInternetAvailableToday(): Boolean {
        return trackingPrefs.wasInternetAvailableToday()
    }

    private companion object {
        const val CACHE_COORDINATE_DELTA_DEGREES = 0.3
        const val CACHE_MAX_AGE_MS = 15 * 60 * 1000L
        const val CACHE_SAMPLE_TIME_WINDOW_MS = 30 * 60 * 1000L
    }
}
