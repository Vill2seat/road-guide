package com.roadguide.app.feature.places

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.feature.places.viewmodel.PlacesViewModel

@Composable
fun PlacesScreen(
    placesViewModel: PlacesViewModel = viewModel(
        factory = { PlacesViewModel(
            placeRepository = com.roadguide.app.data.repository.PlaceRepositoryImpl(
                dao = com.roadguide.app.data.local.db.RoadGuideDatabase.getInstance(
                    androidx.lifecycle.viewmodel.viewModelScopeLocalContext.current.context
                ).placeDao
            ),
            context = androidx.lifecycle.viewmodel.viewModelScopeLocalContext.current.context
        ) }
    ),
    modifier: Modifier = Modifier
) {
    val uiState by placesViewModel.uiState.collectAsState()
    val places by placesViewModel.places.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Places", style = MaterialTheme.typography.headlineSmall) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (uiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = uiState.message,
                    onRetry = { placesViewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = androidx.compose.foundation.layout.VerticalArrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${places.size} places",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        places.forEach { place ->
                            Card(modifier = Modifier.fillMaxSize()) {
                                com.roadguide.app.feature.places.PlacesScreenPlaceItem(
                                    place = place,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlacesScreenPlaceItem(
    place: com.roadguide.app.data.local.entity.Place,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = androidx.compose.foundation.layout.Alignment.CenterHorizontally
    ) {
        Text(
            text = place.name,
            style = MaterialTheme.typography.bodyLarge
        )
                                Text(
                                    text = place.userLabel ?: place.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
    }
}
