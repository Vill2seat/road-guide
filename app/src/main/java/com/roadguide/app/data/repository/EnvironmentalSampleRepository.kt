package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.EnvironmentalSampleDao
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.EnvironmentalValidator
import com.roadguide.app.data.source.CamsSource
import com.roadguide.app.data.source.EnvironmentalDataFetcher
import com.roadguide.app.data.source.OpenMeteoSource
import kotlinx.coroutines.flow.Flow

interface EnvironmentalSampleRepository {
    suspend fun insert(sample: EnvironmentalSample): Long

    fun observeAll(): Flow<List<EnvironmentalSample>>

    fun getLatest(): Flow<EnvironmentalSample?>

    suspend fun deleteOlderThan(cutoff: Long): Int

    /**
     * Fetches environmental data for given coordinates from remote sources,
     * validates, and persists to local database.
     * Returns the saved sample or null if no data could be fetched.
     */
    suspend fun fetchAndSave(lat: Double, lon: Double): EnvironmentalSample?
}

class EnvironmentalSampleRepositoryImpl(private val dao: EnvironmentalSampleDao) :
    EnvironmentalSampleRepository {

    private val fetcher = EnvironmentalDataFetcher(
        openMeteoSource = OpenMeteoSource(),
        camsSource = CamsSource(apiKey = null)
    )

    private val validator = EnvironmentalValidator()

    override suspend fun insert(sample: EnvironmentalSample): Long = dao.insert(sample)

    override fun observeAll(): Flow<List<EnvironmentalSample>> = dao.observeAll()

    override fun getLatest(): Flow<EnvironmentalSample?> = dao.observeLatest()

    override suspend fun deleteOlderThan(cutoff: Long): Int = dao.deleteOlderThan(cutoff)

    override     suspend fun fetchAndSave(lat: Double, lon: Double): EnvironmentalSample? {
        val sample = fetcher.fetch(lat, lon) ?: return null

        val validatedQuality = validator.validate(
            sample = sample,
            requestLat = lat,
            requestLon = lon,
            requestTime = sample.timestamp
        )

        val finalSample = if (validatedQuality != null && validatedQuality != sample.dataQuality) {
            sample.copy(dataQuality = validatedQuality)
        } else {
            sample
        }

        val id = dao.insert(finalSample)
        return finalSample.copy(id = id)
    }
}
