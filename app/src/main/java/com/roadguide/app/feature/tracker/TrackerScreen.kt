package com.roadguide.app.feature.tracker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.feature.tracker.viewmodel.TrackerViewModel
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.domain.usecase.GetTrackingSettingsImpl
import com.roadguide.app.domain.usecase.ToggleTracking
import com.roadguide.app.domain.usecase.ToggleTrackingImpl

/**
 * Tracker (Map) screen with ViewModel integration.
 */
@Composable
fun TrackerScreen(
    trackerViewModel: TrackerViewModel = viewModel(
        factory = { TrackerViewModel(
            getTrackingSettings = GetTrackingSettingsImpl(
                trackingSettingsRepository = com.roadguide.app.data.repository.TrackingSettingsRepositoryImpl(
                    trackingPrefs = com.roadguide.app.data.local.settings.TrackingPrefs(
                        context = androidx.lifecycle.viewmodel.viewModelScopeLocalContext.current.context
                    )
                )
            ),
            toggleTracking = ToggleTrackingImpl(
                trackingSettingsRepository = com.roadguide.app.data.repository.TrackingSettingsRepositoryImpl(
                    trackingPrefs = com.roadguide.app.data.local.settings.TrackingPrefs(
                        context = androidx.lifecycle.viewmodel.viewModelScopeLocalContext.current.context
                    )
                )
            )
        ) }
    ),
    modifier: Modifier = Modifier
) {
    val uiState by trackerViewModel.uiState.collectAsState()
    val trackingEnabled by trackerViewModel.trackingEnabled.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Tracker", style = MaterialTheme.typography.headlineSmall) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = uiState.message,
                    onRetry = { trackerViewModel.loadTrackingState() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success -> {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = androidx.compose.foundation.layout.Alignment.CenterHorizontally
                    ) {
                        androidx.compose.material3.Card(
                            modifier = Modifier.fillMaxSize(0.6f),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            com.roadguide.app.feature.tracker.TrackerScreenContent(
                                trackingEnabled = trackingEnabled,
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
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.foundation.layout.Alignment.CenterHorizontally
    ) {
        androidx.compose.material3.Text(
            text = "Tracking",
            style = MaterialTheme.typography.headlineSmall
        )
        androidx.compose.material3.Text(
            text = if (trackingEnabled) "Enabled" else "Disabled",
            style = MaterialTheme.typography.bodyMedium
        )
        androidx.compose.material3.Button(onClick = onToggle) {
            Text(if (trackingEnabled) "Disable" else "Enable")
        }
    }
}

