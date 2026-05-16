package com.vaia.presentation.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vaia.R
import com.vaia.domain.model.Trip
import com.vaia.domain.model.destinationList
import com.vaia.domain.model.primaryDestination
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.TripCardSkeleton
import com.vaia.presentation.ui.common.AppExceptionDialog
import com.vaia.presentation.ui.common.AppExceptionMapper
import com.vaia.presentation.ui.common.AppExceptionUi
import com.vaia.presentation.ui.common.VaiaDatePickerField
import com.vaia.presentation.ui.common.formatDateForDisplay
import com.vaia.presentation.ui.common.moveFocusOnEnterOrTab
import com.vaia.presentation.ui.common.normalizeDateForApi
import com.vaia.presentation.ui.common.TopBar
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.vaia.presentation.ui.theme.InkBlack
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.ui.theme.SunAccent
import com.vaia.presentation.ui.theme.ErrorRed
import com.vaia.presentation.ui.theme.SalmonOrange
import com.vaia.presentation.viewmodel.TripsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    onNavigateToActivities: (String) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateCalendar: () -> Unit = {},
    onNavigateOrganizer: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    viewModel: TripsViewModel
) {
    val haptic = LocalHapticFeedback.current
    val trips by viewModel.trips.collectAsState()
    val filteredTrips by viewModel.filteredTrips.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMorePages by viewModel.hasMorePages.collectAsState()
    val error by viewModel.error.collectAsState()
    val createTripState by viewModel.createTripState.collectAsState()
    val updateTripState by viewModel.updateTripState.collectAsState()
    val deleteTripState by viewModel.deleteTripState.collectAsState()
    val listState = rememberLazyListState()

    var showSearch by remember { mutableStateOf(false) }
    var showCreateTripDialog by remember { mutableStateOf(false) }
    var tripToEdit by remember { mutableStateOf<Trip?>(null) }
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }
    var appException by remember { mutableStateOf<AppExceptionUi?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val tripCreatedSuccess = stringResource(R.string.trip_created_success)
    val tripUpdatedSuccess = stringResource(R.string.trip_updated_success)
    val tripDeletedSuccess = stringResource(R.string.trip_deleted_success)

    LaunchedEffect(Unit) { viewModel.loadTrips() }

    LaunchedEffect(listState, hasMorePages) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = layoutInfo.totalItemsCount
            lastVisible >= total - 3 && total > 0
        }.collect { nearEnd ->
            if (nearEnd && hasMorePages && !isLoadingMore) {
                viewModel.loadMoreTrips()
            }
        }
    }

    LaunchedEffect(createTripState) {
        when (createTripState) {
            is TripsViewModel.CreateTripState.Success -> {
                showCreateTripDialog = false
                snackbarHostState.showSnackbar(tripCreatedSuccess)
                viewModel.resetCreateTripState()
            }
            is TripsViewModel.CreateTripState.Error -> {
                val message = (createTripState as TripsViewModel.CreateTripState.Error).message
                appException = AppExceptionMapper.fromMessage(message)
            }
            else -> Unit
        }
    }

    LaunchedEffect(updateTripState) {
        when (updateTripState) {
            is TripsViewModel.UpdateTripState.Success -> {
                tripToEdit = null
                snackbarHostState.showSnackbar(tripUpdatedSuccess)
                viewModel.resetUpdateTripState()
            }
            is TripsViewModel.UpdateTripState.Error -> {
                val message = (updateTripState as TripsViewModel.UpdateTripState.Error).message
                appException = AppExceptionMapper.fromMessage(message)
            }
            else -> Unit
        }
    }

    LaunchedEffect(deleteTripState) {
        when (deleteTripState) {
            is TripsViewModel.DeleteTripState.Success -> {
                tripToDelete = null
                snackbarHostState.showSnackbar(tripDeletedSuccess)
                viewModel.resetDeleteTripState()
            }
            is TripsViewModel.DeleteTripState.Error -> {
                val message = (deleteTripState as TripsViewModel.DeleteTripState.Error).message
                appException = AppExceptionMapper.fromMessage(message)
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopBar(onNotificationsClick = onNavigateToNotifications, onProfileClick = onNavigateProfile )
        },
        floatingActionButton = {
            if (trips.isNotEmpty() && !isLoading) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateTripDialog = true },
                    icon = { Icon(Icons.Default.Add, stringResource(R.string.add_trip)) },
                    text = { Text(stringResource(R.string.new_trip)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = MaterialTheme.shapes.large
                )
            }
        },
        bottomBar = {
            // Quitamos el Surface y usamos un Box simple con transparencia total
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .navigationBarsPadding() // Esto asegura que no choque con la barra de gestos de Android
            ) {
                AppQuickBar(
                    currentRoute = "trips",
                    onHome = onNavigateHome,
                    onMap = onNavigateOrganizer,
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
                .background( MaterialTheme.colorScheme.background)
        ) {
            Crossfade(targetState = Triple(isLoading, error, trips.isEmpty()), animationSpec = tween(280), label = "trips-state") { state ->
                val currentLoading = state.first
                val currentError = state.second
                val currentEmpty = state.third
                when {
                    currentLoading -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(3) { TripCardSkeleton() }
                        }
                    }
    
                    currentError != null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Card(
                                shape = MaterialTheme.shapes.extraLarge,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(stringResource(R.string.error_something_went_wrong), style = MaterialTheme.typography.headlineSmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(currentError, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = { viewModel.loadTrips() }) { Text(stringResource(R.string.retry)) }
                                }
                            }
                        }
                    }
    
                    currentEmpty -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(SalmonOrange.copy(alpha = 0.2f), SalmonOrange.copy(alpha = 0.05f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Flight,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = SalmonOrange
                                )
                            }
    
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = stringResource(R.string.no_trips_yet),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.create_first_trip_message),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            ExtendedFloatingActionButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showCreateTripDialog = true
                                },
                                icon = { Icon(Icons.Default.Add, stringResource(R.string.add_trip)) },
                                text = { Text(stringResource(R.string.create_first_trip)) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                shape = MaterialTheme.shapes.large
                            )
                        }
                    }

                    else -> {
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { visible = true }

                        val today = remember { LocalDate.now().toString() }
                        val nextTrip = remember(trips, today) {
                            trips
                                .filter { (normalizeDateForApi(it.startDate) ?: "") > today }
                                .minByOrNull { normalizeDateForApi(it.startDate) ?: it.startDate }
                        }
                        val daysUntilNext = remember(nextTrip) {
                            nextTrip?.let {
                                try {
                                    val start = LocalDate.parse(normalizeDateForApi(it.startDate) ?: "")
                                    ChronoUnit.DAYS.between(LocalDate.now(), start).coerceAtLeast(0)
                                } catch (_: Exception) { null }
                            }
                        }
                        val uniqueDestinations = remember(trips) {
                            trips.flatMap { it.destinationList() }
                                .map { it.trim().lowercase() }
                                .distinct().size
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = paddingValues.calculateTopPadding() + 8.dp,
                                bottom = 100.dp,
                                start = 24.dp,
                                end = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // item 0 — Título + buscador
                            item {
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(500, delayMillis = 0)) +
                                            slideInVertically(tween(500, delayMillis = 0)) { it / 4 }
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.my_trips_title),
                                                style = MaterialTheme.typography.headlineLarge.copy(
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                            )
                                            IconButton(onClick = { showSearch = !showSearch }) {
                                                Icon(
                                                    Icons.Default.Search,
                                                    contentDescription = stringResource(R.string.search),
                                                    tint = MaterialTheme.colorScheme.onBackground
                                                )
                                            }
                                        }
                                        AnimatedVisibility(visible = showSearch) {
                                            OutlinedTextField(
                                                value = searchQuery,
                                                onValueChange = { viewModel.setSearchQuery(it) },
                                                placeholder = { Text(stringResource(R.string.search_trips_hint)) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // item 1 — Stats + card próximo viaje
                            item {
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(500, delayMillis = 120)) +
                                            slideInVertically(tween(500, delayMillis = 120)) { it / 4 }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(180.dp),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            StatItem(
                                                count = trips.size.toString(),
                                                label = stringResource(R.string.planned_trips),
                                                icon = Icons.Default.BusinessCenter,
                                                iconColor = MaterialTheme.colorScheme.secondary
                                            )

                                            StatItem(
                                                count = uniqueDestinations.toString(),
                                                label = stringResource(R.string.unique_destinations),
                                                icon = Icons.Default.Place,
                                                iconColor = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        Card(
                                            modifier = Modifier.weight(1.4f).fillMaxHeight(),
                                            shape = MaterialTheme.shapes.large,
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.primary
                                            )
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.FlightTakeoff,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(24.dp).align(Alignment.TopEnd)
                                                )
                                                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                                        shape = MaterialTheme.shapes.extraSmall // 8.dp de tus VaiaShapes
                                                    ) {
                                                        Text(
                                                            text = stringResource(R.string.trip_upcoming_label).uppercase(),
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                           color = MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text(
                                                        text = nextTrip?.destinationList()?.firstOrNull() ?: stringResource(R.string.no_upcoming_trips),
                                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onPrimary // Contraste perfecto asegurado
                                                    )

                                                    val lastDestination = nextTrip?.destinationList()?.lastOrNull()
                                                    if (!lastDestination.isNullOrEmpty()) {
                                                        Text(
                                                            text = "→ $lastDestination",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    if (daysUntilNext != null) {
                                                        Text(
                                                            // Usamos tu stringResource con formato dinámico que tienes en strings.xml
                                                            text = stringResource(R.string.days_away, daysUntilNext),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // item 2 — Detalles del viaje
                            item {
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(500, delayMillis = 240)) +
                                            slideInVertically(tween(500, delayMillis = 240)) { it / 4 }
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = stringResource(R.string.your_trips_summary).uppercase(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        nextTrip?.let { upcoming ->
                                            DetailRow(
                                                title = "Ruta",
                                                subtitle = upcoming.destinationList().joinToString(" → ")
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            DetailRow(
                                                title = "Fecha",
                                                subtitle = "${displayDate(upcoming.startDate)} - ${displayDate(upcoming.endDate)}"
                                            )
                                        }
                                    }
                                }
                            }

                            // items 3+ — Cada TripCard en cascada fluida
                            itemsIndexed(filteredTrips, key = { _, trip -> trip.id }) { index, trip ->
                                val delay = 360 + index * 120
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn(tween(500, delayMillis = delay)) +
                                            slideInVertically(tween(500, delayMillis = delay)) { it / 4 }
                                ) {
                                    TripCard(
                                        trip = trip,
                                        onClick = { onNavigateToActivities(trip.id) },
                                        onEdit = { tripToEdit = trip },
                                        onDelete = { tripToDelete = trip }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateTripDialog) {
            CreateTripDialog(
                isLoading = createTripState is TripsViewModel.CreateTripState.Loading,
                errorMessage = (createTripState as? TripsViewModel.CreateTripState.Error)?.message,
                onDismiss = {
                    showCreateTripDialog = false
                    viewModel.resetCreateTripState()
                },
                onCreate = { title, destination, startDate, endDate, budget, tripType ->
                    viewModel.createTrip(title, destination, startDate, endDate, budget, tripType)
                }
            )
        }

        tripToEdit?.let { selectedTrip ->
            EditTripDialog(
                trip = selectedTrip,
                isLoading = updateTripState is TripsViewModel.UpdateTripState.Loading,
                errorMessage = (updateTripState as? TripsViewModel.UpdateTripState.Error)?.message,
                onDismiss = {
                    tripToEdit = null
                    viewModel.resetUpdateTripState()
                },
                onSave = { title, destination, startDate, endDate, budget ->
                    viewModel.updateTrip(selectedTrip.id, title, destination, startDate, endDate, budget)
                }
            )
        }

        tripToDelete?.let { selectedTrip ->
            AlertDialog(
                onDismissRequest = {
                    if (deleteTripState !is TripsViewModel.DeleteTripState.Loading) {
                        tripToDelete = null
                        viewModel.resetDeleteTripState()
                    }
                },
                title = { Text(stringResource(R.string.delete_trip_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.delete_trip_confirmation, selectedTrip.title))
                        val message = (deleteTripState as? TripsViewModel.DeleteTripState.Error)?.message
                        if (!message.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.deleteTrip(selectedTrip.id) },
                        enabled = deleteTripState !is TripsViewModel.DeleteTripState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        if (deleteTripState is TripsViewModel.DeleteTripState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.delete))
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            tripToDelete = null
                            viewModel.resetDeleteTripState()
                        },
                        enabled = deleteTripState !is TripsViewModel.DeleteTripState.Loading
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }

        appException?.let { exception ->
            AppExceptionDialog(
                exception = exception,
                onDismiss = { appException = null }
            )
        }
    }
}

@Composable
fun StatItem(count: String, label: String, icon: ImageVector, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(
                text = count,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DetailRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripCard(
    trip: Trip,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Column {
            // 1. HEADER CON IMAGEN Y EFECTOS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AsyncImage(
                    model = tripCoverImageUrl(trip.primaryDestination()),
                    contentDescription = "Foto de portada de ${trip.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradiente de profundidad
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(0.5f), Color.Transparent, Color.Black.copy(0.6f)))
                ))

                // RANGO DE FECHAS (Glassmorphism)
                Surface(
                    modifier = Modifier.padding(16.dp).align(Alignment.BottomStart),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape,
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.DateRange, null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${displayDate(trip.startDate)} - ${displayDate(trip.endDate)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }

                // ACCIONES (Fix del color del icono)
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionCircleButton(icon = Icons.Default.Edit, onClick = onEdit, tint = Color.Black)
                    ActionCircleButton(icon = Icons.Default.Delete, onClick = onDelete, tint = Color.Red)
                }
            }

            // 2. CONTENIDO TÉCNICO
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(14.dp), tint = MintPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = trip.destinationList().joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // STATS (Diseño más limpio sin separadores pesados)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem(trip.activitiesCount.toString(), "Actividades")
                    StatItem(trip.expensesCount.toString(), "Gastos")
                    StatItem("$${trip.totalExpenses}", "Total Consumido", isPrimary = true)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // BARRA DE PRESUPUESTO ESTILIZADA
                val spentRatio = if (trip.budget > 0) (trip.totalExpenses / trip.budget).toFloat().coerceIn(0f, 1f) else 0f
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Progreso del Presupuesto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${trip.budget.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = spentRatio,
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                        color = if (spentRatio > 0.9f) Color(0xFFE57373) else MintPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ActionCircleButton(icon: ImageVector, onClick: () -> Unit, tint: Color) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.9f),
        modifier = Modifier.size(38.dp),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = tint)
        }
    }
}

@Composable
fun StatItem(value: String, label: String, isPrimary: Boolean = false) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = if (isPrimary) MintPrimary else MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun MiniRowItem(
    title: String,
    subtitle: String,
    trailing: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(trailing, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun displayDate(rawValue: String): String = formatDateForDisplay(rawValue)

private fun tripCoverImageUrl(destination: String): String {
    val key = destination.lowercase()
    return when {
        "paris" in key -> "https://images.unsplash.com/photo-1431274172761-fca41d930114?w=1200&q=80&auto=format&fit=crop"
        "london" in key -> "https://images.unsplash.com/photo-1513635269975-59663e0ac1ad?w=1200&q=80&auto=format&fit=crop"
        "rome" in key -> "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=1200&q=80&auto=format&fit=crop"
        "madrid" in key -> "https://images.unsplash.com/photo-1539037116277-4db20889f2d4?w=1200&q=80&auto=format&fit=crop"
        "barcelona" in key -> "https://images.unsplash.com/photo-1583422409516-2895a77efded?w=1200&q=80&auto=format&fit=crop"
        "new york" in key -> "https://images.unsplash.com/photo-1499092346589-b9b6be3e94b2?w=1200&q=80&auto=format&fit=crop"
        else -> "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=1200&q=80&auto=format&fit=crop"
    }
}

private fun tripDestinationName(trip: Trip?): String = trip?.destinationList()?.joinToString(" → ") ?: "—"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTripDialog(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        budget: Double,
        tripType: String?
    ) -> Unit
) {
    TripFormDialog(
        titleText = stringResource(R.string.new_trip),
        isLoading = isLoading,
        errorMessage = errorMessage,
        initialTitle = "",
        initialDestination = "",
        initialStartDate = "",
        initialEndDate = "",
        initialBudget = "",
        initialTripType = "aventura",
        showTripTypeSelector = true,
        confirmText = stringResource(R.string.create_button),
        onDismiss = onDismiss,
        onConfirm = onCreate
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTripDialog(
    trip: Trip,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (title: String, destination: String, startDate: String, endDate: String, budget: Double) -> Unit
) {
    TripFormDialog(
        titleText = stringResource(R.string.edit_trip_title),
        isLoading = isLoading,
        errorMessage = errorMessage,
        initialTitle = trip.title,
        initialDestination = trip.destination,
        initialStartDate = displayDate(trip.startDate),
        initialEndDate = displayDate(trip.endDate),
        initialBudget = trip.budget.toString(),
        initialTripType = null,
        showTripTypeSelector = false,
        confirmText = stringResource(R.string.save),
        onDismiss = onDismiss,
        onConfirm = { title, destination, startDate, endDate, budget, _ ->
            onSave(title, destination, startDate, endDate, budget)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripFormDialog(
    titleText: String,
    isLoading: Boolean,
    errorMessage: String?,
    initialTitle: String,
    initialDestination: String,
    initialStartDate: String,
    initialEndDate: String,
    initialBudget: String,
    initialTripType: String?,
    showTripTypeSelector: Boolean,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        budget: Double,
        tripType: String?
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var destinations by remember {
        mutableStateOf(
            if (initialDestination.isBlank()) listOf("")
            else initialDestination.split(",").map { it.trim() }
        )
    }
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var budget by remember { mutableStateOf(initialBudget) }
    var tripType by remember { mutableStateOf(initialTripType) }
    var localError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val titleRequiredMessage = stringResource(R.string.title_required_error)
    val destinationRequiredMessage = stringResource(R.string.destination_required_error)
    val startDateRequiredMessage = stringResource(R.string.start_date_required_error)
    val endDateRequiredMessage = stringResource(R.string.end_date_required_error)
    val budgetNumericMessage = stringResource(R.string.budget_numeric_error)
    val budgetNegativeMessage = stringResource(R.string.budget_negative_error)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState = sheetState,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Handle visual + título
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(titleText, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.trip_title_label)) },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnEnterOrTab(focusManager)
                )
                Text(
                    text = stringResource(R.string.destinations_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                destinations.forEachIndexed { index, dest ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = dest,
                            onValueChange = { newValue ->
                                destinations = destinations.toMutableList().also { it[index] = newValue }
                            },
                            label = {
                                Text(
                                    if (index == 0) stringResource(R.string.destination_stop_origin)
                                    else stringResource(R.string.destination_stop_label, index + 1)
                                )
                            },
                            singleLine = true,
                            enabled = !isLoading,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .moveFocusOnEnterOrTab(focusManager)
                        )
                        if (index > 0) {
                            IconButton(
                                onClick = {
                                    destinations = destinations.toMutableList().also { it.removeAt(index) }
                                },
                                enabled = !isLoading
                            ) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_destination))
                            }
                        }
                    }
                }
                if (destinations.size < 6) {
                    TextButton(
                        onClick = { destinations = destinations + "" },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.add_destination_stop))
                    }
                }
                if (showTripTypeSelector) {
                    Text(
                        text = stringResource(R.string.trip_type_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tripTypeOptions = listOf(
                            "aventura" to stringResource(R.string.trip_type_adventure),
                            "familiar" to stringResource(R.string.trip_type_family),
                            "solitario" to stringResource(R.string.trip_type_solo),
                            "amigos" to stringResource(R.string.trip_type_friends)
                        )
                        for ((key, label) in tripTypeOptions) {
                            FilterChip(
                                selected = tripType == key,
                                onClick = { tripType = key },
                                label = { Text(label) },
                                enabled = !isLoading
                            )
                        }
                    }
                }
                VaiaDatePickerField(
                    value = startDate,
                    label = stringResource(R.string.start_date),
                    enabled = !isLoading,
                    onDateSelected = { startDate = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .moveFocusOnEnterOrTab(focusManager, isDoneField = true)
                )
                VaiaDatePickerField(
                    value = endDate,
                    label = stringResource(R.string.end_date),
                    enabled = !isLoading,
                    onDateSelected = { endDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budget,
                    onValueChange = { budget = it },
                    label = { Text(stringResource(R.string.budget)) },
                    singleLine = true,
                    enabled = !isLoading,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )

            val message = localError ?: errorMessage
            if (!message.isNullOrBlank()) {
                Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.cancel)) }
                Button(
                    onClick = {
                        val parsedBudget = budget.replace(",", ".").toDoubleOrNull()
                        val destinationString = destinations.map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")
                        localError = when {
                            title.isBlank() -> titleRequiredMessage
                            destinationString.isBlank() -> destinationRequiredMessage
                            startDate.isBlank() -> startDateRequiredMessage
                            endDate.isBlank() -> endDateRequiredMessage
                            parsedBudget == null -> budgetNumericMessage
                            parsedBudget < 0.0 -> budgetNegativeMessage
                            else -> null
                        }
                        if (localError == null && parsedBudget != null) {
                            onConfirm(title.trim(), destinationString, startDate, endDate, parsedBudget, tripType)
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(2f)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text(confirmText)
                }
            }
        }
    }
}
