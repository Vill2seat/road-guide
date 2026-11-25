package com.roadguide.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadguide.app.R
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.domain.model.TrackingSettingsState
import com.roadguide.app.feature.profile.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    val profileViewModel: ProfileViewModel = hiltViewModel()

    val uiState by profileViewModel.uiState.collectAsState()
    val trackingSettings by profileViewModel.trackingSettings.collectAsState()

    val currentUiState = uiState

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = RgColor.Background
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
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = RgDimens.Space4, vertical = RgDimens.Space4),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(RgDimens.Space3)
                    ) {
                        Text(
                            text = stringResource(R.string.nav_profile),
                            style = MaterialTheme.typography.headlineSmall,
                            color = RgColor.OnBackground,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(RgDimens.Space2))

                        // Local profile card
                        ProfileHeaderCard(settings = trackingSettings)

                        // Tracking toggle card
                        TrackingToggleCard(
                            settings = trackingSettings,
                            onTrackingToggle = { profileViewModel.toggleTracking(it) }
                        )

                        // Settings link
                        ProfileLinkCard(
                            icon = Icons.Default.Settings,
                            title = stringResource(R.string.nav_settings),
                            onClick = onOpenSettings
                        )

                        // Privacy link
                        ProfileLinkCard(
                            icon = Icons.Default.Shield,
                            title = "Privacy & Data",
                            onClick = { }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(settings: TrackingSettingsState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RgDimens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = RgColor.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = RgDimens.Elevation1)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RgDimens.Space4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(RgDimens.Space3)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RgColor.PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = RgColor.Primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "Local Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = RgColor.OnSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Units: ${settings.units}",
                    style = MaterialTheme.typography.bodySmall,
                    color = RgColor.OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrackingToggleCard(
    settings: TrackingSettingsState,
    onTrackingToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RgDimens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = RgColor.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = RgDimens.Elevation1)
    ) {
        Column(
            modifier = Modifier.padding(RgDimens.Space4)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tracking",
                    style = MaterialTheme.typography.titleSmall,
                    color = RgColor.OnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = settings.trackingEnabled,
                    onCheckedChange = onTrackingToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RgColor.Primary,
                        checkedTrackColor = RgColor.PrimaryContainer,
                        uncheckedThumbColor = RgColor.OnSurfaceVariant,
                        uncheckedTrackColor = RgColor.SurfaceVariant
                    )
                )
            }
            Text(
                text = "Interval: ${settings.intervalSeconds}s",
                style = MaterialTheme.typography.bodySmall,
                color = RgColor.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileLinkCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RgDimens.RadiusCard),
        colors = CardDefaults.cardColors(containerColor = RgColor.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = RgDimens.Elevation1),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RgDimens.Space4),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RgDimens.Space3)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(RgColor.PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = RgColor.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = RgColor.OnSurface
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = RgColor.OnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
