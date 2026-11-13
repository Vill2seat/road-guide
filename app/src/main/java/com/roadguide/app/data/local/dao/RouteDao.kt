package com.roadguide.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.roadguide.app.data.local.entity.Route
import com.roadguide.app.data.local.entity.RouteStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(route: Route): Long

    @Update
    suspend fun update(route: Route)

    @Query("SELECT * FROM routes WHERE id = :id")
    fun getById(id: Long): Flow<Route?>

    @Query("SELECT * FROM routes WHERE date = :date")
    fun getByDate(date: String): Flow<List<Route>>

    @Query("SELECT * FROM routes ORDER BY startTime DESC")
    fun observeAll(): Flow<List<Route>>

    @Query("SELECT * FROM routes WHERE status = :status ORDER BY startTime DESC")
    fun observeByStatus(status: RouteStatus): Flow<List<Route>>

    @Query("SELECT * FROM routes ORDER BY startTime DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<Route>>
}
