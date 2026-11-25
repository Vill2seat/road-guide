package com.roadguide.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.roadguide.app.R
import com.roadguide.app.ScreenRoutes
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.data.local.settings.TrackingPreferences
import com.roadguide.app.data.local.settings.TrackingPrefs
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.service.TrackingServiceController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import android.util.Log

enum class SettingsSection(val route: String, val titleRes: Int, val icon: RgIcon.IconData) {
    MAIN(ScreenRoutes.Settings, R.string.settings_title, RgIcon.Settings),
    TRACKING(ScreenRoutes.SettingsTracking, R.string.settings_tracking, RgIcon.Clock),
    PRIVACY(ScreenRoutes.SettingsPrivacy, R.string.settings_privacy, RgIcon.Shield),
    ENVIRONMENTAL(
        ScreenRoutes.SettingsEnvironmental,
        R.string.settings_environmental,
        RgIcon.Leaf
    ),
    NOTIFICATIONS(
        ScreenRoutes.SettingsNotifications,
        R.string.settings_notifications,
        RgIcon.Bell
    ),
    MAP(ScreenRoutes.SettingsMap, R.string.settings_map, RgIcon.Map),
    UNITS_LANGUAGE(
        ScreenRoutes.SettingsUnits,
        R.string.settings_units_language,
        RgIcon.Settings
    ),
    ACCOUNT(ScreenRoutes.SettingsAccount, R.string.settings_account, RgIcon.User),
    ABOUT(ScreenRoutes.SettingsAbout, R.string.settings_about, RgIcon.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    section: SettingsSection,
    onBack: () -> Unit,
    onOpenSection: (SettingsSection) -> Unit,
    onOpenPlaces: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val trackingPrefs = remember { TrackingPrefs(context) }
    val permissionSource = remember { PermissionSource(context) }
    val trackingServiceController = remember(trackingPrefs) {
        TrackingServiceController(context, trackingPrefs, permissionSource)
    }
    val preferences by trackingPrefs.preferencesFlow.collectAsState(
        initial = TrackingPreferences()
    )
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(section.titleRes)) },
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
        if (section == SettingsSection.MAIN) {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = RgDimens.Space4,
                    vertical = RgDimens.Space3
                ),
                verticalArrangement = Arrangement.spacedBy(RgDimens.Space3)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings_personalize),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = RgDimens.Space2)
                    )
                }
                items(SettingsSection.entries.filter { it != SettingsSection.MAIN }) { item ->
                    SettingsCategoryCard(item) { onOpenSection(item) }
                }
            }
        } else {
            SettingsSectionContent(
                section = section,
                preferences = preferences,
                permissionSource = permissionSource,
                onOpenPlaces = onOpenPlaces,
                onTrackingChanged = { enabled ->
                    scope.launch {
                        try {
                            trackingServiceController.setTrackingEnabled(enabled)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to update tracking", e)
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.settings_tracking_update_failed)
                            )
                        }
                    }
                },
                onIntervalChanged = { seconds ->
                    scope.launch {
                        try {
                            trackingPrefs.setIntervalSeconds(seconds)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to update tracking interval", e)
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.settings_save_failed)
                            )
                        }
                    }
                },
                onUnitsChanged = { units ->
                    scope.launch {
                        try {
                            trackingPrefs.setUnits(units)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e("SettingsScreen", "Failed to update units", e)
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.settings_save_failed)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsCategoryCard(
    section: SettingsSection,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RgDimens.Space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RgIcon.RgIconComposable(
                icon = section.icon.copy(color = Color(0xFF317854)),
                modifier = Modifier.size(RgDimens.IconLarge)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = RgDimens.Space4)
            ) {
                Text(
                    text = stringResource(section.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(section.subtitleRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RgIcon.RgIconComposable(
                icon = RgIcon.Chevron.copy(color = Color(0xFF596B61)),
                contentDescription = null
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSectionContent(
    section: SettingsSection,
    preferences: TrackingPreferences,
    permissionSource: PermissionSource,
    onOpenPlaces: () -> Unit,
    onTrackingChanged: (Boolean) -> Unit,
    onIntervalChanged: (Int) -> Unit,
    onUnitsChanged: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = RgDimens.Space4,
            vertical = RgDimens.Space3
        ),
        verticalArrangement = Arrangement.spacedBy(RgDimens.Space3)
    ) {
        when (section) {
            SettingsSection.TRACKING -> {
                item {
                    SettingsInfoCard(
                        icon = RgIcon.Shield,
                        message = stringResource(
                            if (preferences.trackingEnabled) R.string.settings_tracking_active
                            else R.string.settings_tracking_inactive
                        )
                    )
                }
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_automatic_tracking),
                        subtitle = stringResource(R.string.settings_automatic_tracking_hint),
                        icon = RgIcon.Clock,
                        trailing = {
                            Switch(
                                checked = preferences.trackingEnabled,
                                onCheckedChange = onTrackingChanged
                            )
                        }
                    )
                }
                item {
                    var expanded by remember { mutableStateOf(false) }
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(Modifier.padding(RgDimens.Space4)) {
                            Text(
                                text = stringResource(R.string.settings_tracking_interval),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(RgDimens.Space2))
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it }
                            ) {
                                OutlinedTextField(
                                    value = stringResource(
                                        when (preferences.intervalSeconds) {
                                            10 -> R.string.settings_interval_10_seconds
                                            30 -> R.string.settings_interval_30_seconds
                                            300 -> R.string.settings_interval_5_minutes
                                            else -> R.string.settings_interval_1_minute
                                        }
                                    ),
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listOf(
                                        10 to R.string.settings_interval_10_seconds,
                                        30 to R.string.settings_interval_30_seconds,
                                        60 to R.string.settings_interval_1_minute,
                                        300 to R.string.settings_interval_5_minutes
                                    ).forEach { (seconds, label) ->
                                        DropdownMenuItem(
                                            text = { Text(stringResource(label)) },
                                            onClick = {
                                                onIntervalChanged(seconds)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_location_permission),
                        subtitle = permissionStatus(
                            permissionSource.hasLocationPermission(),
                            R.string.settings_allowed,
                            R.string.settings_permission_missing
                        ),
                        icon = RgIcon.Map
                    )
                }
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_notifications_permission),
                        subtitle = permissionStatus(
                            permissionSource.hasNotificationsPermission(),
                            R.string.settings_allowed,
                            R.string.settings_permission_missing
                        ),
                        icon = RgIcon.Check
                    )
                }
            }

            SettingsSection.PRIVACY -> {
                item {
                    SettingsInfoCard(
                        icon = RgIcon.Shield,
                        message = stringResource(R.string.settings_privacy_intro)
                    )
                }
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_private_zones),
                        subtitle = stringResource(R.string.settings_private_zones_hint),
                        icon = RgIcon.Shield,
                        onClick = onOpenPlaces
                    )
                }
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_local_storage),
                        subtitle = stringResource(R.string.settings_local_storage_hint),
                        icon = RgIcon.Clock
                    )
                }
                item {
                    SettingsInfoCard(
                        icon = RgIcon.Trash,
                        message = stringResource(R.string.settings_privacy_actions_unavailable)
                    )
                }
            }

            SettingsSection.ENVIRONMENTAL -> {
                item {
                    SettingsInfoCard(
                        icon = RgIcon.Info,
                        message = stringResource(R.string.settings_environmental_notice)
                    )
                }
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_data_sources),
                        subtitle = stringResource(R.string.settings_data_sources_hint),
                        icon = RgIcon.Globe
                    )
                }
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_aqi_standard),
                        subtitle = stringResource(R.string.settings_aqi_standard_value),
                        icon = RgIcon.Leaf
                    )
                }
            }

            SettingsSection.NOTIFICATIONS -> {
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_notifications_permission),
                        subtitle = permissionStatus(
                            permissionSource.hasNotificationsPermission(),
                            R.string.settings_allowed,
                            R.string.settings_permission_missing
                        ),
                        icon = RgIcon.Bell
                    )
                }
                item {
                    SettingsInfoCard(
                        icon = RgIcon.Info,
                        message = stringResource(R.string.settings_notifications_hint)
                    )
                }
            }

            SettingsSection.MAP -> {
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_map_layers),
                        subtitle = stringResource(R.string.settings_map_layers_hint),
                        icon = RgIcon.Layers
                    )
                }
                item {
                    SettingsInfoCard(
                        icon = RgIcon.Info,
                        message = stringResource(R.string.settings_map_options_unavailable)
                    )
                }
            }

            SettingsSection.UNITS_LANGUAGE -> {
                item {
                    Text(
                        text = stringResource(R.string.settings_units),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = RgDimens.Space2)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(RgDimens.Space2)) {
                        FilterChip(
                            selected = preferences.units == "metric",
                            onClick = { onUnitsChanged("metric") },
                            label = { Text(stringResource(R.string.settings_metric)) }
                        )
                        FilterChip(
                            selected = preferences.units == "imperial",
                            onClick = { onUnitsChanged("imperial") },
                            label = { Text(stringResource(R.string.settings_imperial)) }
                        )
                    }
                }
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_language),
                        subtitle = stringResource(R.string.settings_language_system),
                        icon = RgIcon.Globe
                    )
                }
            }

            SettingsSection.ACCOUNT -> {
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_local_profile),
                        subtitle = stringResource(R.string.settings_local_profile_hint),
                        icon = RgIcon.User
                    )
                }
                item {
                    SettingsInfoCard(
                        icon = RgIcon.Info,
                        message = stringResource(R.string.settings_account_optional)
                    )
                }
            }

            SettingsSection.ABOUT -> {
                item {
                    SettingsRow(
                        title = stringResource(R.string.app_name),
                        subtitle = stringResource(R.string.settings_version),
                        icon = RgIcon.Info
                    )
                }
                item {
                    SettingsInfoCard(
                        icon = RgIcon.Leaf,
                        message = stringResource(R.string.settings_about_sources)
                    )
                }
            }

            SettingsSection.MAIN -> Unit
        }
    }
}

