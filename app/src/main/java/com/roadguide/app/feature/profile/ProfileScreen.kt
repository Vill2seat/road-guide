package com.roadguide.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadguide.app.R
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.feature.profile.viewmodel.ProfileViewModel

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val uiState by profileViewModel.uiState.collectAsState()
    val trackingSettings by profileViewModel.trackingSettings.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        RgIcon.RgIconComposable(
                            icon = RgIcon.Back,
                            contentDescription = stringResource(R.string.common_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                ScreenState.Loading -> LoadingPlaceholder(Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = state.message,
                    onRetry = profileViewModel::loadSettings,
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(RgDimens.Space4),
                        verticalArrangement = Arrangement.spacedBy(RgDimens.Space4)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(RgDimens.Space6),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                RgIcon.RgIconComposable(
                                    icon = RgIcon.User,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(Modifier.height(RgDimens.Space3))
                                Text(
                                    stringResource(R.string.settings_local_profile),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    stringResource(R.string.settings_account_optional),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(RgDimens.Space4),
                                verticalArrangement = Arrangement.spacedBy(RgDimens.Space2)
                            ) {
                                Text(
                                    stringResource(R.string.settings_tracking),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    stringResource(
                                        if (trackingSettings.trackingEnabled) {
                                            R.string.tracker_state_recording
                                        } else {
                                            R.string.tracker_state_stopped
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.settings_title))
                        }
                    }
                }
            }
        }
    }
}
