package com.roadguide.app.feature.onboarding

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.feature.onboarding.viewmodel.OnboardingViewModel
import com.roadguide.app.feature.permissions.PermissionScreen

@Composable
fun OnboardingFlow(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val permissionSource = onboardingViewModel.permissionSource

    val currentPageState = onboardingViewModel.currentPage.collectAsState()
    val currentPage = currentPageState.value

    val permissionLauncher: ActivityResultLauncher<Array<String>> = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val allGranted = granted.values.all { it }
        if (allGranted) {
            onboardingViewModel.completeOnboarding()
            onFinished()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            when (currentPage) {
                0 -> WelcomeScreen(
                    onNext = { onboardingViewModel.nextPage() },
                    modifier = Modifier.fillMaxSize()
                )
                1 -> InfoScreen(
                    onBack = { onboardingViewModel.previousPage() },
                    onNext = { onboardingViewModel.nextPage() },
                    modifier = Modifier.fillMaxSize()
                )
                2 -> PermissionScreen(
                    onGrantPermissions = {
                        val activity = context as? Activity ?: return@PermissionScreen
                        val missing = permissionSource.getMissingPermissions()
                        if (missing.isNotEmpty()) {
                            permissionLauncher.launch(missing.toTypedArray())
                        } else {
                            onboardingViewModel.completeOnboarding()
                            onFinished()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                else -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
