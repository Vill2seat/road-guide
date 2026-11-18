package com.roadguide.app.feature.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.data.source.PermissionSource
import com.roadguide.app.feature.onboarding.viewmodel.OnboardingViewModel
import com.roadguide.app.feature.permissions.PermissionScreen

/**
 * Onboarding flow container.
 * Shows Welcome → Info → Permissions screens, then marks onboarding complete.
 *
 * @param onFinished callback invoked when onboarding is fully completed.
 */
@Composable
fun OnboardingFlow(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionSource = PermissionSource(androidx.compose.ui.platform.LocalContext.current)
    val onboardingViewModel: OnboardingViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return OnboardingViewModel(
                    permissionSource = permissionSource
                ) as T
            }
        }
    )

    val currentPageState = onboardingViewModel.currentPage.collectAsState()
    val currentPage = currentPageState.value

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
                        onboardingViewModel.completeOnboarding()
                        onFinished()
                    },
                    modifier = Modifier.fillMaxSize()
                )
                else -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
