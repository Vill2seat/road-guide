package com.roadguide.app.feature.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.roadguide.app.data.local.entity.PrivacyZone
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
    val privacyZones by placesViewModel.privacyZones.collectAsState()
    val actionError by placesViewModel.actionError.collectAsState()

    val selectedTab = remember { mutableStateOf(0) }
    val searchQuery = remember { mutableStateOf("") }
    val placeDialogOpen = remember { mutableStateOf(false) }
    val zoneDialogOpen = remember { mutableStateOf(false) }
    val editingPlace = remember { mutableStateOf<Place?>(null) }
    val editingZone = remember { mutableStateOf<PrivacyZone?>(null) }
    val deletingPlace = remember { mutableStateOf<Place?>(null) }
    val deletingZone = remember { mutableStateOf<PrivacyZone?>(null) }
    val query = searchQuery.value.trim()
    val filteredPlaces = remember(places, query) {
        if (query.isBlank()) places else places.filter { place ->
            place.name.contains(query, ignoreCase = true) ||
                place.address?.contains(query, ignoreCase = true) == true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.places_title)) },
                actions = {
                    TextButton(
                        onClick = {
                            if (selectedTab.value == 0) {
                                editingPlace.value = null
                                placeDialogOpen.value = true
                            } else {
                                editingZone.value = null
                                zoneDialogOpen.value = true
                            }
                        }
                    ) {
                        Text(
                            stringResource(
                                if (selectedTab.value == 0) R.string.place_add
                                else R.string.privacy_zone_add
                            )
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTab.value) {
                Tab(
                    selected = selectedTab.value == 0,
                    onClick = { selectedTab.value = 0 },
                    text = { Text(stringResource(R.string.places_tab)) }
                )
                Tab(
                    selected = selectedTab.value == 1,
                    onClick = { selectedTab.value = 1 },
                    text = { Text(stringResource(R.string.privacy_zones_tab)) }
                )
            }

            actionError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            when (val state = uiState) {
                ScreenState.Loading -> LoadingPlaceholder(modifier = Modifier.fillMaxSize())
                is ScreenState.Error -> ErrorPlaceholder(
                    message = state.message,
                    onRetry = { placesViewModel.loadData() },
                    modifier = Modifier.fillMaxSize()
                )
                ScreenState.Success, ScreenState.Empty -> {
                    if (selectedTab.value == 0) {
                        OutlinedTextField(
                            value = searchQuery.value,
                            onValueChange = { searchQuery.value = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            placeholder = { Text(stringResource(R.string.places_search)) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        if (filteredPlaces.isEmpty()) {
                            EmptyPanel(
                                title = stringResource(
                                    if (query.isBlank()) R.string.places_empty
                                    else R.string.places_no_search_results
                                ),
                                message = stringResource(
                                    if (query.isBlank()) R.string.places_empty_hint
                                    else R.string.places_search_hint
                                )
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredPlaces, key = Place::id) { place ->
                                    PlaceCard(
                                        place = place,
                                        onPlaceClick = { onPlaceClick?.invoke(place) },
                                        onEdit = {
                                            editingPlace.value = place
                                            placeDialogOpen.value = true
                                        },
                                        onDelete = { deletingPlace.value = place }
                                    )
                                }
                            }
                        }
                    } else if (privacyZones.isEmpty()) {
                        EmptyPanel(
                            title = stringResource(R.string.privacy_zone_empty),
                            message = stringResource(R.string.privacy_zone_empty_hint)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(privacyZones, key = PrivacyZone::id) { zone ->
                                PrivacyZoneCard(
                                    zone = zone,
                                    onActiveChanged = {
                                        placesViewModel.savePrivacyZone(zone.copy(isActive = it))
                                    },
                                    onEdit = {
                                        editingZone.value = zone
                                        zoneDialogOpen.value = true
                                    },
                                    onDelete = { deletingZone.value = zone }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (placeDialogOpen.value) {
        PlaceEditorDialog(
            place = editingPlace.value,
            onDismiss = { placeDialogOpen.value = false },
            onSave = {
                placesViewModel.savePlace(it)
                placeDialogOpen.value = false
            }
        )
    }
    if (zoneDialogOpen.value) {
        PrivacyZoneEditorDialog(
            zone = editingZone.value,
            onDismiss = { zoneDialogOpen.value = false },
            onSave = {
                placesViewModel.savePrivacyZone(it)
                zoneDialogOpen.value = false
            }
        )
    }
    deletingPlace.value?.let { place ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.place_delete_title),
            message = stringResource(R.string.place_delete_message, place.name),
            onDismiss = { deletingPlace.value = null },
            onConfirm = {
                placesViewModel.deletePlace(place.id)
                deletingPlace.value = null
            }
        )
    }
    deletingZone.value?.let { zone ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.privacy_zone_delete_title),
            message = stringResource(R.string.privacy_zone_delete_message, zone.name),
            onDismiss = { deletingZone.value = null },
            onConfirm = {
                placesViewModel.deletePrivacyZone(zone.id)
                deletingZone.value = null
            }
        )
    }
}

@Composable
private fun categoryLabel(category: PlaceCategory): String =
    stringResource(
        when (category) {
            PlaceCategory.HOME -> R.string.place_category_home
            PlaceCategory.WORK -> R.string.place_category_work
            PlaceCategory.SCHOOL -> R.string.place_category_school
            PlaceCategory.GYM -> R.string.place_category_gym
            PlaceCategory.PARK -> R.string.place_category_park
            PlaceCategory.SHOP -> R.string.place_category_shop
            PlaceCategory.RESTAURANT -> R.string.place_category_restaurant
            PlaceCategory.TRANSIT -> R.string.place_category_transit
            PlaceCategory.OTHER -> R.string.place_category_other
        }
    )

@Composable
private fun PlaceCard(
    place: Place,
    onPlaceClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPlaceClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(place.name, style = MaterialTheme.typography.titleMedium)
                    place.address?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
                RgIcon.RgIconComposable(
                    icon = categoryIconFor(place.category),
                    modifier = Modifier.size(RgDimens.IconMedium)
                )
                if (place.isPrivate) {
                    RgIcon.RgIconComposable(
                        icon = RgIcon.Shield,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.place_category_value, categoryLabel(place.category)),
                style = MaterialTheme.typography.bodySmall,
                color = categoryColorFor(place.category)
            )
            Text(
                text = stringResource(R.string.place_radius_value, place.radiusMeters),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(R.string.place_coordinates_value, place.latitude, place.longitude),
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.place_edit)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.place_delete)) }
            }
        }
    }
}

