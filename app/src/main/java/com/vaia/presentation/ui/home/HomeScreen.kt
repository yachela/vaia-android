package com.vaia.presentation.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaia.domain.model.Trip
import com.vaia.domain.model.destinationList
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.TopBar
import com.vaia.presentation.ui.common.TripCardSkeleton
import com.vaia.presentation.ui.theme.BluePrimary
import com.vaia.presentation.ui.theme.BlueLight
import com.vaia.presentation.viewmodel.HomeViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Destinos sugeridos para el feed vacío
private val SUGGESTED_DESTINATIONS = listOf(
    SuggestedDestination("París", "Francia", "Ciudad de la luz y el romance"),
    SuggestedDestination("Tokio", "Japón", "Tradición y modernidad"),
    SuggestedDestination("Nueva York", "EE.UU.", "La ciudad que nunca duerme"),
    SuggestedDestination("Roma", "Italia", "Historia en cada esquina"),
    SuggestedDestination("Bali", "Indonesia", "Paraíso tropical"),
    SuggestedDestination("Barcelona", "España", "Arte y arquitectura única"),
)

data class SuggestedDestination(
    val city: String,
    val country: String,
    val tagline: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToTripDetails: (String) -> Unit,
    onNavigateToAllTrips: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onNavigateTrips: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onNavigateExplore: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {},
    onNavigateOrganizer: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.loadTrips() }

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
                    currentRoute = "home",
                    onHome = onNavigateHome,
                    onMap = onNavigateOrganizer, // TODO: Implement navigation
                    onTrips = onNavigateTrips,
                    onCalendar = onNavigateCalendar,
                    onCurrency = {}
                )
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding() + 16.dp,
                            bottom = 100.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(Modifier.height(8.dp)) }
                        items(3) { TripCardSkeleton() }
                    }
                }

                is HomeUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = { viewModel.loadTrips() }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }

                is HomeUiState.Success -> {
                    if (state.trips.isEmpty()) {
                        EmptyFeed(
                            modifier = Modifier.fillMaxSize(),
                            paddingValues = paddingValues,
                            onCreateTrip = onNavigateToAllTrips
                        )
                    } else {
                        TripsFeed(
                            trips = state.trips,
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                            paddingValues = paddingValues,
                            onTripClick = onNavigateToTripDetails,
                            onSeeAll = onNavigateToAllTrips
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFeed(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    onCreateTrip: () -> Unit
){
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding() + 16.dp,
            bottom = 100.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Hero
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 3 }
            ) {
                HeroCard(onCreateTrip = onCreateTrip)
            }
        }

        // Sección "Inspiración"
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Destinos populares",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700))
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(SUGGESTED_DESTINATIONS) { dest ->
                        DestinationChip(destination = dest, onClick = onCreateTrip)
                    }
                }
            }
        }

        // Tips
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800)) + slideInVertically(tween(800)) { it / 3 }
            ) {
                TipsCard()
            }
        }
    }
}

@Composable
private fun HeroCard(onCreateTrip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(BluePrimary, BlueLight)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "¿A dónde viajamos?",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Planea tu próxima aventura con IA",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Button(
                onClick = onCreateTrip,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = BluePrimary
                ),
                shape = RoundedCornerShape(999.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Crear viaje", fontWeight = FontWeight.SemiBold)
            }
        }
        // Decoración
        Icon(
            imageVector = Icons.Default.Public,
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .size(72.dp),
            tint = Color.White.copy(alpha = 0.25f)
        )
    }
}

@Composable
private fun DestinationChip(
    destination: SuggestedDestination,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                destination.city,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                destination.country,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                destination.tagline,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TipsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "¿Sabías que?",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "VAIA genera tu itinerario automáticamente con IA, incluyendo actividades, lista de equipaje y presupuesto estimado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TripsFeed(
    trips: List<Trip>,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
    onTripClick: (String) -> Unit,
    onSeeAll: () -> Unit
){
    val today = remember { LocalDate.now() }
    val upcoming = remember(trips) {
        trips.filter {
            try { LocalDate.parse(it.startDate.take(10)).isAfter(today) } catch (_: Exception) { false }
        }.minByOrNull { it.startDate }
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Calcula el offset de índice para la cascada según si hay "próximo viaje"
    val baseOffset = if (upcoming != null) 2 else 0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = paddingValues.calculateTopPadding() + 16.dp,
            bottom = 100.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // Próximo viaje highlight
        if (upcoming != null) {
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(350, delayMillis = 0)) +
                            slideInVertically(tween(350, delayMillis = 0)) { it / 4 }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Próximo viaje",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400, delayMillis = 80)) +
                            slideInVertically(tween(400, delayMillis = 80)) { it / 4 }
                ) {
                    UpcomingTripCard(
                        trip = upcoming,
                        status = viewModel.calculateTripStatus(upcoming),
                        onClick = { onTripClick(upcoming.id) }
                    )
                }
            }
        }

        // Todos mis viajes
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(350, delayMillis = baseOffset * 80)) +
                        slideInVertically(tween(350, delayMillis = baseOffset * 80)) { it / 4 }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mis viajes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = onSeeAll) {
                        Text("Ver todos")
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        itemsIndexed(trips.take(4)) { index, trip ->
            val delay = (baseOffset + 1 + index) * 80
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400, delayMillis = delay)) +
                        slideInVertically(tween(400, delayMillis = delay)) { it / 4 }
            ) {
                HomeTripCard(
                    trip = trip,
                    status = viewModel.calculateTripStatus(trip),
                    onClick = { onTripClick(trip.id) }
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun UpcomingTripCard(
    trip: Trip,
    status: TripStatus,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(BluePrimary, BlueLight.copy(alpha = 0.8f))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        StatusBadge(status = status)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = trip.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                            Text(
                                text = trip.destinationList().joinToString(" → "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Outlined.DateRange, contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                    Text(
                        text = formatDateRange(trip.startDate, trip.endDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTripCard(
    trip: Trip,
    status: TripStatus,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Flight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = trip.destinationList().joinToString(" → "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatDateRange(trip.startDate, trip.endDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusBadge(status = status)
        }
    }
}

@Composable
private fun StatusBadge(status: TripStatus) {
    val (label, containerColor, textColor) = when (status) {
        TripStatus.UPCOMING    -> Triple("PRÓXIMO", MaterialTheme.colorScheme.primary, Color.White)
        TripStatus.IN_PROGRESS -> Triple("EN CURSO", MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary)
        TripStatus.COMPLETED   -> Triple("COMPLETADO", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )
    }
}

private fun formatDateRange(startDate: String, endDate: String): String {
    return try {
        val formatter = DateTimeFormatter.ISO_DATE
        val start = LocalDate.parse(startDate.take(10), formatter)
        val end = LocalDate.parse(endDate.take(10), formatter)
        val display = DateTimeFormatter.ofPattern("dd MMM")
        "${start.format(display)} – ${end.format(display)}"
    } catch (_: Exception) {
        "$startDate – $endDate"
    }
}

enum class TripStatus { UPCOMING, IN_PROGRESS, COMPLETED }

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val trips: List<Trip>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
