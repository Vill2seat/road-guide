package com.roadguide.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roadguide.app.data.local.entity.Place
import com.roadguide.app.data.local.entity.PlaceCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: Place): Long

    @Update
    suspend fun update(place: Place)

    @Query("SELECT * FROM places WHERE id = :id")
    fun getById(id: Long): Flow<Place?>

    @Query("SELECT * FROM places WHERE category = :category")
    fun observeByCategory(category: PlaceCategory): Flow<List<Place>>

    @Query("SELECT * FROM places WHERE isPrivate = 0 ORDER BY visitCount DESC")
    fun observePublicPlaces(): Flow<List<Place>>
}
