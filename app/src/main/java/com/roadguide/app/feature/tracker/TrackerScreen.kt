package com.roadguide.app.feature.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.R
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.data.local.DataProvider
import com.roadguide.app.data.local.entity.EnvironmentalSample
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.repository.EnvironmentalSampleRepositoryImpl
import com.roadguide.app.data.repository.TrackingSettingsRepositoryImpl
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.domain.usecase.impl.GetTrackingSettingsImpl
import com.roadguide.app.domain.usecase.ToggleTracking
import com.roadguide.app.domain.usecase.impl.ToggleTrackingImpl
import com.roadguide.app.feature.tracker.viewmodel.RecordingState
import com.roadguide.app.feature.tracker.viewmodel.TrackerViewModel
import com.roadguide.app.map.MapView

@Composable
fun TrackerScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataProvider = DataProvider.getInstance(context)
    val trackingPrefs = TrackingPrefs(context)
    val trackingSettingsRepository = TrackingSettingsRepositoryImpl(trackingPrefs)
    val environmentalSampleRepository = EnvironmentalSampleRepositoryImpl(
        dataProvider.environmentalSampleDao
    )

    val trackerViewModel: TrackerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TrackerViewModel(
                    getTrackingSettings = GetTrackingSettingsImpl(trackingSettingsRepository),
                    toggleTracking = ToggleTrackingImpl(trackingSettingsRepository),
                    getEnvironmentalSample = com.roadguide.app.domain.usecase.impl.GetEnvironmentalSampleImpl(environmentalSampleRepository)
                ) as T
            }
        }
    )

    val uiState by trackerViewModel.uiState.collectAsState()
    val trackingEnabled by trackerViewModel.trackingEnabled.collectAsState()
    val recordingState by trackerViewModel.recordingState.collectAsState()
    val currentRoutePoints by trackerViewModel.currentRoutePoints.collectAsState()
    val environmentalSample by trackerViewModel.environmentalSample.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val screenState = uiState
            when (screenState) {
                is com.roadguide.app.core.common.ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is com.roadguide.app.core.common.ScreenState.Error -> ErrorPlaceholder(
                    message = screenState.message,
                    onRetry = { trackerViewModel.loadTrackingState() },
                    modifier = Modifier.fillMaxSize()
                )
                com.roadguide.app.core.common.ScreenState.Success,
                com.roadguide.app.core.common.ScreenState.Empty -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MapView(
                            modifier = Modifier.fillMaxWidth()
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = RgDimens.Space3),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            TrackerScreenContent(
                                trackingEnabled = trackingEnabled,
                                recordingState = recordingState,
                                currentRoutePoints = currentRoutePoints,
                                environmentalSample = environmentalSample,
                                onStart = { trackerViewModel.startTracking() },
                                onStop = { trackerViewModel.stopTracking() },
                                onToggle = { trackerViewModel.toggleTracking(!trackingEnabled) },
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerScreenContent(
    trackingEnabled: Boolean,
    recordingState: RecordingState,
    currentRoutePoints: List<com.roadguide.app.domain.model.LocationPointSummary>,
    environmentalSample: EnvironmentalSample?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RowStatus(
            trackingEnabled = trackingEnabled,
            recordingState = recordingState,
            pointCount = currentRoutePoints.size,
            environmentalSample = environmentalSample,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(RgDimens.Space4))

        ActionRow(
            recordingState = recordingState,
            trackingEnabled = trackingEnabled,
            onStart = onStart,
            onStop = onStop,
            onToggle = onToggle,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RowStatus(
    trackingEnabled: Boolean,
    recordingState: RecordingState,
    pointCount: Int,
    environmentalSample: EnvironmentalSample?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column() {
            Text(
                text = "Трекинг",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when (recordingState) {
                    RecordingState.IDLE -> "Готов к записи"
                    RecordingState.RECORDING -> "Идёт запись..."
                    RecordingState.PAUSED -> "На паузе"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (trackingEnabled) "ВКЛ" else "ВЫКЛ",
                style = MaterialTheme.typography.titleMedium,
                color = if (trackingEnabled) RgColor.Success else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (pointCount > 0) {
                Text(
                    text = "Точек: $pointCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            environmentalSample?.let { sample ->
                val aqi = sample.europeanAqi ?: sample.usAqi ?: 0
                val aqiColor = when (aqi) {
                    in 0..50 -> RgColor.ExposureGood
                    in 51..100 -> RgColor.ExposureModerate
                    in 101..150 -> RgColor.ExposureHigh
                    else -> RgColor.ExposureVeryHigh
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AQI: $aqi",
                    style = MaterialTheme.typography.bodySmall,
                    color = aqiColor
                )
            }
        }
    }
}

@Composable
private fun ActionRow(
    recordingState: RecordingState,
    trackingEnabled: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (recordingState) {
            RecordingState.RECORDING -> {
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = RgColor.Error)
                ) {
                    Text(stringResource(R.string.tracker_stop), style = MaterialTheme.typography.labelLarge)
                }
            }
            RecordingState.IDLE -> {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = RgColor.Primary)
                ) {
                    Text(stringResource(R.string.tracker_start), style = MaterialTheme.typography.labelLarge)
                }
            }
            RecordingState.PAUSED -> {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = RgColor.Primary)
                ) {
                    Text(stringResource(R.string.tracker_resume), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        Switch(
            checked = trackingEnabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        Text(
            text = if (trackingEnabled) "Фоновый" else "Офлайн",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
