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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.data.local.DataProvider
import com.roadguide.app.data.local.db.RoadGuideDatabase
import com.roadguide.app.data.repository.PlaceRepositoryImpl
import com.roadguide.app.feature.places.viewmodel.PlacesViewModel

@Composable
fun PlacesScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dataProvider = DataProvider.getInstance(context)
    val placeRepository = PlaceRepositoryImpl(dataProvider.placeDao)

    val placesViewModel: PlacesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PlacesViewModel(
                    placeRepository = placeRepository,
                    context = context
                ) as T
            }
        }
    )

    val uiState by placesViewModel.uiState.collectAsState()
    val places by placesViewModel.places.collectAsState()

    val currentUiState = uiState

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentUiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = currentUiState.message,
                    onRetry = { placesViewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "${places.size} places",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        places.forEach { place ->
                            Card(modifier = Modifier.fillMaxSize()) {
                                PlacesScreenPlaceItem(
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
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = place.name,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = place.category.toString(),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
