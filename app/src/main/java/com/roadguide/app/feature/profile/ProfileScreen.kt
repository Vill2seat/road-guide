package com.roadguide.app.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.feature.profile.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(
        factory = { ProfileViewModel(
            getTrackingSettings = com.roadguide.app.domain.usecase.impl.GetTrackingSettingsImpl(
                trackingSettingsRepository = com.roadguide.app.data.repository.TrackingSettingsRepositoryImpl(
                    trackingPrefs = com.roadguide.app.data.local.settings.TrackingPrefs(
                        context = androidx.lifecycle.viewmodel.viewModelScopeLocalContext.current.context
                    )
                )
            ),
            toggleTracking = com.roadguide.app.domain.usecase.impl.ToggleTrackingImpl(
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
    val uiState by profileViewModel.uiState.collectAsState()
    val trackingSettings by profileViewModel.trackingSettings.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Profile", style = MaterialTheme.typography.headlineSmall) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = uiState.message,
                    onRetry = { profileViewModel.loadSettings() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.VerticalArrangement.spacedBy(16.dp)
                    ) {
                        androidx.compose.material3.Card(
                            modifier = Modifier.fillMaxSize(0.8f),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            com.roadguide.app.feature.profile.ProfileScreenSettings(
                                settings = trackingSettings,
                                onTrackingToggle = { profileViewModel.toggleTracking(it) },
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
private fun ProfileScreenSettings(
    settings: TrackingSettingsState,
    onTrackingToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.foundation.layout.Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tracking Settings",
            style = MaterialTheme.typography.headlineSmall
        )
        androidx.compose.material3.Switch(
            checked = settings.trackingEnabled,
            onCheckedChange = onTrackingToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        Text(
            text = "Interval: ${settings.intervalSeconds}s",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Units: ${settings.units}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
