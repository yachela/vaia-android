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
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.theme.BlueLight
import com.vaia.presentation.ui.theme.BluePrimary
import com.vaia.presentation.viewmodel.ExploreViewModel

// Gradientes por categoría de actividad
private fun categoryGradient(category: String): Pair<String, List<Color>> = when (category.lowercase()) {
    "cultura"         -> "🏛️" to listOf(Color(0xFF3949AB), Color(0xFF5C6BC0))
    "arte"            -> "🎨" to listOf(Color(0xFF8E24AA), Color(0xFFAB47BC))
    "naturaleza"      -> "🌿" to listOf(Color(0xFF2E7D32), Color(0xFF43A047))
    "gastronomía",
    "gastronomia"     -> "🍜" to listOf(Color(0xFFE65100), Color(0xFFF57C00))
    "playa"           -> "🏖️" to listOf(Color(0xFF0288D1), Color(0xFF4FC3F7))
    "aventura"        -> "⛰️" to listOf(Color(0xFF4E342E), Color(0xFF6D4C41))
    "musica"          -> "🎵" to listOf(Color(0xFF880E4F), Color(0xFFAD1457))
    "deporte"         -> "⚽" to listOf(Color(0xFF1B5E20), Color(0xFF388E3C))
    else              -> "🗺️" to listOf(BluePrimary, BlueLight)
}

// Emoji por nombre de destino
private fun destinationEmoji(name: String): String = when {
    name.contains("París", ignoreCase = true) || name.contains("Paris", ignoreCase = true) -> "🗼"
    name.contains("Tokyo", ignoreCase = true) || name.contains("Tokio", ignoreCase = true) -> "🗾"
    name.contains("Nueva York", ignoreCase = true) || name.contains("New York", ignoreCase = true) -> "🗽"
    name.contains("Barcelona", ignoreCase = true) -> "💃"
    name.contains("Roma", ignoreCase = true) || name.contains("Rome", ignoreCase = true) -> "🏛️"
    name.contains("Londres", ignoreCase = true) || name.contains("London", ignoreCase = true) -> "🎡"
    name.contains("Bali", ignoreCase = true) -> "🌴"
    name.contains("Miami", ignoreCase = true) -> "🏖️"
    name.contains("Cancún", ignoreCase = true) || name.contains("Cancun", ignoreCase = true) -> "🌊"
    name.contains("Buenos Aires", ignoreCase = true) -> "🥩"
    name.contains("Río", ignoreCase = true) || name.contains("Rio", ignoreCase = true) -> "🌅"
    name.contains("Machu Picchu", ignoreCase = true) || name.contains("Lima", ignoreCase = true) -> "🏔️"
    name.contains("Amsterdam", ignoreCase = true) -> "🌷"
    name.contains("Dubái", ignoreCase = true) || name.contains("Dubai", ignoreCase = true) -> "🌆"
    else -> "✈️"
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
    onNavigateProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

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
            AppQuickBar(
                currentRoute = "explore",
                onHome = onNavigateHome,
                onExplore = {},
                onTrips = onNavigateTrips,
                onProfile = onNavigateProfile
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
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
                        Text("😕", style = MaterialTheme.typography.displaySmall)
                        Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                        Button(onClick = { viewModel.loadExploreData() }) { Text("Reintentar") }
                    }
                }
            }

            is ExploreUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 16.dp),
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
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Destinos tendencia",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            TextButton(onClick = onNavigateToAllDestinations) {
                                Text("Ver todos")
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.trendingDestinations.take(6)) { destination ->
                                TrendingDestinationCard(
                                    destination = destination,
                                    onClick = { onNavigateToDestination(destination.id) }
                                )
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
                                contentDescription = null,
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
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.nearbyActivities) { activity ->
                                NearbyActivityCard(
                                    activity = activity,
                                    onClick = { onNavigateToActivity(activity.id) }
                                )
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
                                    contentDescription = null,
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
                                onClick = {}
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
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
    val emoji = destinationEmoji(destination.name)
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
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
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
    val (emoji, gradientColors) = categoryGradient(activity.category)
    Surface(
        onClick = onClick,
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        tonalElevation = 0.dp
    ) {
        Column {
            // Header con gradiente y emoji
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                // Icono decorativo de fondo
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.align(Alignment.Center)
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
                            contentDescription = null,
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
                        contentDescription = null,
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
            // Icono decorativo grande de fondo
            Text(
                text = "🌍",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier
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
                            contentDescription = null,
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
