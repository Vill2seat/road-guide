package com.roadguide.app.feature.planner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.EmptyPlaceholder
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder

@Composable
fun PlannerScreen(
    state: ScreenState = ScreenState.Empty,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when (state) {
        ScreenState.Empty -> EmptyPlaceholder("Planner", "Eco route planner will be displayed here.", modifier)
        ScreenState.Loading -> LoadingPlaceholder(modifier)
        is ScreenState.Error -> ErrorPlaceholder(state.message, onRetry, modifier)
    }
}
