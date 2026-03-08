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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vaia.R
import com.vaia.domain.model.Trip
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.AppExceptionDialog
import com.vaia.presentation.ui.common.AppExceptionMapper
import com.vaia.presentation.ui.common.AppExceptionUi
import com.vaia.presentation.ui.common.VaiaDatePickerField
import com.vaia.presentation.ui.common.formatDateForDisplay
import com.vaia.presentation.ui.common.moveFocusOnEnterOrTab
import com.vaia.presentation.ui.common.normalizeDateForApi
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.vaia.presentation.ui.theme.InkBlack
import com.vaia.presentation.ui.theme.MintPrimary
import com.vaia.presentation.ui.theme.SkyBackground
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
    onNavigateCalendar: () -> Unit,
    onNavigateOrganizer: () -> Unit,
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
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.my_trips_title),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateTripDialog = true },
                icon = { Icon(Icons.Default.Add, stringResource(R.string.add_trip)) },
                text = { Text(stringResource(R.string.new_trip)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = InkBlack,
                shape = MaterialTheme.shapes.large
            )
        },
        bottomBar = {
            AppQuickBar(
                currentRoute = "trips",
                onHome = onNavigateHome,
                onTrips = onNavigateTrips,
                onProfile = onNavigateProfile,
                onCalendar = onNavigateCalendar,
                onMap = onNavigateOrganizer
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SkyBackground,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Crossfade(targetState = Triple(isLoading, error, trips.isEmpty()), animationSpec = tween(280), label = "trips-state") { state ->
                val currentLoading = state.first
                val currentError = state.second
                val currentEmpty = state.third
                when {
                currentLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.loading_trips), style = MaterialTheme.typography.bodyLarge)
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
                            Text("✈️", style = MaterialTheme.typography.displayMedium)
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
                            contentColor = InkBlack,
                            shape = MaterialTheme.shapes.large
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(stringResource(R.string.trip_brand), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(
                                            onClick = { showSearch = !showSearch },
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(
                                                    if (showSearch) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                        ) { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) }
                                        Box {
                                            IconButton(
                                                onClick = {},
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.surface)
                                            ) { Icon(Icons.Default.NotificationsNone, contentDescription = stringResource(R.string.notifications)) }
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(SalmonOrange)
                                                    .align(Alignment.TopEnd)
                                            )
                                        }
                                    }
                                }
                                AnimatedVisibility(visible = showSearch) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.setSearchQuery(it) },
                                        placeholder = { Text(stringResource(R.string.search_trips_hint)) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        trailingIcon = {
                                            if (searchQuery.isNotBlank()) {
                                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                                    Icon(Icons.Default.Search, contentDescription = null)
                                                }
                                            }
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.your_trips_summary),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
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
                                    trips.map { it.destination.trim().lowercase() }.distinct().size
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = MintPrimary.copy(alpha = 0.22f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                trips.size.toString(),
                                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(stringResource(R.string.planned_trips), style = MaterialTheme.typography.bodySmall)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                uniqueDestinations.toString(),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(stringResource(R.string.unique_destinations), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(118.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(
                                                            MintPrimary.copy(alpha = 0.32f),
                                                            MaterialTheme.colorScheme.surfaceVariant
                                                        )
                                                    )
                                                )
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    stringResource(R.string.next_trip_label),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    nextTrip?.destination ?: stringResource(R.string.no_upcoming_trips),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                                )
                                                if (daysUntilNext != null) {
                                                    Text(
                                                        stringResource(R.string.days_away, daysUntilNext),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.tertiary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                AnimatedVisibility(visible = nextTrip != null) {
                                    nextTrip?.let { upcoming ->
                                        Column {
                                            MiniRowItem(stringResource(R.string.route), upcoming.destination, "→")
                                            Spacer(modifier = Modifier.height(8.dp))
                                            MiniRowItem(stringResource(R.string.travel_month), "${displayDate(upcoming.startDate)} - ${displayDate(upcoming.endDate)}", "→")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        items(filteredTrips, key = { it.id }) { trip ->
                            TripCard(
                                trip = trip,
                                onClick = { onNavigateToActivities(trip.id) },
                                onEdit = { tripToEdit = trip },
                                onDelete = { tripToDelete = trip }
                            )
                        }

                        item {
                            if (isLoadingMore) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(80.dp))
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
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "trip-card-scale"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .animateContentSize()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = tripCoverImageUrl(trip.destination),
                    contentDescription = trip.destination,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    InkBlack.copy(alpha = 0.2f)
                                )
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.86f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = trip.destination,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(R.string.trip_cover_label),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(trip.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(trip.destination, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_trip_title))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_trip_title), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text("${displayDate(trip.startDate)} - ${displayDate(trip.endDate)}") },
                    leadingIcon = { Icon(Icons.Outlined.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("$${trip.budget}") },
                    colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(containerColor = SunAccent.copy(alpha = 0.25f))
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(trip.activitiesCount.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MintPrimary))
                    Text(stringResource(R.string.activities), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(trip.expensesCount.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MintPrimary))
                    Text(stringResource(R.string.expenses), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        "$${trip.totalExpenses}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (trip.totalExpenses > trip.budget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    )
                    Text(stringResource(R.string.total_label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Barra de presupuesto
            val spentRatio = if (trip.budget > 0) {
                (trip.totalExpenses / trip.budget).toFloat().coerceIn(0f, 1f)
            } else 0f
            val budgetBarColor = when {
                spentRatio >= 1f -> ErrorRed
                spentRatio >= 0.7f -> SunAccent
                else -> MintPrimary
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.budget_spent_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$${trip.totalExpenses.toInt()} / $${trip.budget.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = budgetBarColor
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = spentRatio,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = budgetBarColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SunAccent, contentColor = Color.Black)
            ) {
                Text(stringResource(R.string.view_trip), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
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

private fun tripDestinationName(trip: Trip?): String = trip?.destination ?: "—"

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
    var destination by remember { mutableStateOf(initialDestination) }
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

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(titleText) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text(stringResource(R.string.destination)) },
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
                if (showTripTypeSelector) {
                    Text(
                        text = stringResource(R.string.trip_type_label),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "aventura" to stringResource(R.string.trip_type_adventure),
                            "familiar" to stringResource(R.string.trip_type_family),
                            "solitario" to stringResource(R.string.trip_type_solo),
                            "amigos" to stringResource(R.string.trip_type_friends)
                        ).forEach { (key, label) ->
                            AssistChip(
                                onClick = { tripType = key },
                                label = { Text(label) }
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedBudget = budget.replace(",", ".").toDoubleOrNull()
                    localError = when {
                        title.isBlank() -> titleRequiredMessage
                        destination.isBlank() -> destinationRequiredMessage
                        startDate.isBlank() -> startDateRequiredMessage
                        endDate.isBlank() -> endDateRequiredMessage
                        parsedBudget == null -> budgetNumericMessage
                        parsedBudget < 0.0 -> budgetNegativeMessage
                        else -> null
                    }

                    if (localError == null && parsedBudget != null) {
                        onConfirm(title.trim(), destination.trim(), startDate, endDate, parsedBudget, tripType)
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(confirmText)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text(stringResource(R.string.cancel)) }
        }
    )
}
