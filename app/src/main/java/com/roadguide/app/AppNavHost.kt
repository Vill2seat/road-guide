package com.roadguide.app

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.roadguide.app.R
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.data.source.OsrmRoutingSource
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.domain.usecase.impl.RouteComparisonEngine
import com.roadguide.app.domain.usecase.impl.RouteEvaluationEngine
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.feature.dailyreport.DailyReportScreen
import com.roadguide.app.feature.history.HistoryScreen
import com.roadguide.app.feature.onboarding.OnboardingFlow
import com.roadguide.app.feature.permissions.PermissionScreen
import com.roadguide.app.feature.planner.PlannerScreen
import com.roadguide.app.feature.places.PlacesScreen
import com.roadguide.app.feature.profile.ProfileScreen
import com.roadguide.app.feature.settings.SettingsScreen
import com.roadguide.app.feature.settings.SettingsSection
import com.roadguide.app.feature.timeline.TimelineScreen
import com.roadguide.app.feature.tracker.TrackerScreen
import com.roadguide.app.feature.weather.WeatherScreen

private data class BottomNavItem(
    val route: String,
    val label: Int,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    permissionSource: PermissionSource,
    permissionLauncher: ActivityResultLauncher<Array<String>>,
    locationSource: LocationSource? = null,
    osrmRoutingSource: OsrmRoutingSource? = null,
    routeEvaluationEngine: RouteEvaluationEngine? = null,
    routeComparisonEngine: RouteComparisonEngine? = null
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
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    androidx.compose.material3.TextButton(onClick = { navController.navigate(ScreenRoutes.Profile) }) {
                        Text(stringResource(R.string.nav_profile))
                    }
                }
            )
        },
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
                PlannerScreen(
                    locationSource = locationSource,
                    osrmRoutingSource = osrmRoutingSource,
                    routeEvaluationEngine = routeEvaluationEngine,
                    routeComparisonEngine = routeComparisonEngine
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
