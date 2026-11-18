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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.roadguide.app.core.ui.theme.RoadGuideTheme
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.feature.onboarding.OnboardingFlow
import com.roadguide.app.feature.permissions.PermissionScreen
import dagger.hilt.android.AndroidEntryPoint
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
        val hasPermissions = remember { mutableStateOf(permissionSource.hasAllPermissions()) }
        val hasBeenAsked = remember { mutableStateOf(false) }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { granted ->
            val allGranted = granted.values.all { it }
            hasPermissions.value = allGranted
            hasBeenAsked.value = true
        }

        val onboardingDone by permissionSource.hasCompletedOnboarding().collectAsState(initial = false)

        if (!onboardingDone) {
            OnboardingFlow(
                onFinished = {
                    hasPermissions.value = true
                    hasBeenAsked.value = true
                    navController.navigate(ScreenRoutes.Tracker) {
                        popUpTo(ScreenRoutes.Permission) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (!hasPermissions.value) {
            PermissionScreen(
                onGrantPermissions = {
                    val missing = permissionSource.getMissingPermissions()
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    } else {
                        hasPermissions.value = true
                        hasBeenAsked.value = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AppNavHost(
                permissionSource = permissionSource,
                permissionLauncher = permissionLauncher
            )
        }
    }
}