@Composable
private fun PrivacyZoneCard(
    zone: PrivacyZone,
    onActiveChanged: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(zone.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.privacy_zone_radius_value, zone.radiusMeters),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = zone.isActive, onCheckedChange = onActiveChanged)
            }
            Text(
                text = stringResource(R.string.privacy_zone_coordinates_value, zone.centerLatitude, zone.centerLongitude),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(
                    R.string.privacy_zone_policy_value,
                    stringResource(if (zone.excludeFromHistory) R.string.privacy_yes else R.string.privacy_no),
                    stringResource(if (zone.excludeFromExposure) R.string.privacy_yes else R.string.privacy_no)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.place_edit)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.place_delete)) }
            }
        }
    }
}

@Composable
private fun PlaceEditorDialog(
    place: Place?,
    onDismiss: () -> Unit,
    onSave: (Place) -> Unit
) {
    val name = remember(place?.id) { mutableStateOf(place?.name.orEmpty()) }
    val address = remember(place?.id) { mutableStateOf(place?.address.orEmpty()) }
    val latitude = remember(place?.id) { mutableStateOf(place?.latitude?.toString().orEmpty()) }
    val longitude = remember(place?.id) { mutableStateOf(place?.longitude?.toString().orEmpty()) }
    val radius = remember(place?.id) { mutableStateOf(place?.radiusMeters?.toString() ?: "100") }
    val category = remember(place?.id) { mutableStateOf(place?.category ?: PlaceCategory.OTHER) }
    val isPrivate = remember(place?.id) { mutableStateOf(place?.isPrivate ?: false) }
    val categoryMenuOpen = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf<String?>(null) }
    val invalidFormMessage = stringResource(R.string.places_invalid_form)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (place == null) R.string.place_add else R.string.place_edit))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(name.value, { name.value = it }, label = { Text(stringResource(R.string.place_name)) }, singleLine = true)
                OutlinedTextField(address.value, { address.value = it }, label = { Text(stringResource(R.string.place_address)) }, singleLine = true)
                OutlinedTextField(latitude.value, { latitude.value = it }, label = { Text(stringResource(R.string.place_latitude)) }, singleLine = true)
                OutlinedTextField(longitude.value, { longitude.value = it }, label = { Text(stringResource(R.string.place_longitude)) }, singleLine = true)
                OutlinedTextField(radius.value, { radius.value = it }, label = { Text(stringResource(R.string.place_radius)) }, singleLine = true)
                TextButton(onClick = { categoryMenuOpen.value = true }) {
                    Text(stringResource(R.string.place_category_value, categoryLabel(category.value)))
                }
                DropdownMenu(
                    expanded = categoryMenuOpen.value,
                    onDismissRequest = { categoryMenuOpen.value = false }
                ) {
                    PlaceCategory.entries.forEach { value ->
                        DropdownMenuItem(
                            text = { Text(categoryLabel(value)) },
                            onClick = {
                                category.value = value
                                categoryMenuOpen.value = false
                            }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isPrivate.value,
                        onCheckedChange = { isPrivate.value = it }
                    )
                    Text(stringResource(R.string.place_private_toggle))
                }
                error.value?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lat = latitude.value.toDoubleOrNull()
                    val lon = longitude.value.toDoubleOrNull()
                    val radiusMeters = radius.value.toDoubleOrNull()
                    if (
                        name.value.isBlank() ||
                        lat == null || !lat.isFinite() || lat !in -90.0..90.0 ||
                        lon == null || !lon.isFinite() || lon !in -180.0..180.0 ||
                        radiusMeters == null || !radiusMeters.isFinite() || radiusMeters <= 0.0
                    ) {
                        error.value = invalidFormMessage
                    } else {
                        onSave(
                            (place ?: Place(
                                name = name.value.trim(),
                                latitude = lat,
                                longitude = lon
                            )).copy(
                                name = name.value.trim(),
                                category = category.value,
                                latitude = lat,
                                longitude = lon,
                                radiusMeters = radiusMeters,
                                address = address.value.trim().ifBlank { null },
                                isPrivate = isPrivate.value
                            )
                        )
                    }
                }
            ) { Text(stringResource(R.string.place_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.place_cancel)) }
        }
    )
}

