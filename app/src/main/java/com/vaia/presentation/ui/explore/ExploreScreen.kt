package com.vaia.presentation.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.WaypathButton
import com.vaia.presentation.ui.theme.BlueLight
import com.vaia.presentation.ui.theme.BluePrimary
import com.vaia.presentation.viewmodel.ExploreViewModel
import com.vaia.R

// Íconos por categoría de actividad
private fun categoryGradient(category: String): Pair<ImageVector, List<Color>> = when (category.lowercase()) {
    "cultura"         -> Icons.Default.AccountBalance to listOf(Color(0xFF3949AB), Color(0xFF5C6BC0))
    "arte"            -> Icons.Default.Palette to listOf(Color(0xFF8E24AA), Color(0xFFAB47BC))
    "naturaleza"      -> Icons.Default.Park to listOf(Color(0xFF2E7D32), Color(0xFF43A047))
    "gastronomía",
    "gastronomia"     -> Icons.Default.Restaurant to listOf(Color(0xFFE65100), Color(0xFFF57C00))
    "playa"           -> Icons.Default.BeachAccess to listOf(Color(0xFF0288D1), Color(0xFF4FC3F7))
    "aventura"        -> Icons.Default.Landscape to listOf(Color(0xFF4E342E), Color(0xFF6D4C41))
    "musica"          -> Icons.Default.MusicNote to listOf(Color(0xFF880E4F), Color(0xFFAD1457))
    "deporte"         -> Icons.Default.SportsSoccer to listOf(Color(0xFF1B5E20), Color(0xFF388E3C))
    else              -> Icons.Default.Map to listOf(BluePrimary, BlueLight)
}

// Ícono por nombre de destino
private fun destinationIcon(name: String): ImageVector = when {
    name.contains("París", ignoreCase = true) || name.contains("Paris", ignoreCase = true) -> Icons.Default.LocationCity
    name.contains("Tokyo", ignoreCase = true) || name.contains("Tokio", ignoreCase = true) -> Icons.Default.LocationCity
    name.contains("Nueva York", ignoreCase = true) || name.contains("New York", ignoreCase = true) -> Icons.Default.LocationCity
    name.contains("Barcelona", ignoreCase = true) -> Icons.Default.LocationCity
    name.contains("Roma", ignoreCase = true) || name.contains("Rome", ignoreCase = true) -> Icons.Default.AccountBalance
    name.contains("Londres", ignoreCase = true) || name.contains("London", ignoreCase = true) -> Icons.Default.LocationCity
    name.contains("Bali", ignoreCase = true) -> Icons.Default.BeachAccess
    name.contains("Miami", ignoreCase = true) -> Icons.Default.BeachAccess
    name.contains("Cancún", ignoreCase = true) || name.contains("Cancun", ignoreCase = true) -> Icons.Default.BeachAccess
    name.contains("Buenos Aires", ignoreCase = true) -> Icons.Default.LocationCity
    name.contains("Río", ignoreCase = true) || name.contains("Rio", ignoreCase = true) -> Icons.Default.LocationCity
    name.contains("Machu Picchu", ignoreCase = true) || name.contains("Lima", ignoreCase = true) -> Icons.Default.Landscape
    name.contains("Amsterdam", ignoreCase = true) -> Icons.Default.LocationCity
    name.contains("Dubái", ignoreCase = true) || name.contains("Dubai", ignoreCase = true) -> Icons.Default.LocationCity
    else -> Icons.Default.Flight
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ExploreViewModel = hiltViewModel(),
    onNavigateToDestination: (String) -> Unit,
    onNavigateToActivity: (String) -> Unit,
    onNavigateToAllDestinations: () -> Unit,
    onNavigateHome: () -> Unit = {},
    onNavigateTrips: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onNavigateOrganizer: () -> Unit = {},
    onNavigateCalendar: () -> Unit = {}

) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    var selectedDestination by remember { mutableStateOf<TrendingDestination?>(null) }
    var selectedActivity by remember { mutableStateOf<NearbyActivity?>(null) }

    val destinationSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activitySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) { viewModel.loadExploreData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Explorar",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .navigationBarsPadding()
            ) {
                AppQuickBar(
                    currentRoute = "explore",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is ExploreUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is ExploreUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.SentimentDissatisfied,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.error,
                                contentDescription = stringResource(R.string.search)
                            )
                            Text(
                                state.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = { viewModel.loadExploreData() }) { Text("Reintentar") }
                        }
                    }
                }

                is ExploreUiState.Success -> {
                    // Filtrado client-side según búsqueda
                    val filteredDestinations = remember(searchQuery, state.trendingDestinations) {
                        if (searchQuery.isBlank()) state.trendingDestinations
                        else state.trendingDestinations.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.country.contains(searchQuery, ignoreCase = true)
                        }
                    }
                    val filteredActivities = remember(searchQuery, state.nearbyActivities) {
                        if (searchQuery.isBlank()) state.nearbyActivities
                        else state.nearbyActivities.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                                    it.category.contains(searchQuery, ignoreCase = true) ||
                                    it.location.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            bottom = 100.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Barra de búsqueda
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                placeholder = { Text("Buscar destinos, actividades...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp)
                            )
                        }

                        // Destinos Tendencia
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = stringResource(R.string.explore),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Destinos tendencia",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                TextButton(onClick = onNavigateTrips) {
                                    Text("Ver todos")
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        item {
                            if (filteredDestinations.isEmpty() && searchQuery.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Sin resultados para \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredDestinations.take(6)) { destination ->
                                        TrendingDestinationCard(
                                            destination = destination,
                                            onClick = { selectedDestination = destination }
                                        )
                                    }
                                }
                            }
                        }

                        // Actividades cercanas
                        item {
                            Spacer(Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.LocationOn,
                                    contentDescription = stringResource(R.string.location),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Actividades cercanas",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        item {
                            if (filteredActivities.isEmpty() && searchQuery.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Sin actividades para \"$searchQuery\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredActivities) { activity ->
                                        NearbyActivityCard(
                                            activity = activity,
                                            onClick = { selectedActivity = activity }
                                        )
                                    }
                                }
                            }
                        }

                        // Elección del Editor
                        state.editorChoice?.let { editorChoice ->
                            item {
                                Spacer(Modifier.height(24.dp))
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = stringResource(R.string.ia_suggestions),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Destacado",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                EditorChoiceCard(
                                    editorChoice = editorChoice,
                                    onClick = { onNavigateTrips() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet — Destino seleccionado
    selectedDestination?.let { destination ->
        ModalBottomSheet(
            onDismissRequest = { selectedDestination = null },
            sheetState = destinationSheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            DestinationDetailSheet(
                destination = destination,
                onPlanTrip = {
                    selectedDestination = null
                    onNavigateTrips()
                },
                onDismiss = { selectedDestination = null }
            )
        }
    }

    // Bottom sheet — Actividad seleccionada
    selectedActivity?.let { activity ->
        ModalBottomSheet(
            onDismissRequest = { selectedActivity = null },
            sheetState = activitySheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ActivityDetailSheet(
                activity = activity,
                onPlanTrip = {
                    selectedActivity = null
                    onNavigateTrips()
                },
                onDismiss = { selectedActivity = null }
            )
        }
    }
}

@Composable
private fun DestinationDetailSheet(
    destination: TrendingDestination,
    onPlanTrip: () -> Unit,
    onDismiss: () -> Unit
) {
    val icon = destinationIcon(destination.name)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Ícono grande
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(colors = listOf(BluePrimary, BlueLight))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = destination.name, tint = Color.White, modifier = Modifier.size(40.dp))
        }

        // Nombre y país
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = destination.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = destination.country,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        Text(
            text = "¿Querés planificar un viaje a ${destination.name}?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        WaypathButton(
            text = "Planificar viaje",
            onClick = onPlanTrip,
            modifier = Modifier.fillMaxWidth()
        )

        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar")
        }
    }
}

