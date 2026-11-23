// Settings Screen implementation

package com.roadguide.app.feature.settings

import androidx.activity.compose.backstackNavigate
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ripple.rippleClickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.compose
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.ScopeUpdate
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.contextOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntDp
import com.roadguide.app.data.repository.TrackingSettingsRepository
import com.roadguide.app.feature.settings.ui.section
import com.roadguide.app.feature.settings.ui.toggle
import com.roadguide.app.feature.settings.ui.dropdown
import com.roadguide.app.feature.settings.ui.radioGroup
import com.roadguide.app.feature.settings.ui.confirmationDialog
import com.roadguide.app.feature.settings.ui.aboutSection
import com.roadguide.app.feature.settings.ui.notificationsSection
import com.roadguide.app.feature.settings.ui.mapSettingsSection
import com.roadguide.app.feature.settings.ui.unitsLanguageSection
import com.roadguide.app.feature.settings.ui.accountSection
import com.roadguide.app.feature.settings.ui.aboutSection
import com.roadguide.app.navigation.navRoute
import com.roadguide.app.navigation.navDeepLink
import com.roadguide.app.R
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.domain.model.TrackingPreferences
import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.domain.usecase.ToggleTracking
import com.roadguide.app.data.repository.TrackingSettingsRepository
import kotlin.coroutines.resumeWithException

