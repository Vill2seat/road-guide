package com.roadguide.app.domain.usecase.impl

import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.repository.EnvironmentalSampleRepository
import com.roadguide.app.domain.usecase.GetEnvironmentalSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetEnvironmentalSampleImpl(
    private val environmentalSampleRepository: EnvironmentalSampleRepository
) : GetEnvironmentalSample {

    override suspend fun invoke(lat: Double, lon: Double): EnvironmentalSample? {
        return environmentalSampleRepository.fetchAndSave(lat, lon)
    }

    override fun observeNearest(
        lat: Double,
        lon: Double,
        time: Long,
        delta: Double,
        window: Long
    ): Flow<EnvironmentalSample?> {
        return environmentalSampleRepository.observeAll().map { samples ->
            samples.firstOrNull { s ->
                kotlin.math.abs(s.latitude - lat) <= delta &&
                kotlin.math.abs(s.longitude - lon) <= delta &&
                kotlin.math.abs(s.timestamp - time) <= window
            }
        }
    }
}
