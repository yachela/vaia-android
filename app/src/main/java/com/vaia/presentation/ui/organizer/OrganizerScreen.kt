package com.vaia.presentation.ui.organizer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.DragState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.android.libraries.places.api.model.Place
import com.vaia.domain.model.Activity
import com.vaia.domain.model.Trip
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.TopBar
import com.vaia.presentation.viewmodel.MapViewModel
import com.vaia.presentation.viewmodel.TripsViewModel

data class RecommendationCategory(
    val label: String,
    val types: List<String>,
    val color: Color
)

val categories = listOf(
    RecommendationCategory("Gastronomía", listOf("restaurant", "cafe", "bar", "bakery"), Color(0xFFFF5252)),
    RecommendationCategory("Cultura", listOf("tourist_attraction", "museum", "art_gallery", "place_of_worship"), Color(0xFF7C4DFF)),
    RecommendationCategory("Naturaleza", listOf("park", "amusement_park", "zoo", "aquarium"), Color(0xFF4CAF50)),
    RecommendationCategory("Compras", listOf("shopping_mall", "supermarket", "convenience_store"), Color(0xFFFFAB40))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerScreen(
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateOrganizer: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {},
    onNavigateCurrency: () -> Unit = {},
    onNavigateToPackingList: (String, String, Int) -> Unit = { _, _, _ -> },
    tripsViewModel: TripsViewModel,
    mapViewModel: MapViewModel
) {
    val trips by tripsViewModel.trips.collectAsState()
    val activities by mapViewModel.activities.collectAsState()
    val geocodedLocations by mapViewModel.geocodedLocations.collectAsState()
    val accommodationLocation by mapViewModel.accommodationLocation.collectAsState()
    val destinationLocation by mapViewModel.destinationLocation.collectAsState()
    val selectedActivityId by mapViewModel.selectedActivityId.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()

    // New states for recommendations
    val searchPointerLocation by mapViewModel.searchPointerLocation.collectAsState()
    val recommendations by mapViewModel.recommendations.collectAsState()
    val selectedPlace by mapViewModel.selectedPlace.collectAsState()
    var selectedCategory by remember { mutableStateOf<RecommendationCategory?>(null) }

    var selectedTrip by remember { mutableStateOf<Trip?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState()
    val bottomSheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        if (trips.isEmpty()) tripsViewModel.loadTrips()
    }

    LaunchedEffect(trips) {
        if (selectedTrip == null && trips.isNotEmpty()) {
            val today = java.time.LocalDate.now()
            val formatter = java.time.format.DateTimeFormatter.ISO_DATE
            selectedTrip = trips.minByOrNull { trip ->
                try {
                    val startDate = java.time.LocalDate.parse(trip.startDate.take(10), formatter)
                    val days = java.time.temporal.ChronoUnit.DAYS.between(today, startDate)
                    if (days >= 0) days else Long.MAX_VALUE // Prioritize future trips
                } catch (e: Exception) {
                    Long.MAX_VALUE
                }
            } ?: trips.first()
        }
    }

    LaunchedEffect(selectedTrip) {
        selectedTrip?.let { mapViewModel.loadActivities(it.id, it.destination) }
    }

    // Pan camera to accommodation or destination and set initial search pointer
    LaunchedEffect(accommodationLocation, destinationLocation) {
        val target = accommodationLocation ?: destinationLocation
        target?.let {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(it, 14f),
                durationMs = 1000
            )
            if (searchPointerLocation == null) {
                mapViewModel.updateSearchPointer(it)
            }
        }
    }

    val selectedActivity = remember(selectedActivityId, activities) {
        activities.find { it.id == selectedActivityId }
    }

    if (selectedActivity != null) {
        ModalBottomSheet(
            onDismissRequest = { mapViewModel.selectActivity(null) },
            sheetState = bottomSheetState
        ) {
            ActivityInfoSheet(activity = selectedActivity)
        }
    }

    if (selectedPlace != null) {
        val placePhoto by mapViewModel.selectedPlacePhoto.collectAsState()
        ModalBottomSheet(
            onDismissRequest = { mapViewModel.clearSelectedPlace() },
            sheetState = bottomSheetState
        ) {
            PlaceInfoSheet(place = selectedPlace!!, photo = placePhoto)
        }
    }

    Scaffold(
        topBar = {
            TopBar(onNotificationsClick = onNavigateToNotifications, onProfileClick = onNavigateProfile )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .navigationBarsPadding()
            ) {
                AppQuickBar(
                    currentRoute = "organizer",
                    onHome = onNavigateHome,
                    onMap = onNavigateOrganizer, // TODO: Implement explore navigation
                    onTrips = onNavigateTrips,
                    onCalendar = onNavigateCalendar,
                    onCurrency = onNavigateCurrency
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp) // Espacio al final de la barra
                ) {
                    Text(
                        text = "Mapa de actividades",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    if (trips.isNotEmpty()) {
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedTrip?.title ?: "Seleccionar viaje",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                label = { Text("Viaje") }
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                trips.forEach { trip ->
                                    DropdownMenuItem(
                                        text = { Text(trip.title) },
                                        onClick = {
                                            if (selectedTrip?.id != trip.id) {
                                                selectedTrip = trip
                                            }
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        selectedTrip?.let { trip ->
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(categories) { category ->
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = {
                                            if (selectedCategory == category) {
                                                selectedCategory = null
                                                mapViewModel.clearRecommendations()
                                            } else {
                                                selectedCategory = category
                                                mapViewModel.searchNearby(category.types)
                                            }
                                        },
                                        label = { 
                                            Text(
                                                text = category.label,
                                                color = if (selectedCategory == category) Color.White else category.color
                                            ) 
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = category.color,
                                            selectedLabelColor = Color.White,
                                            containerColor = category.color.copy(alpha = 0.1f),
                                            labelColor = category.color
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = category.color.copy(alpha = 0.5f),
                                            selectedBorderColor = category.color
                                        ),
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Category,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = if (selectedCategory == category) Color.White else category.color
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    activities.forEach { activity ->
                        val latLng = geocodedLocations[activity.id]
                        if (latLng != null) {
                            Marker(
                                state = MarkerState(position = latLng),
                                title = activity.title,
                                snippet = activity.location,
                                onClick = {
                                    mapViewModel.selectActivity(activity.id)
                                    true
                                }
                            )
                        }
                    }

                    // Recommendations Markers with Composables
                    recommendations.forEach { place ->
                        val latLng = place.latLng
                        if (latLng != null) {
                            val categoryColor = selectedCategory?.color ?: MaterialTheme.colorScheme.primary
                            MarkerComposable(
                                state = MarkerState(position = latLng),
                                onClick = {
                                    mapViewModel.fetchPlaceDetails(place.id ?: "")
                                    true
                                }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.wrapContentSize()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(categoryColor, CircleShape)
                                            .border(2.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when(selectedCategory?.label) {
                                                "Gastronomía" -> Icons.Default.Restaurant
                                                "Cultura" -> Icons.Default.Museum
                                                "Naturaleza" -> Icons.Default.Park
                                                "Compras" -> Icons.Default.ShoppingBag
                                                else -> Icons.Default.Place
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = place.name ?: "",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Search Pointer
                    searchPointerLocation?.let { pointer ->
                        val pointerState = rememberMarkerState(position = pointer)
                        
                        LaunchedEffect(pointer) {
                            if (pointerState.dragState != DragState.DRAG) {
                                pointerState.position = pointer
                            }
                        }
                        
                        LaunchedEffect(pointerState.dragState) {
                            if (pointerState.dragState == DragState.END) {
                                mapViewModel.updateSearchPointer(pointerState.position)
                            }
                        }
                        
                        Marker(
                            state = pointerState,
                            draggable = true,
                            title = "Puntero de búsqueda (Mover para buscar)",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                        )
                    }
                }

                // Floating Action Button to add search pointer if it doesn't exist or to reset it to center
                FloatingActionButton(
                    onClick = {
                        val center = cameraPositionState.position.target
                        mapViewModel.updateSearchPointer(center)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 120.dp, start = 16.dp),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Ubicar puntero de búsqueda")
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // Precision Messages
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isLoading && selectedTrip != null && accommodationLocation == null) {
                        Card(
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                            ),
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            Text(
                                text = if (destinationLocation != null)
                                    "Centrado en el destino del viaje. Cargá un alojamiento para mayor precisión."
                                else
                                    "Cargá un alojamiento para ubicar más rápido la ubicación de tu viaje.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                if (!isLoading && geocodedLocations.isEmpty() && activities.isNotEmpty() && destinationLocation == null) {
                    Card(modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                    ) {
                        Text(
                            text = "No se pudo geolocalizar ninguna actividad.\nVerifica que las ubicaciones sean válidas.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (!isLoading && activities.isEmpty() && selectedTrip != null) {
                    Card(modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                    ) {
                        Text(
                            text = "Este viaje no tiene actividades aún.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityInfoSheet(activity: Activity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = activity.title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (activity.description.isNotBlank()) {
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccessTime, contentDescription = stringResource(com.vaia.R.string.time), modifier = Modifier.padding(end = 6.dp))
            Text(text = "${activity.date}  ${activity.time}", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = stringResource(com.vaia.R.string.location), modifier = Modifier.padding(end = 6.dp))
            Text(text = activity.location, style = MaterialTheme.typography.bodyMedium)
        }
        if (activity.cost > 0.0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachMoney, contentDescription = stringResource(com.vaia.R.string.cost), modifier = Modifier.padding(end = 6.dp))
                Text(text = "%.2f".format(activity.cost), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PlaceInfoSheet(place: Place, photo: android.graphics.Bitmap?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = place.name ?: "Lugar recomendado",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                place.rating?.let { rating ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star, 
                            contentDescription = null, 
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$rating",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Opening hours status - we check if info is available
            if (place.currentOpeningHours != null) {
                val isOpen = place.currentOpeningHours?.periods?.any { true } ?: false // Simplified check
                Surface(
                    color = if (isOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isOpen) "Abierto" else "Cerrado",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Summary / Description
        place.editorialSummary?.let { summary ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        place.address?.let { address ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.LocationOn, 
                    contentDescription = null, 
                    modifier = Modifier.size(20.dp).padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        place.phoneNumber?.let { phone ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Phone, 
                    contentDescription = null, 
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}
