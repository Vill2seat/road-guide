package com.roadguide.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.compose.LocalLifecycleOwner
import com.roadguide.app.core.ui.theme.RoadGuideTheme
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.feature.onboarding.OnboardingFlow
import com.roadguide.app.feature.permissions.PermissionScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var permissionSource: PermissionSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoadGuideTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionGate()
                }
            }
        }
    }

    @Composable
    private fun PermissionGate() {
        // Use rememberSaveable to persist state across configuration changes
        var hasPermissions by rememberSaveable { mutableStateOf(permissionSource.hasAllPermissions()) }
        var hasBeenAsked by rememberSaveable { mutableStateOf(false) }
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            val allGranted = granted.values.all { it }
            hasPermissions = allGranted
            hasBeenAsked.value = true
        }

        // Sync state from PermissionSource on resume
        LaunchedEffect(Unit) {
            val currentHasPermission = permissionSource.hasAllPermissions()
            // Only update if state is different to avoid unnecessary recompositions
            if (hasPermissions != currentHasPermission) {
                hasPermissions = currentHasPermission
            }
        }

        // Collect onboarding completion from DataStore
        val onboardingDone by permissionSource.hasCompletedOnboarding().collectAsState(initial = false)

        // Sync hasPermissions from PermissionSource when onboarding completes
        LaunchedEffect(onboardingDone) {
            if (onboardingDone == true && !hasPermissions) {
                hasPermissions = permissionSource.hasAllPermissions()
            }
        }

        if (onboardingDone == null) {
            LoadingPlaceholder(modifier = Modifier.fillMaxSize())
        } else if (onboardingDone == false) {
            OnboardingFlow(
                onFinished = {
                    // State will be synced by the LaunchedEffect above
                }
            )
        } else if (!hasPermissions) {
            PermissionScreen(
                onGrantPermissions = {
                    val missing = permissionSource.getMissingPermissions()
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    } else {
                        hasPermissions = true
                    }
                }
            )
        } else {
            AppNavHost(
                permissionSource = permissionSource,
                permissionLauncher = permissionLauncher,
                locationSource = locationSource,
                osrmRoutingSource = osrmRoutingSource,
                routeEvaluationEngine = routeEvaluationEngine,
                routeComparisonEngine = routeComparisonEngine
            )
        }
    }
}
