package com.roadguide.app.feature.tracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.data.local.DataProvider
import com.roadguide.app.data.local.settings.TrackingPrefs
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
    val trackingPrefs = TrackingPrefs(context)
    val trackingSettingsRepository = TrackingSettingsRepositoryImpl(trackingPrefs)

    val trackerViewModel: TrackerViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TrackerViewModel(
                    getTrackingSettings = GetTrackingSettingsImpl(trackingSettingsRepository),
                    toggleTracking = ToggleTrackingImpl(trackingSettingsRepository)
                ) as T
            }
        }
    )

    val uiState by trackerViewModel.uiState.collectAsState()
    val trackingEnabled by trackerViewModel.trackingEnabled.collectAsState()
    val recordingState by trackerViewModel.recordingState.collectAsState()
    val currentRoutePoints by trackerViewModel.currentRoutePoints.collectAsState()

    val currentUiState = uiState

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Tracker", style = MaterialTheme.typography.headlineSmall) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentUiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = currentUiState.message,
                    onRetry = { trackerViewModel.loadTrackingState() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        MapView(
                            modifier = Modifier.weight(1f),
                            testTag = "tracker_map"
                        )

                        Card(
                            modifier = Modifier.fillMaxSize(0.5f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            TrackerScreenContent(
                                trackingEnabled = trackingEnabled,
                                recordingState = recordingState,
                                currentRoutePoints = currentRoutePoints,
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
    onStart: () -> Unit,
    onStop: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tracking",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = if (trackingEnabled) "Enabled" else "Disabled",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = when (recordingState) {
                RecordingState.IDLE -> "Ready"
                RecordingState.RECORDING -> "Recording..."
                RecordingState.PAUSED -> "Paused"
            },
            style = MaterialTheme.typography.bodyMedium
        )

        if (currentRoutePoints.isNotEmpty()) {
            Text(
                text = "Points: ${currentRoutePoints.size}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        when {
            recordingState == RecordingState.RECORDING -> {
                androidx.compose.material3.Button(onClick = onStop) {
                    Text("Stop Recording")
                }
            }
            recordingState == RecordingState.IDLE -> {
                androidx.compose.material3.Button(onClick = onStart) {
                    Text("Start Tracking")
                }
            }
        }

        androidx.compose.material3.Button(onClick = onToggle) {
            Text(if (trackingEnabled) "Disable" else "Enable")
        }
    }
}
