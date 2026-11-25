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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.roadguide.app.R
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.feature.dailyreport.DailyReportScreen
import com.roadguide.app.feature.history.HistoryScreen
import com.roadguide.app.feature.onboarding.OnboardingFlow
import com.roadguide.app.feature.permissions.PermissionScreen
import com.roadguide.app.feature.places.PlacesScreen
import com.roadguide.app.feature.weather.WeatherScreen
import com.roadguide.app.data.source.OsrmRoutingSource
import com.roadguide.app.domain.usecase.impl.RouteComparisonEngine
import com.roadguide.app.domain.usecase.impl.RouteEvaluationEngine
import com.roadguide.app.feature.planner.PlannerScreen
import com.roadguide.app.feature.profile.ProfileScreen
import com.roadguide.app.feature.settings.SettingsScreen
import com.roadguide.app.feature.timeline.TimelineScreen
import com.roadguide.app.feature.tracker.TrackerScreen
import dagger.Lazy

private data class BottomNavItem(
    val route: String,
    val label: Int,
    val icon: ImageVector
)

@Composable
fun AppNavHost(
    permissionSource: PermissionSource,
    permissionLauncher: ActivityResultLauncher<Array<String>>,
    locationSource: Lazy<LocationSource>? = null,
    osrmRoutingSource: Lazy<OsrmRoutingSource>? = null,
    routeEvaluationEngine: Lazy<RouteEvaluationEngine>? = null,
    routeComparisonEngine: Lazy<RouteComparisonEngine>? = null
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem(ScreenRoutes.Tracker, R.string.nav_map, Icons.Default.Map),
        BottomNavItem(ScreenRoutes.Timeline, R.string.nav_timeline, Icons.Default.CalendarMonth),
        BottomNavItem(ScreenRoutes.Planner, R.string.nav_planner, Icons.Default.Route),
        BottomNavItem(ScreenRoutes.History, R.string.nav_history, Icons.Default.History),
        BottomNavItem(ScreenRoutes.Places, R.string.nav_places, Icons.Default.Place),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on main tab screens
    val showBottomBar = currentRoute in items.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = RgColor.Surface,
                    tonalElevation = androidx.compose.ui.unit.Dp.Hairline
                ) {
                    items.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    item.icon,
                                    contentDescription = stringResource(item.label),
                                    modifier = Modifier.padding(0.dp)
                                )
                            },
                            label = { Text(stringResource(item.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RgColor.Primary,
                                selectedTextColor = RgColor.Primary,
                                indicatorColor = RgColor.PrimaryContainer,
                                unselectedIconColor = RgColor.OnSurfaceVariant,
                                unselectedTextColor = RgColor.OnSurfaceVariant
                            )
                        )
                    }
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
                TrackerScreen(
                    onOpenTimeline = { navController.navigate(ScreenRoutes.Timeline) },
                    onOpenDailyReport = { navController.navigate(ScreenRoutes.DailyReport) },
                    onOpenProfile = { navController.navigate(ScreenRoutes.Profile) }
                )
            }
            composable(ScreenRoutes.Timeline) {
                TimelineScreen(
                    onOpenDailyReport = { navController.navigate(ScreenRoutes.DailyReport) }
                )
            }
            composable(ScreenRoutes.Planner) {
                val plannerLocationSource = remember(locationSource) { locationSource?.get() }
                val plannerRoutingSource = remember(osrmRoutingSource) { osrmRoutingSource?.get() }
                val plannerEvaluationEngine =
                    remember(routeEvaluationEngine) { routeEvaluationEngine?.get() }
                val plannerComparisonEngine =
                    remember(routeComparisonEngine) { routeComparisonEngine?.get() }
                PlannerScreen(
                    locationSource = plannerLocationSource,
                    osrmRoutingSource = plannerRoutingSource,
                    routeEvaluationEngine = plannerEvaluationEngine,
                    routeComparisonEngine = plannerComparisonEngine
                )
            }
            composable(ScreenRoutes.History) {
                HistoryScreen()
            }
            composable(ScreenRoutes.Places) {
                PlacesScreen()
            }
            composable(ScreenRoutes.Profile) {
                ProfileScreen(
                    onOpenSettings = { navController.navigate(ScreenRoutes.Settings) }
                )
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