@Composable
private fun permissionStatus(granted: Boolean, grantedRes: Int, missingRes: Int): String =
    stringResource(if (granted) grantedRes else missingRes)

@Composable
private fun SettingsInfoCard(icon: RgIcon.IconData, message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RgDimens.Space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RgIcon.RgIconComposable(icon = icon, modifier = Modifier.size(RgDimens.IconLarge))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = RgDimens.Space3)
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: RgIcon.IconData,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RgDimens.Space4),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RgIcon.RgIconComposable(
                icon = icon.copy(color = Color(0xFF317854)),
                modifier = Modifier.size(RgDimens.IconLarge)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = RgDimens.Space4)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing?.invoke()
        }
    }
    if (onClick == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            rowContent()
        }
    } else {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            rowContent()
        }
    }
}

private fun SettingsSection.subtitleRes(): Int = when (this) {
    SettingsSection.TRACKING -> R.string.settings_tracking_subtitle
    SettingsSection.PRIVACY -> R.string.settings_privacy_subtitle
    SettingsSection.ENVIRONMENTAL -> R.string.settings_environmental_subtitle
    SettingsSection.NOTIFICATIONS -> R.string.settings_notifications_subtitle
    SettingsSection.MAP -> R.string.settings_map_subtitle
    SettingsSection.UNITS_LANGUAGE -> R.string.settings_units_subtitle
    SettingsSection.ACCOUNT -> R.string.settings_account_subtitle
    SettingsSection.ABOUT -> R.string.settings_about_subtitle
    SettingsSection.MAIN -> R.string.settings_personalize
}
