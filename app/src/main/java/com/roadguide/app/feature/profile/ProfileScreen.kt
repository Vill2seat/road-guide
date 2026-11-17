package com.roadguide.app.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.data.local.DataProvider
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.repository.TrackingSettingsRepositoryImpl
import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.domain.usecase.GetTrackingSettings
import com.roadguide.app.domain.usecase.ToggleTracking
import com.roadguide.app.domain.usecase.impl.GetTrackingSettingsImpl
import com.roadguide.app.domain.usecase.impl.ToggleTrackingImpl
import com.roadguide.app.feature.profile.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val trackingPrefs = TrackingPrefs(context)
    val trackingSettingsRepository = TrackingSettingsRepositoryImpl(trackingPrefs)

    val profileViewModel: ProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ProfileViewModel(
                    getTrackingSettings = GetTrackingSettingsImpl(trackingSettingsRepository),
                    toggleTracking = ToggleTrackingImpl(trackingSettingsRepository)
                ) as T
            }
        }
    )

    val uiState by profileViewModel.uiState.collectAsState()
    val trackingSettings by profileViewModel.trackingSettings.collectAsState()

    val currentUiState = uiState

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentUiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = currentUiState.message,
                    onRetry = { profileViewModel.loadSettings() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Card(
                            modifier = Modifier.fillMaxSize(0.8f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            ProfileScreenSettings(
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
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tracking Settings",
            style = MaterialTheme.typography.headlineSmall
        )
        Switch(
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
