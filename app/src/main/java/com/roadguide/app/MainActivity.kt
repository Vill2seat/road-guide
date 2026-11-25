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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.roadguide.app.core.ui.theme.RoadGuideTheme
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.data.source.LocationSource
import com.roadguide.app.data.source.OsrmRoutingSource
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.domain.usecase.impl.RouteComparisonEngine
import com.roadguide.app.domain.usecase.impl.RouteEvaluationEngine
import com.roadguide.app.feature.onboarding.OnboardingFlow
import com.roadguide.app.feature.permissions.PermissionScreen
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var permissionSource: PermissionSource
    @Inject lateinit var locationSource: Lazy<LocationSource>
    @Inject lateinit var osrmRoutingSource: Lazy<OsrmRoutingSource>
    @Inject lateinit var routeEvaluationEngine: Lazy<RouteEvaluationEngine>
    @Inject lateinit var routeComparisonEngine: Lazy<RouteComparisonEngine>

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
        var hasPermissions by rememberSaveable { mutableStateOf(permissionSource.hasAllPermissions()) }
        val lifecycleOwner = LocalLifecycleOwner.current
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            val missing = permissionSource.getMissingPermissions()
            if (missing.isEmpty()) {
                hasPermissions = true
            } else if (granted.values.all { it }) {
                hasPermissions = permissionSource.hasAllPermissions()
            } else {
                hasPermissions = false
            }
        }

        DisposableEffect(lifecycleOwner, permissionSource) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasPermissions = permissionSource.hasAllPermissions()
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        // Collect onboarding completion from DataStore
        val onboardingDone by permissionSource.hasCompletedOnboarding()
            .collectAsState<Boolean, Boolean?>(initial = null)

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
                locationSource = locationSource.get(),
                osrmRoutingSource = osrmRoutingSource.get(),
                routeEvaluationEngine = routeEvaluationEngine.get(),
                routeComparisonEngine = routeComparisonEngine.get()
            )
        }
    }
}
