package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.EnvironmentalSampleDao
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.source.EnvironmentalDataFetcher
import kotlinx.coroutines.flow.Flow

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
    suspend fun fetchAndSave(lat: Double, lon: Double): EnvironmentalSample?

    /**
     * Returns true if internet was available at least once today.
     */
    suspend fun wasInternetAvailableToday(): Boolean
}

class EnvironmentalSampleRepositoryImpl(
    private val dao: EnvironmentalSampleDao,
    private val fetcher: EnvironmentalDataFetcher,
    private val trackingPrefs: TrackingPrefs
) :
    EnvironmentalSampleRepository {

    override suspend fun insert(sample: EnvironmentalSample): Long = dao.insert(sample)

    override fun observeAll(): Flow<List<EnvironmentalSample>> = dao.observeAll()

    override fun getLatest(): Flow<EnvironmentalSample?> = dao.observeLatest()

    override suspend fun deleteOlderThan(cutoff: Long): Int = dao.deleteOlderThan(cutoff)

    override suspend fun fetchAndSave(lat: Double, lon: Double): EnvironmentalSample? {
        val sample = fetcher.fetch(lat, lon) ?: return null

        // Mark internet as available since we successfully fetched data
        trackingPrefs.markInternetAvailable()

        val id = dao.insert(sample)
        return sample.copy(id = id)
    }

    override suspend fun wasInternetAvailableToday(): Boolean {
        return trackingPrefs.wasInternetAvailableToday()
    }
}
