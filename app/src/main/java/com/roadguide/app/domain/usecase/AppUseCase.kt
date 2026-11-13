package com.roadguide.app.domain.usecase

import com.roadguide.app.core.common.Result
import com.roadguide.app.domain.model.AppModel

/**
 * Placeholder use case interface.
 * Real use cases are defined per layer in 02_SYSTEM_ARCHITECTURE.md
 * and will be implemented in later tasks.
 */
interface AppUseCase {
    suspend fun execute(): Result<AppModel>
}

/**
 * Placeholder use case implementation.
 */
class AppUseCaseImpl : AppUseCase {
    override suspend fun execute(): Result<AppModel> =
        Result.Success(AppModel(id = "1", name = "Placeholder"))
}