package com.roadguide.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.roadguide.app.R
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
import com.roadguide.app.feature.settings.SettingsSection
import com.roadguide.app.feature.timeline.TimelineScreen
import com.roadguide.app.feature.tracker.TrackerScreen
import com.roadguide.app.core.ui.theme.RgIcon
import dagger.Lazy

private data class BottomNavItem(
    val route: String,
    val label: Int,
    val icon: RgIcon.IconData
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
        BottomNavItem(ScreenRoutes.Tracker, R.string.nav_map, RgIcon.MapPin),
        BottomNavItem(ScreenRoutes.Timeline, R.string.nav_timeline, RgIcon.Calendar),
        BottomNavItem(ScreenRoutes.Planner, R.string.nav_planner, RgIcon.Route),
        BottomNavItem(ScreenRoutes.History, R.string.nav_history, RgIcon.History),
        BottomNavItem(ScreenRoutes.Places, R.string.nav_places, RgIcon.Home),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (items.any { it.route == currentRoute }) {
                NavigationBar {
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
                                RgIcon.RgIconComposable(
                                    icon = item.icon.copy(
                                        color = if (selected) {
                                            Color(0xFF317854)
                                        } else {
                                            Color(0xFF596B61)
                                        }
                                    ),
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(item.label)) }
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
                    onOpenProfile = { navController.navigate(ScreenRoutes.Profile) },
                    onOpenMapSettings = { navController.navigate(ScreenRoutes.SettingsMap) }
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
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(ScreenRoutes.Settings) }
                )
            }
            composable(ScreenRoutes.Settings) {
                SettingsScreen(
                    section = SettingsSection.MAIN,
                    onBack = { navController.popBackStack() },
                    onOpenSection = { section -> navController.navigate(section.route) },
                    onOpenPlaces = { navController.navigate(ScreenRoutes.Places) }
                )
            }
            SettingsSection.entries.filter { it != SettingsSection.MAIN }.forEach { section ->
                composable(section.route) {
                    SettingsScreen(
                        section = section,
                        onBack = { navController.popBackStack() },
                        onOpenSection = { next -> navController.navigate(next.route) },
                        onOpenPlaces = { navController.navigate(ScreenRoutes.Places) }
                    )
                }
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
                DailyReportScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTimeline = { navController.navigate(ScreenRoutes.Timeline) },
                    onOpenPlanner = { navController.navigate(ScreenRoutes.Planner) }
                )
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
