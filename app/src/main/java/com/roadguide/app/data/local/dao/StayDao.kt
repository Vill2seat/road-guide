package com.roadguide.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.roadguide.app.data.local.entity.Stay
import kotlinx.coroutines.flow.Flow

@Dao
interface StayDao {
    @Insert
    suspend fun insert(stay: Stay): Long

    @Update
    suspend fun update(stay: Stay)

    @Query("SELECT * FROM stays WHERE routeId = :routeId ORDER BY startTime")
    fun observeByRoute(routeId: Long): Flow<List<Stay>>

    @Query("SELECT * FROM stays WHERE placeId = :placeId ORDER BY startTime DESC")
    fun observeByPlace(placeId: Long): Flow<List<Stay>>
}
