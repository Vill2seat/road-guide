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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.roadguide.app.core.ui.theme.RoadGuideTheme
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.data.source.OsrmRoutingSource
import com.roadguide.app.domain.usecase.impl.RouteComparisonEngine
import com.roadguide.app.domain.usecase.impl.RouteEvaluationEngine
import com.roadguide.app.feature.onboarding.OnboardingFlow
import com.roadguide.app.feature.permissions.PermissionScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var permissionSource: PermissionSource
    @Inject lateinit var osrmRoutingSource: OsrmRoutingSource
    @Inject lateinit var routeEvaluationEngine: RouteEvaluationEngine
    @Inject lateinit var routeComparisonEngine: RouteComparisonEngine

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
        var hasPermissions by remember { mutableStateOf(permissionSource.hasAllPermissions()) }
        var hasBeenAsked by remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            hasPermissions = permissionSource.hasAllPermissions()
            hasBeenAsked = true
        }

        val onboardingDone by permissionSource.hasCompletedOnboarding().collectAsState(initial = false)

        LaunchedEffect(hasPermissions) {
            if (!hasPermissions && permissionSource.hasAllPermissions()) {
                hasPermissions = true
            }
        }

        if (!onboardingDone) {
            OnboardingFlow(
                onFinished = {
                    hasPermissions = permissionSource.hasAllPermissions()
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (!hasPermissions) {
            PermissionScreen(
                onGrantPermissions = {
                    val missing = permissionSource.getMissingPermissions()
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    } else {
                        hasPermissions = true
                        hasBeenAsked = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AppNavHost(
                permissionSource = permissionSource,
                permissionLauncher = permissionLauncher,
                osrmRoutingSource = osrmRoutingSource,
                routeEvaluationEngine = routeEvaluationEngine,
                routeComparisonEngine = routeComparisonEngine
            )
        }
    }
}