@Composable
private fun PrivacyZoneEditorDialog(
    zone: PrivacyZone?,
    onDismiss: () -> Unit,
    onSave: (PrivacyZone) -> Unit
) {
    val name = remember(zone?.id) { mutableStateOf(zone?.name.orEmpty()) }
    val latitude = remember(zone?.id) { mutableStateOf(zone?.centerLatitude?.toString().orEmpty()) }
    val longitude = remember(zone?.id) { mutableStateOf(zone?.centerLongitude?.toString().orEmpty()) }
    val radius = remember(zone?.id) { mutableStateOf(zone?.radiusMeters?.toString() ?: "100") }
    val excludeHistory = remember(zone?.id) { mutableStateOf(zone?.excludeFromHistory ?: true) }
    val excludeExposure = remember(zone?.id) { mutableStateOf(zone?.excludeFromExposure ?: false) }
    val isActive = remember(zone?.id) { mutableStateOf(zone?.isActive ?: true) }
    val error = remember { mutableStateOf<String?>(null) }
    val invalidFormMessage = stringResource(R.string.places_invalid_form)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (zone == null) R.string.privacy_zone_add else R.string.privacy_zone_edit))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(name.value, { name.value = it }, label = { Text(stringResource(R.string.privacy_zone_name)) }, singleLine = true)
                OutlinedTextField(latitude.value, { latitude.value = it }, label = { Text(stringResource(R.string.place_latitude)) }, singleLine = true)
                OutlinedTextField(longitude.value, { longitude.value = it }, label = { Text(stringResource(R.string.place_longitude)) }, singleLine = true)
                OutlinedTextField(radius.value, { radius.value = it }, label = { Text(stringResource(R.string.place_radius)) }, singleLine = true)
                CheckOption(
                    checked = excludeHistory.value,
                    label = stringResource(R.string.privacy_zone_exclude_history),
                    onCheckedChange = { excludeHistory.value = it }
                )
                CheckOption(
                    checked = excludeExposure.value,
                    label = stringResource(R.string.privacy_zone_exclude_exposure),
                    onCheckedChange = { excludeExposure.value = it }
                )
                CheckOption(
                    checked = isActive.value,
                    label = stringResource(R.string.privacy_zone_active),
                    onCheckedChange = { isActive.value = it }
                )
                error.value?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val lat = latitude.value.toDoubleOrNull()
                    val lon = longitude.value.toDoubleOrNull()
                    val radiusMeters = radius.value.toDoubleOrNull()
                    if (
                        name.value.isBlank() ||
                        lat == null || !lat.isFinite() || lat !in -90.0..90.0 ||
                        lon == null || !lon.isFinite() || lon !in -180.0..180.0 ||
                        radiusMeters == null || !radiusMeters.isFinite() || radiusMeters <= 0.0
                    ) {
                        error.value = invalidFormMessage
                    } else {
                        onSave(
                            (zone ?: PrivacyZone(
                                name = name.value.trim(),
                                centerLatitude = lat,
                                centerLongitude = lon,
                                radiusMeters = radiusMeters
                            )).copy(
                                name = name.value.trim(),
                                centerLatitude = lat,
                                centerLongitude = lon,
                                radiusMeters = radiusMeters,
                                excludeFromHistory = excludeHistory.value,
                                excludeFromExposure = excludeExposure.value,
                                isActive = isActive.value
                            )
                        )
                    }
                }
            ) { Text(stringResource(R.string.place_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.place_cancel)) }
        }
    )
}

@Composable
private fun CheckOption(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.place_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.place_cancel)) }
        }
    )
}

@Composable
private fun EmptyPanel(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun categoryIconFor(category: PlaceCategory): RgIcon.IconData =
    when (category) {
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

private fun categoryColorFor(category: PlaceCategory): androidx.compose.ui.graphics.Color =
    when (category) {
        PlaceCategory.HOME -> RgColor.Primary
        PlaceCategory.WORK -> RgColor.Secondary
        PlaceCategory.SCHOOL -> androidx.compose.ui.graphics.Color(0xFF8FC9A8)
        PlaceCategory.GYM -> androidx.compose.ui.graphics.Color(0xFFE7D18B)
        PlaceCategory.PARK -> RgColor.ExposureGood
        PlaceCategory.SHOP -> androidx.compose.ui.graphics.Color(0xFFE8A77F)
        PlaceCategory.RESTAURANT -> androidx.compose.ui.graphics.Color(0xFFD98787)
        PlaceCategory.TRANSIT -> RgColor.Info
        PlaceCategory.OTHER -> RgColor.OnSurfaceVariant
    }
