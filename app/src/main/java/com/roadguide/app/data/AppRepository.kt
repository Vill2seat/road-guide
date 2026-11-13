package com.roadguide.app.data

/**
 * Placeholder repository interface.
 * No database or network implementation in TASK-001.
 */
interface AppRepository {
    suspend fun ping(): String
}

/**
 * Placeholder repository implementation.
 */
class AppRepositoryImpl : AppRepository {
    override suspend fun ping(): String = "pong"
}