package com.roadguide.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.roadguide.app.R
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.feature.dailyreport.DailyReportScreen
import com.roadguide.app.feature.history.HistoryScreen
import com.roadguide.app.feature.onboarding.OnboardingFlow
import com.roadguide.app.feature.permissions.PermissionScreen
import com.roadguide.app.feature.places.PlacesScreen
import com.roadguide.app.feature.weather.WeatherScreen
import com.roadguide.app.feature.planner.PlannerScreen
import com.roadguide.app.feature.profile.ProfileScreen
import com.roadguide.app.feature.settings.SettingsScreen
import com.roadguide.app.feature.timeline.TimelineScreen
import com.roadguide.app.feature.tracker.TrackerScreen

private data class BottomNavItem(
    val route: String,
    val label: Int,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    permissionSource: PermissionSource,
    permissionLauncher: ActivityResultLauncher<Array<String>>
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem(ScreenRoutes.Tracker, R.string.nav_map, Icons.Default.Place),
        BottomNavItem(ScreenRoutes.Timeline, R.string.nav_timeline, Icons.Default.Info),
        BottomNavItem(ScreenRoutes.Planner, R.string.nav_planner, Icons.Default.Search),
        BottomNavItem(ScreenRoutes.History, R.string.nav_history, Icons.Default.Home),
        BottomNavItem(ScreenRoutes.Places, R.string.nav_places, Icons.Default.Person),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = { navController.navigate(ScreenRoutes.Profile) }) {
                        Text(stringResource(R.string.nav_profile))
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = stringResource(item.label)) },
                        label = { Text(stringResource(item.label)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ScreenRoutes.Tracker,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ScreenRoutes.Tracker) {
                TrackerScreen()
            }
            composable(ScreenRoutes.Timeline) {
                TimelineScreen()
            }
            composable(ScreenRoutes.Planner) {
                PlannerScreen()
            }
            composable(ScreenRoutes.History) {
                HistoryScreen()
            }
            composable(ScreenRoutes.Places) {
                PlacesScreen()
            }
            composable(ScreenRoutes.Profile) {
                ProfileScreen()
            }
            composable(ScreenRoutes.Settings) {
                SettingsScreen()
            }
            composable(ScreenRoutes.Permission) {
                PermissionScreen(
                    onGrantPermissions = {
                        val missing = permissionSource.getMissingPermissions()
                        if (missing.isNotEmpty()) {
                            permissionLauncher.launch(missing.toTypedArray())
                        } else {
                            navController.navigate(ScreenRoutes.Tracker) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
            composable(ScreenRoutes.Weather) {
                WeatherScreen()
            }
            composable(ScreenRoutes.DailyReport) {
                DailyReportScreen()
            }
            composable(ScreenRoutes.Onboarding) {
                OnboardingFlow(
                    onFinished = {
                        navController.navigate(ScreenRoutes.Tracker) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