@Composable
private fun ActivityDetailSheet(
    activity: NearbyActivity,
    onPlanTrip: () -> Unit,
    onDismiss: () -> Unit
) {
    val (icon, gradientColors) = categoryGradient(activity.category)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header con gradiente
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = activity.name,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                // Badge categoría
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = activity.category,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                // Ubicación
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.LocationOn,
                        contentDescription = stringResource(R.string.location),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${activity.location} · ${activity.distance}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Text(
                text = "¿Querés incluir esta actividad en un viaje?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            WaypathButton(
                text = "Planificar viaje",
                onClick = onPlanTrip,
                modifier = Modifier.fillMaxWidth()
            )

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar")
            }
        }
    }
}

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TrendingDestinationCard(
        destination: TrendingDestination,
        onClick: () -> Unit
    ) {
        val icon = destinationIcon(destination.name)
        Surface(
            onClick = onClick,
            modifier = Modifier.width(110.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            tonalElevation = 0.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(colors = listOf(BluePrimary, BlueLight))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = destination.name,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = destination.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NearbyActivityCard(
        activity: NearbyActivity,
        onClick: () -> Unit
    ) {
        val (icon, gradientColors) = categoryGradient(activity.category)
        Surface(
            onClick = onClick,
            modifier = Modifier.width(260.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp,
            tonalElevation = 0.dp
        ) {
            Column {
                // Header con gradiente e ícono
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(Brush.linearGradient(gradientColors)),
                    contentAlignment = Alignment.Center
                ) {
                    // Ícono decorativo de fondo
                    Icon(
                        imageVector = icon,
                        contentDescription = activity.name,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(48.dp).align(Alignment.Center)
                    )
                    // Badge de categoría
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = activity.category,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                    // Distancia
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Outlined.LocationOn,
                                contentDescription = stringResource(R.string.location),
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = activity.distance,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
                // Contenido de texto
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = activity.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = stringResource(R.string.location),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = activity.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EditorChoiceCard(
        editorChoice: EditorChoice,
        onClick: () -> Unit
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(220.dp),
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 4.dp,
            tonalElevation = 0.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Fondo con gradiente vibrante
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0D47A1),
                                    Color(0xFF1565C0),
                                    Color(0xFF1E88E5)
                                )
                            )
                        )
                )
                // Ícono decorativo grande de fondo
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = stringResource(R.string.explore),
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .offset(y = 8.dp)
                )
                // Círculo decorativo
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .offset(x = (-40).dp, y = (-40).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 30.dp, y = 30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                )

                // Overlay inferior para legibilidad
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f))
                            )
                        )
                )

                // Contenido
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Badge superior
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = stringResource(R.string.ia_suggestions),
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = editorChoice.label,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    // Título y descripción
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = editorChoice.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = editorChoice.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
