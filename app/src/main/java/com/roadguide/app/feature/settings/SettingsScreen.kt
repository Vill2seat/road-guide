package com.roadguide.app.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectAsState
import kotlinx.coroutines.flow.flow
import com.roadguide.app.data.local.settings.TrackingPreferences
import com.roadguide.app.data.repository.TrackingSettingsRepositoryImpl
import com.roadguide.app.feature.onboarding.viewmodel.OnboardingViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.R
import com.roadguide.app.data.source.PermissionSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current.applicationContext
    var trackingPrefs by rememberSaveable { TrackingPrefs(context) }
    val trackingSettingsRepository = remember(trackingPrefs) {
        TrackingSettingsRepositoryImpl(trackingPrefs)
    }
    val trackingServiceController = remember(trackingPrefs) {
        try {
            TrackingServiceController(context, trackingPrefs, PermissionSource(context))
        } catch (e: Exception) {
            null
        }
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect preferences flow with proper error handling
    val prefsState = try {
        trackingSettingsRepository.preferencesFlow.collectAsState(initial = TrackingPreferences())
    } catch (e: Exception) {
        TrackingPreferences()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Настройки отслеживания", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Автоматическое отслеживание", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = prefsState.trackingEnabled,
                                onCheckedChange = { enabled -> scope.launch {
                                    try {
                                        trackingServiceController?.setTrackingEnabled(enabled) ?: Unit
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Не удалось изменить состояние трекинга")
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        var expandedInterval by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedInterval,
                            onExpandedChange = { expandedInterval = it }
                        ) {
                            OutlinedTextField(
                                value = when (prefsState.intervalSeconds) {
                                    10 -> "10 сек"; 30 -> "30 сек"; 60 -> "1 мин"; 300 -> "5 мин"; else -> "60 сек"
                                },
                                onValueChange = {},
                                label = { Text("Интервал обновления") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInterval) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedInterval,
                                onDismissRequest = { expandedInterval = false }
                            ) {
                                listOf("10 сек" to 10, "30 сек" to 30, "1 мин" to 60, "5 мин" to 300).forEach { (label, value) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { scope.launch { trackingSettingsRepository.setIntervalSeconds(value) }; expandedInterval = false }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Уведомления по AQI: ${prefsState.aqiThreshold}", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = prefsState.aqiThreshold > 0,
                                onCheckedChange = { }
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Конфиденциальность", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { }) {
                            Text("Зоны приватности")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("О приложении", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Road Guide v1.0.0", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
