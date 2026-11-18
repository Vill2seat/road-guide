package com.roadguide.app.feature.places

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roadguide.app.R
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.data.local.DataProvider
import com.roadguide.app.data.local.entity.Place
import com.roadguide.app.data.local.entity.PlaceCategory
import com.roadguide.app.data.repository.PlaceRepositoryImpl
import com.roadguide.app.feature.places.viewmodel.PlacesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    modifier: Modifier = Modifier,
    onPlaceClick: ((Place) -> Unit)? = null
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.places_title)) }
            )
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = state.message,
                    onRetry = { placesViewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    val state = uiState
                    if (places.isEmpty()) {
                        EmptyPlacesState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = places,
                                key = { it.id }
                            ) { place ->
                                PlaceCard(
                                    place = place,
                                    onClick = { onPlaceClick?.invoke(place) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = RgDimens.Space3)
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
private fun EmptyPlacesState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RgIcon.RgIconComposable(
            icon = RgIcon.MapPin,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(RgDimens.Space3))
        Text(
            text = stringResource(R.string.places_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(RgDimens.Space2))
        Text(
            text = stringResource(R.string.places_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(RgDimens.Space4))
        Button(
            onClick = { /* TODO: open add place dialog */ },
            colors = ButtonDefaults.buttonColors(containerColor = RgColor.Primary)
        ) {
            Text(stringResource(R.string.places_add_place))
        }
    }
}

@Composable
private fun PlaceCard(
    place: Place,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val categoryColor = categoryColorFor(place.category)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (place.address != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = place.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = place.category.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = categoryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    RgIcon.RgIconComposable(
                        icon = categoryIconFor(place.category),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaceStatItem(
                    label = "Радиус",
                    value = "${place.radiusMeters.toInt()} м"
                )
                PlaceStatItem(
                    label = "Посещений",
                    value = place.visitCount.toString()
                )
                PlaceStatItem(
                    label = "Время",
                    value = formatDuration(place.totalDurationSeconds)
                )
            }

            if (place.isPrivate) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RgIcon.RgIconComposable(
                        icon = RgIcon.Shield,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Приватное место",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDuration(seconds: Long): String {
    if (seconds < 60) return "${seconds} с"
    val minutes = seconds / 60
    if (minutes < 60) return "${minutes} мин"
    val hours = minutes / 60
    return "${hours} ч ${minutes % 60} мин"
}

private fun categoryIconFor(category: PlaceCategory): RgIcon.IconData {
    return when (category) {
        PlaceCategory.HOME -> RgIcon.Home
        PlaceCategory.WORK -> RgIcon.Work
        PlaceCategory.SCHOOL -> RgIcon.School
        PlaceCategory.GYM -> RgIcon.Shield
        PlaceCategory.PARK -> RgIcon.Tree
        PlaceCategory.SHOP -> RgIcon.Bag
        PlaceCategory.RESTAURANT -> RgIcon.Droplets
        PlaceCategory.TRANSIT -> RgIcon.Bus
        PlaceCategory.OTHER -> RgIcon.MapPin
    }
}

private fun categoryColorFor(category: PlaceCategory): Color {
    return when (category) {
        PlaceCategory.HOME -> RgColor.Primary
        PlaceCategory.WORK -> RgColor.Secondary
        PlaceCategory.SCHOOL -> Color(0xFF8FC9A8)
        PlaceCategory.GYM -> Color(0xFFE7D18B)
        PlaceCategory.PARK -> RgColor.ExposureGood
        PlaceCategory.SHOP -> Color(0xFFE8A77F)
        PlaceCategory.RESTAURANT -> Color(0xFFD98787)
        PlaceCategory.TRANSIT -> RgColor.Info
        PlaceCategory.OTHER -> RgColor.OnSurfaceVariant
    }
}

private val PlaceCategory.displayName: String
    get() = when (this) {
        PlaceCategory.HOME -> "Дом"
        PlaceCategory.WORK -> "Работа"
        PlaceCategory.SCHOOL -> "Школа"
        PlaceCategory.GYM -> "Тренажерный зал"
        PlaceCategory.PARK -> "Парк"
        PlaceCategory.SHOP -> "Магазин"
        PlaceCategory.RESTAURANT -> "Ресторан"
        PlaceCategory.TRANSIT -> "Транспорт"
        PlaceCategory.OTHER -> "Другое"
    }
