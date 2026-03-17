package com.vaia.presentation.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaia.domain.model.Trip
import com.vaia.presentation.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToTripDetails: (String) -> Unit,
    onNavigateToAllTrips: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadTrips()
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                userName = "Usuario",
                onNotificationsClick = onNavigateToNotifications
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is HomeUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Sección "Mis Viajes"
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mis Viajes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = onNavigateToAllTrips) {
                                Text("Ver todos")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Cards de viajes (máximo 3)
                    items(state.trips.take(3)) { trip ->
                        TripCard(
                            trip = trip,
                            tripStatus = viewModel.calculateTripStatus(trip),
                            onClick = { onNavigateToTripDetails(trip.id) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    userName: String,
    onNotificationsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Bienvenido de vuelta, $userName",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        actions = {
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notificaciones"
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripCard(
    trip: Trip,
    tripStatus: TripStatus,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Imagen de portada placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.TopEnd
            ) {
                // Badge de estado
                Surface(
                    modifier = Modifier.padding(12.dp),
                    color = when (tripStatus) {
                        TripStatus.UPCOMING -> MaterialTheme.colorScheme.primary
                        TripStatus.IN_PROGRESS -> Color(0xFFFFA726)
                        TripStatus.COMPLETED -> Color(0xFF9E9E9E)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (tripStatus) {
                            TripStatus.UPCOMING -> "PRÓXIMO"
                            TripStatus.IN_PROGRESS -> "EN CURSO"
                            TripStatus.COMPLETED -> "COMPLETADO"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Contenido de la card
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Nombre del destino
                Text(
                    text = trip.destination,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // Rating con estrella
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "4.5",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Rango de fechas con ícono calendario
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDateRange(trip.startDate, trip.endDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón "Detalles"
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Detalles")
                }
            }
        }
    }
}

private fun formatDateRange(startDate: String, endDate: String): String {
    return try {
        val formatter = DateTimeFormatter.ISO_DATE
        val start = LocalDate.parse(startDate, formatter)
        val end = LocalDate.parse(endDate, formatter)
        val displayFormatter = DateTimeFormatter.ofPattern("dd MMM")
        "${start.format(displayFormatter)} - ${end.format(displayFormatter)}"
    } catch (e: Exception) {
        "$startDate - $endDate"
    }
}

enum class TripStatus {
    UPCOMING,
    IN_PROGRESS,
    COMPLETED
}

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val trips: List<Trip>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
