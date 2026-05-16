package com.vaia.presentation.ui.organizer

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vaia.domain.model.Activity
import com.vaia.domain.model.Trip
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.TopBar
import com.vaia.presentation.viewmodel.MapViewModel
import com.vaia.presentation.viewmodel.TripsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerScreen(
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateOrganizer: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {},
    tripsViewModel: TripsViewModel,
    mapViewModel: MapViewModel
) {
    val trips by tripsViewModel.trips.collectAsState()
    val activities by mapViewModel.activities.collectAsState()
    val geocodedLocations by mapViewModel.geocodedLocations.collectAsState()
    val selectedActivityId by mapViewModel.selectedActivityId.collectAsState()
    val isLoading by mapViewModel.isLoading.collectAsState()

    var selectedTrip by remember { mutableStateOf<Trip?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState()
    val bottomSheetState = rememberModalBottomSheetState()

    LaunchedEffect(Unit) {
        if (trips.isEmpty()) tripsViewModel.loadTrips()
    }

    LaunchedEffect(trips) {
        if (selectedTrip == null && trips.isNotEmpty()) {
            selectedTrip = trips.first()
        }
    }

    LaunchedEffect(selectedTrip) {
        selectedTrip?.let { mapViewModel.loadActivities(it.id) }
    }

    // Pan camera to first resolved location
    LaunchedEffect(geocodedLocations) {
        if (geocodedLocations.isNotEmpty()) {
            val firstLatLng = geocodedLocations.values.first()
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(firstLatLng, 13f),
                durationMs = 800
            )
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
                    onCurrency = {}
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
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                if (!isLoading && geocodedLocations.isEmpty() && activities.isNotEmpty()) {
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
            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
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
