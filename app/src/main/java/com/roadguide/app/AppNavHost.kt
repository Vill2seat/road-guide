package com.roadguide.app

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.roadguide.app.feature.history.HistoryScreen
import com.roadguide.app.feature.places.PlacesScreen
import com.roadguide.app.feature.planner.PlannerScreen
import com.roadguide.app.feature.profile.ProfileScreen
import com.roadguide.app.feature.timeline.TimelineScreen
import com.roadguide.app.feature.tracker.TrackerScreen

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem(ScreenRoutes.Tracker, "Map", Icons.Default.Place),
        BottomNavItem(ScreenRoutes.Timeline, "Timeline", Icons.Default.Info),
        BottomNavItem(ScreenRoutes.Planner, "Planner", Icons.Default.Search),
        BottomNavItem(ScreenRoutes.History, "History", Icons.Default.Home),
        BottomNavItem(ScreenRoutes.Places, "Places", Icons.Default.Person),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Road Guide") },
                actions = {
                    TextButton(onClick = { navController.navigate(ScreenRoutes.Profile) }) {
                        Text("Profile")
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
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
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
