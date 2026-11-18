package com.roadguide.app.feature.places

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.roadguide.app.R
import com.roadguide.app.core.common.ScreenState
import com.roadguide.app.core.ui.theme.ErrorPlaceholder
import com.roadguide.app.core.ui.theme.LoadingPlaceholder
import com.roadguide.app.core.ui.theme.RgColor
import com.roadguide.app.core.ui.theme.RgDimens
import com.roadguide.app.core.ui.theme.RgIcon
import com.roadguide.app.data.local.entity.Place
import com.roadguide.app.data.local.entity.PlaceCategory
import com.roadguide.app.feature.places.viewmodel.PlacesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesScreen(
    modifier: Modifier = Modifier,
    onPlaceClick: ((Place) -> Unit)? = null
) {
    val placesViewModel: PlacesViewModel = hiltViewModel()

    val uiState by placesViewModel.uiState.collectAsState()
    val places by placesViewModel.places.collectAsState()

    // Search state
    val searchQuery = remember { mutableStateOf("") }

    // Filter places by search query
    val filteredPlaces = remember(places, searchQuery) {
        if (searchQuery.value.isEmpty()) {
            places
        } else {
            val query = searchQuery.value.lowercase()
            places.filter { place ->
                place.name.lowercase().contains(query) ||
                place.address?.let { it.lowercase().contains(query) } == true
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.places_title)) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(RgDimens.Space2)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = {
                    Text(
                        text = "Поиск места (дом, работа, парк, школа...)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RgColor.Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(RgDimens.Space4))

            when (val state = uiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = state.message,
                    onRetry = { placesViewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    if (filteredPlaces.isEmpty()) {
                        // Show empty state (no results or empty search)
                        if (searchQuery.value.isEmpty()) {
                            // Initial empty state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    RgIcon.RgIconComposable(
                                        icon = RgIcon.MapPin,
                                        modifier = Modifier.size(RgDimens.IconLarge)
                                    )
                                    Spacer(modifier = Modifier.height(RgDimens.Space3))
                                    Text(
                                        text = "Места не найдены",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(RgDimens.Space2))
                                    Text(
                                        text = "Чтобы добавить место, откройте меню и выберите "+
                                        "«Добавить место»",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            // Search returned no results
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Ничего не найдено",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(RgDimens.Space3))
                                    Text(
                                        text = "По запросу «${searchQuery.value}» ничего не найдено. "+
                                        "Попробуйте другой запрос.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(RgDimens.Space4))
                                    Button(
                                        onClick = { searchQuery.value = "" },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = RgColor.Primary
                                        )
                                    ) {
                                        Text("Очистить поиск")
                                    }
                                }
                            }
                        }
                    } else {
                        // Results found
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = filteredPlaces,
                                key = { it.id }
                            ) { place ->
                                PlaceCard(
                                    place = place,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    onPlaceClick = onPlaceClick
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
private fun PlaceCard(
    place: Place,
    modifier: Modifier = Modifier,
    onPlaceClick: ((Place) -> Unit)? = null
) {
    Card(
        modifier = modifier,
        onClick = { onPlaceClick?.invoke(place) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = place.address ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                RgIcon.RgIconComposable(
                    icon = categoryIconFor(place.category),
                    modifier = Modifier.size(RgDimens.IconMedium)
                )
            }

            Spacer(modifier = Modifier.height(RgDimens.Space3))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Посещений: ${place.visitCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = place.category.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = categoryColorFor(place.category)
                    )
                }
                if (place.isPrivate) {
                    RgIcon.RgIconComposable(
                        icon = RgIcon.Shield,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
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

private fun categoryColorFor(category: PlaceCategory): androidx.compose.ui.graphics.Color {
    return when (category) {
        PlaceCategory.HOME -> com.roadguide.app.core.ui.theme.RgColor.Primary
        PlaceCategory.WORK -> com.roadguide.app.core.ui.theme.RgColor.Secondary
        PlaceCategory.SCHOOL -> androidx.compose.ui.graphics.Color(0xFF8FC9A8)
        PlaceCategory.GYM -> androidx.compose.ui.graphics.Color(0xFFE7D18B)
        PlaceCategory.PARK -> com.roadguide.app.core.ui.theme.RgColor.ExposureGood
        PlaceCategory.SHOP -> androidx.compose.ui.graphics.Color(0xFFE8A77F)
        PlaceCategory.RESTAURANT -> androidx.compose.ui.graphics.Color(0xFFD98787)
        PlaceCategory.TRANSIT -> com.roadguide.app.core.ui.theme.RgColor.Info
        PlaceCategory.OTHER -> com.roadguide.app.core.ui.theme.RgColor.OnSurfaceVariant
    }
}
