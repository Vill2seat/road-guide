package com.roadguide.app.domain.model

import com.roadguide.app.data.local.entity.PrivacyZone

/**
 * Состояние геофенсинга для отображения в UI.
 *
 * @param currentZones список активных приват-зон, в которых находится пользователь
 * @param isInsideProtectedZone флаг: находится ли пользователь хотя бы в одной активной зоне
 */
data class GeofenceState(
    val currentZones: List<PrivacyZone> = emptyList(),
    val isInsideProtectedZone: Boolean = false
)