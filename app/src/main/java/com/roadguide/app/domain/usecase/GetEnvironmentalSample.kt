package com.roadguide.app.domain.usecase

import com.roadguide.app.data.local.entity.EnvironmentalSample
import kotlinx.coroutines.flow.Flow

/**
 * Use case for fetching and retrieving environmental samples (weather + AQI)
 * for a given location and time.
 */
interface GetEnvironmentalSample {
    suspend fun invoke(lat: Double, lon: Double): EnvironmentalSample?

    fun observeNearest(lat: Double, lon: Double, time: Long, delta: Double, window: Long): Flow<EnvironmentalSample?>
}