@Composable
fun SettingsScreen() {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    val navController = rememberNavController()
    
    // Load preferences
    val prefs by rememberSaveable {
        mutableStateOf(TrackingPreferences())
    }
    val prefsState by TrackingSettingsRepository.preferencesFlow.collectAsState()
    prefs.copy(
        trackingEnabled = prefsState.trackingEnabled,
        intervalSeconds = prefsState.intervalSeconds,
        units = prefsState.units,
        aqiThreshold = prefsState.aqiThreshold
    ).also { prefs ->
        LaunchedEffect(prefs) {
            TrackingSettingsRepository.setTrackingEnabled(prefs.trackingEnabled)
            TrackingSettingsRepository.setIntervalSeconds(prefs.intervalSeconds)
            TrackingSettingsRepository.setUnits(prefs.units)
            TrackingSettingsRepository.setAqiThreshold(prefs.aqiThreshold)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = "Настройки", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(RgIcon.Back, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(RgDimens.Space8),
            contentPadding = PaddingValues(horizontal = RgDimens.Space16)
        ) {
            // === 1. Tracking Settings ===
            section(
                title = "Настройки отслеживания",
                icon = RgIcon.Tracking,
                expanded = expanded
            ) {
                Column(modifier = Modifier.padding(RgDimens.Space12)) {
                    toggle(
                        title = "Автоматическое отслеживание",
                        checked = prefs.trackingEnabled,
                        onCheckedChange = {
                            prefs = prefs.copy(trackingEnabled = it)
                            TrackingSettingsRepository.setTrackingEnabled(it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    dropdown(
                        title = "Интервал обновления",
                        selected = when {
                            prefs.intervalSeconds == 10 -> "10 сек"
                            prefs.intervalSeconds == 30 -> "30 сек"
                            prefs.intervalSeconds == 60 -> "1 мин"
                            else -> "5 мин"
                        },
                        options = listOf("10 сек", "30 сек", "1 мин", "5 мин"),
                        onSelect = {
                            val option = it
                            when (option) {
                                "10 сек" -> prefs = prefs.copy(intervalSeconds = 10)
                                "30 сек" -> prefs = prefs.copy(intervalSeconds = 30)
                                "1 мин" -> prefs = prefs.copy(intervalSeconds = 60)
                                "5 мин" -> prefs = prefs.copy(intervalSeconds = 300)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    dropdown(
                        title = "Точность местоположения",
                        selected = when {
                            prefs.units == "high" -> "Высокая"
                            prefs.units == "low" -> "Низкая"
                            else -> "Баланс"
                        },
                        options = listOf("Высокая", "Низкая", "Баланс"),
                        onSelect = {
                            val option = it
                            when (option) {
                                "Высокая" -> prefs = prefs.copy(units = "high")
                                "Низкая" -> prefs = prefs.copy(units = "low")
                                "Баланс" -> prefs = prefs.copy(units = "balanced")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Бattery Optimisation",
                        checked = prefs.isBatteryOptimisationEnabled,
                        onCheckedChange = {
                            prefs = prefs.copy(isBatteryOptimisationEnabled = it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Обнаружение активности",
                        checked = prefs.isActivityDetectionEnabled,
                        onCheckedChange = {
                            prefs = prefs.copy(isActivityDetectionEnabled = it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Фоновое мониторинг",
                        checked = prefs.isBackgroundMonitoringEnabled,
                        onCheckedChange = {
                            prefs = prefs.copy(isBackgroundMonitoringEnabled = it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    OutlinedButton(
                        onClick = {
                            // TODO: implement pause tracking
                        }
                    ) {
                        Text("Приостановить отслеживание")
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(RgDimens.Space8)) {
                            Text(
                                text = "Статус разрешений Android",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Фигурирует разрешения GPS и фонового отслеживания",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            section(
                title = "Конфиденциальность и данные",
                icon = RgIcon.Privacy,
                expanded = expanded
            ) {
                Column(modifier = Modifier.padding(RgDimens.Space12)) {
                    OutlinedButton(
                        onClick = { navController.navigate("privacy_zones") }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Зоны приватности",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.ArrowRight, contentDescription = "Перейти")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                    OutlinedButton(
                        onClick = { /* TODO: navigate to excluded locations */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Исключённые локации",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.ArrowRight, contentDescription = "Перейти")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                    dropdown(
                        title = "Хранение данных",
                        selected = when {
                            prefs.dataRetention == 7 -> "7 дней"
                            prefs.dataRetention == 30 -> "30 дней"
                            else -> "90 дней"
                        },
                        options = listOf("7 дней", "30 дней", "90 дней"),
                        onSelect = {
                            val option = it
                            when (option) {
                                "7 дней" -> prefs = prefs.copy(dataRetention = 7)
                                "30 дней" -> prefs = prefs.copy(dataRetention = 30)
                                "90 дней" -> prefs = prefs.copy(dataRetention = 90)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                    OutlinedButton(
                        onClick = { /* TODO: export data */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Экспорт моих данных",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.ArrowRight, contentDescription = "Экспорт")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                    OutlinedButton(
                        onClick = { /* TODO: show confirmation dialog */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Удалить историю за день",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.Delete, contentDescription = "Удалить")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                    OutlinedButton(
                        onClick = { /* TODO: show confirmation dialog */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Удалить все данные",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.Delete, contentDescription = "Удалить всё")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                    OutlinedButton(
                        onClick = { /* TODO: open privacy policy */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Политика конфиденциальности",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.ArrowRight, contentDescription = "Ссылка")
                        }
                    }
                }
            }

            section(
                title = "Ecological предпочтения",
                icon = RgIcon.Environment,
                expanded = expanded
            ) {
                Column(modifier = Modifier.padding(RgDimens.Space12)) {
                    OutlinedButton(
                        onClick = { /* TODO: show data sources */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Источники данных",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.ArrowRight, contentDescription = "Список")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                    dropdown(
                        title = "Standard AQI",
                        selected = when {
                            prefs.aqiStandard == "us" -> "US Standard"
                            prefs.aqiStandard == "european" -> "European Standard"
                            else -> "Default"
                        },
                        options = listOf("US Standard", "European Standard", "Default"),
                        onSelect = {
                            val option = it
                            when (option) {
                                "US Standard" -> prefs = prefs.copy(aqiStandard = "us")
                                "European Standard" -> prefs = prefs.copy(aqiStandard = "european")
                                "Default" -> prefs = prefs.copy(aqiStandard = "default")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                    Column(modifier = Modifier.padding(RgDimens.Space4)) {
                        Text(
                            text = "Отображение загрязнителей",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { /* TODO: toggle PM2.5 display */ }
                            ) {
                                Text(
                                    text = "PM2.5",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        prefs.showPM2_5 -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = { /* TODO: toggle PM10 display */ }
                            ) {
                                Text(
                                    text = "PM10",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        prefs.showPM10 -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = { /* TODO: toggle NO2 display */ }
                            ) {
                                Text(
                                    text = "NO2",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        prefs.showNO2 -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = { /* TODO: toggle O3 display */ }
                            ) {
                                Text(
                                    text = "O3",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        prefs.showO3 -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = { /* TODO: toggle SO2 display */ }
                            ) {
                                Text(
                                    text = "SO2",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        prefs.showSO2 -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = { /* TODO: toggle CO display */ }
                            ) {
                                Text(
                                    text = "CO",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        prefs.showCO -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    OutlinedButton(
                        onClick = { /* TODO: show formula explanation */ }
                    ) {
                        Text(
                            text = "Объяснение балла экспозиции",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                    OutlinedButton(
                        onClick = { /* TODO: show warning */ }
                    ) {
                        Text(
                            text = "Предупреждение о расчётных данных",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            section(
                title = "Уведомления",
                icon = RgIcon.Notifications,
                expanded = expanded
            ) {
                Column(modifier = Modifier.padding(RgDimens.Space12)) {
                    toggle(
                        title = "Ежедневный отчёт",
                        checked = prefs.isDailyReportEnabled,
                        onCheckedChange = {
                            prefs = prefs.copy(isDailyReportEnabled = it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Статус отслеживания",
                        checked = prefs.isTrackingStatusEnabled,
                        onCheckedChange = {
                            prefs = prefs.copy(isTrackingStatusEnabled = it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Предупреждения данных",
                        checked = prefs.isDataWarningsEnabled,
                        onCheckedChange = {
                            prefs = prefs.copy(isDataWarningsEnabled = it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Suggestions для маршрутов",
                        checked = prefs.isRouteSuggestionsEnabled,
                        onCheckedChange = {
                            prefs = prefs.copy(isRouteSuggestionsEnabled = it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Ecological оповещения",
                        checked = prefs.isEnvironmentalAlertsEnabled,
                        onCheckedChange = {
                            prefs = prefs.copy(isEnvironmentalAlertsEnabled = it)
                        }
                    )
                }
            }

            section(
                title = "Настройки карты",
                icon = RgIcon.Map,
                expanded = expanded
            ) {
                Column(modifier = Modifier.padding(RgDimens.Space12)) {
                    dropdown(
                        title = "Стиль карты",
                        selected = when {
                            prefs.mapStyle == "openfreemap" -> "OpenFreeMap"
                            prefs.mapStyle == "maplibre" -> "MapLibre"
                            else -> "Standard"
                        },
                        options = listOf("OpenFreeMap", "MapLibre", "Standard"),
                        onSelect = {
                            val option = it
                            when (option) {
                                "OpenFreeMap" -> prefs = prefs.copy(mapStyle = "openfreemap")
                                "MapLibre" -> prefs = prefs.copy(mapStyle = "maplibre")
                                "Standard" -> prefs = prefs.copy(mapStyle = "standard")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Показывать зоны",
                        checked = prefs.isShowingGreenAreas,
                        onCheckedChange = {
                            prefs = prefs.copy(isShowingGreenAreas = it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Показывать главные дороги",
                        checked = prefs.isShowingMajorRoads,
                        onCheckedChange = {
                            prefs = prefs.copy(isShowingMajorRoads = it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Показать экспозицию",
                        checked = prefs.isShowingExposureOverlay,
                        onCheckedChange = {
                            prefs = prefs.copy(isShowingExposureOverlay = it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Показывать точки времени",
                        checked = prefs.isShowingTimelinePoints,
                        onCheckedChange = {
                            prefs = prefs.copy(isShowingTimelinePoints = it)
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    toggle(
                        title = "Показывать зоны приватности",
                        checked = false,
                        onCheckedChange = {
                            // No-op - default disabled
                        }
                    )
                }
            }

            section(
                title = "Единицы и язык",
                icon = RgIcon.Units,
                expanded = expanded
            ) {
                Column(modifier = Modifier.padding(RgDimens.Space12)) {
                    radioGroup(
                        title = "Дистанция",
                        selected = when {
                            prefs.distanceUnits == "km" -> "km"
                            prefs.distanceUnits == "miles" -> "miles"
                            else -> "km"
                        },
                        options = listOf("km", "miles"),
                        onSelect = {
                            val option = it
                            when (option) {
                                "km" -> prefs = prefs.copy(distanceUnits = "km")
                                "miles" -> prefs = prefs.copy(distanceUnits = "miles")
                            }
                        },
                        optionsIcons = listOf(
                            Icon(Icons.Default.Map, contentDescription = "Километры"),
                            Icon(Icons.Default.Star, contentDescription = "Мили")
                        )
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    radioGroup(
                        title = "Температура",
                        selected = when {
                            prefs.temperatureUnits == "celsius" -> "Celsius"
                            prefs.temperatureUnits == "fahrenheit" -> "Fahrenheit"
                            else -> "Celsius"
                        },
                        options = listOf("Celsius", "Fahrenheit"),
                        onSelect = {
                            val option = it
                            when (option) {
                                "Celsius" -> prefs = prefs.copy(temperatureUnits = "celsius")
                                "Fahrenheit" -> prefs = prefs.copy(temperatureUnits = "fahrenheit")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    dropdown(
                        title = "Язык",
                        selected = when {
                            prefs.language == "en" -> "English"
                            else -> "English"
                        },
                        options = listOf("English"),
                        onSelect = {
                            val option = it
                            when (option) {
                                "English" -> prefs = prefs.copy(language = "en")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    radioGroup(
                        title = "Формат времени",
                        selected = when {
                            prefs.timeFormat == "12h" -> "12ч"
                            prefs.timeFormat == "24h" -> "24ч"
                            else -> "24ч"
                        },
                        options = listOf("12ч", "24ч"),
                        onSelect = {
                            val option = it
                            when (option) {
                                "12ч" -> prefs = prefs.copy(timeFormat = "12h")
                                "24ч" -> prefs = prefs.copy(timeFormat = "24h")
                            }
                        }
                    )
                }
            }

            section(
                title = "Account",
                icon = RgIcon.Account,
                expanded = expanded
            ) {
                Column(modifier = Modifier.padding(RgDimens.Space12)) {
                    OutlinedButton(
                        onClick = { navController.navigate("profile") }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Профиль",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.ArrowRight, contentDescription = "Профиль")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    OutlinedButton(
                        onClick = { /* TODO: show confirmation dialog */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Выйти",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.Delete, contentDescription = "Выйти")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                }
            }

            section(
                title = "О Road Guide",
                icon = RgIcon.Info,
                expanded = expanded
            ) {
                Column(modifier = Modifier.padding(RgDimens.Space12)) {
                    OutlinedButton(
                        onClick = { /* TODO: show version */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Версия приложения",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "1.0.0",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    OutlinedButton(
                        onClick = { /* TODO: show data sources */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Источники данных",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.ArrowRight, contentDescription = "Список")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    OutlinedButton(
                        onClick = { /* TODO: open licenses */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Открытые лицензии",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.ArrowRight, contentDescription = "Лицензии")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    OutlinedButton(
                        onClick = { /* TODO: open privacy policy */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Политика конфиденциальности",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.ArrowRight, contentDescription = "Ссылка")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    OutlinedButton(
                        onClick = { /* TODO: open terms */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Условия использования",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.ArrowRight, contentDescription = "Условия")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    OutlinedButton(
                        onClick = { /* TODO: open contact */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Контактная информация",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.ArrowRight, contentDescription = "Контакт")
                        }
                    }
                    Spacer(modifier = Modifier.height(RgDimens.Space8))
                    OutlinedButton(
                        onClick = { /* TODO: show disclaimer */ }
                    ) {
                        Row(
                            modifier = Modifier.padding(RgDimens.Space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Отказ от ответственности",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(RgIcon.Warning, contentDescription = "Предупреждение")
                        }
                    }
                }
            }
        }
    }
}
