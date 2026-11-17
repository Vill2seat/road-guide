package com.roadguide.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.roadguide.app.data.local.entity.EnvironmentalSample
import kotlinx.coroutines.flow.Flow

@Dao
interface EnvironmentalSampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: EnvironmentalSample): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(samples: List<EnvironmentalSample>): List<Long>

    @Query(
        """
        SELECT * FROM environmental_samples
        WHERE latitude BETWEEN :lat - :delta AND :lat + :delta
          AND longitude BETWEEN :lon - :delta AND :lon + :delta
          AND timestamp BETWEEN :time - :window AND :time + :window
          AND dataQuality != 'OUT_OF_RANGE'
        ORDER BY ABS(latitude - :lat) + ABS(longitude - :lon) + ABS(timestamp - :time)
        LIMIT 1
        """
    )
    fun findNearest(
        lat: Double,
        lon: Double,
        time: Long,
        delta: Double,
        window: Long
    ): Flow<EnvironmentalSample?>

    @Query("SELECT * FROM environmental_samples ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<EnvironmentalSample?>

    @Query("SELECT * FROM environmental_samples ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<EnvironmentalSample>>

    @Query("DELETE FROM environmental_samples WHERE fetchedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int
}
