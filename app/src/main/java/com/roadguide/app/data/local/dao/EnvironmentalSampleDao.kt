package com.roadguide.app.data.local.dao

import com.roadguide.app.data.local.entity.EnvironmentalSample
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

interface EnvironmentalSampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sample: EnvironmentalSample)

    @Query("SELECT * FROM environmental_samples WHERE dataQuality != 'OUT_OF_RANGE' ORDER BY timestamp DESC LIMIT 1")
    fun observeLatest(): Flow<EnvironmentalSample?>

    @Query("SELECT * FROM environmental_samples WHERE fetchedAt < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("SELECT * FROM environmental_samples WHERE latitude BETWEEN :lat - :delta AND :lat + :delta AND longitude BETWEEN :lon - :delta AND :lon + :delta AND timestamp BETWEEN :time - :window AND :time + :window LIMIT 1")
    fun findNearest(lat: Double, lon: Double, time: Long, delta: Double, window: Long): Flow<EnvironmentalSample?>

    @Query("SELECT * FROM environmental_samples ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<EnvironmentalSample>>

    @Update
    suspend fun update(sample: EnvironmentalSample)

    @Query("DELETE FROM environmental_samples WHERE id = :id")
    suspend fun deleteById(id: Long)
}
