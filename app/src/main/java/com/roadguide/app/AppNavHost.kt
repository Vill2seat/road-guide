package com.roadguide.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomNavigation
import androidx.compose.material3.BottomNavigationItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roadguide.app.domain.model.RouteTab
import com.roadguide.app.feature.history.HistoryScreen
import com.roadguide.app.feature.places.PlacesScreen
import com.roadguide.app.feature.planner.PlannerScreen
import com.roadguide.app.feature.profile.ProfileScreen
import com.roadguide.app.feature.tracker.TrackerScreen
import com.roadguide.app.feature.timeline.TimelineScreen

/**
 * Main navigation host with bottom navigation bar.
 *
 * Bottom tabs: Map (Tracker), Timeline, Planner, History, Places.
 * Profile/Settings accessible via top bar entry point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigation {
                var selectedTab by remember { mutableStateOf(RouteTab.TRACKER) }

                RouteTab.all().forEach { tab ->
                    BottomNavigationItem(
                        icon = {
                            FilledIcon(
                                imageVector = when (tab) {
                                    RouteTab.TRACKER -> FilledIcon.Build
                                    RouteTab.TIMELINE -> FilledIcon.Calendar
                                    RouteTab.PLANNER -> FilledIcon.Add
                                    RouteTab.HISTORY -> FilledIcon.ArrowForward
                                    RouteTab.PLACES -> FilledIcon.Place
                                },
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(text = tab.label) },
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            val route = ScreenRoutes.tabToRoute(tab)
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = true
                                }
                            }
                        }
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
                ProfileScreen()
            }
        }
    }
}
