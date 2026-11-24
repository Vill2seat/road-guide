package com.roadguide.app.data.repository

import com.roadguide.app.data.local.dao.PlaceDao
import com.roadguide.app.data.local.entity.Place
import com.roadguide.app.data.local.entity.PlaceCategory
import kotlinx.coroutines.flow.Flow

interface PlaceRepository {
    suspend fun insert(place: Place): Long

    suspend fun update(place: Place)

    suspend fun deleteById(id: Long)

    fun getById(id: Long): Flow<Place?>

    fun observeByCategory(category: PlaceCategory): Flow<List<Place>>

    fun observeAll(): Flow<List<Place>>

    fun observePublicPlaces(): Flow<List<Place>>
}

class PlaceRepositoryImpl(private val dao: PlaceDao) : PlaceRepository {

    override suspend fun insert(place: Place): Long = dao.insert(place)

    override suspend fun update(place: Place) = dao.update(place)

    override suspend fun deleteById(id: Long) = dao.deleteById(id)

    override fun getById(id: Long): Flow<Place?> = dao.getById(id)

    override fun observeByCategory(category: PlaceCategory): Flow<List<Place>> =
        dao.observeByCategory(category)

    override fun observeAll(): Flow<List<Place>> = dao.observeAll()

    override fun observePublicPlaces(): Flow<List<Place>> = dao.observePublicPlaces()
}
