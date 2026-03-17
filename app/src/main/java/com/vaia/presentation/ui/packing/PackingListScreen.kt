package com.vaia.presentation.ui.packing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaia.domain.model.PackingCategory
import com.vaia.domain.model.PackingItem
import com.vaia.presentation.viewmodel.PackingListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackingListScreen(
    tripId: String,
    tripName: String,
    daysUntilDeparture: Int,
    viewModel: PackingListViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToExplore: () -> Unit = {},
    onNavigateToWeather: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddItemDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(tripId) {
        viewModel.loadPackingList(tripId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lista de Equipaje") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            PackingContextualNavigationBar(
                currentRoute = "packing",
                onNavigateToPacking = { /* Already here */ },
                onNavigateToExplore = onNavigateToExplore,
                onNavigateToWeather = onNavigateToWeather,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar ítem")
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is PackingListUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is PackingListUiState.Error -> {
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
            is PackingListUiState.Success, is PackingListUiState.Syncing -> {
                val packingList = when (state) {
                    is PackingListUiState.Success -> state.packingList
                    is PackingListUiState.Syncing -> state.packingList
                    else -> return@Scaffold
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Header
                    item {
                        PackingListHeader(
                            tripName = tripName,
                            daysUntilDeparture = daysUntilDeparture,
                            progress = packingList.progress
                        )
                    }

                    // Search bar
                    item {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // Categories with items
                    val filteredCategories = if (searchQuery.isBlank()) {
                        packingList.itemsByCategory
                    } else {
                        packingList.itemsByCategory.map { category ->
                            category.copy(
                                items = category.items.filter {
                                    it.name.contains(searchQuery, ignoreCase = true)
                                }
                            )
                        }.filter { it.items.isNotEmpty() }
                    }

                    filteredCategories.forEach { category ->
                        item {
                            CategorySection(
                                category = category,
                                onToggleItem = { itemId -> viewModel.toggleItem(itemId) },
                                onDeleteItem = { itemId -> viewModel.deleteItem(itemId, tripId) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        AddItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, category ->
                viewModel.addItem(tripId, name, category)
                showAddItemDialog = false
            }
        )
    }
}


@Composable
private fun PackingListHeader(
    tripName: String,
    daysUntilDeparture: Int,
    progress: com.vaia.domain.model.PackingProgress
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = tripName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Faltan $daysUntilDeparture días para la partida",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress bar
        LinearProgressIndicator(
            progress = { progress.percentage / 100f },
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF4CAF50)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${progress.packed} de ${progress.total} ítems empacados (${progress.percentage}%)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Buscar ítems...") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                }
            }
        },
        singleLine = true
    )
}

@Composable
private fun CategorySection(
    category: PackingCategory,
    onToggleItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        // Category header
        Surface(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getCategoryIcon(category.category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category.category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Contraer" else "Expandir"
                )
            }
        }
        
        // Items
        if (expanded) {
            category.items.forEach { item ->
                PackingItemRow(
                    item = item,
                    onToggle = { onToggleItem(item.id) },
                    onDelete = { onDeleteItem(item.id) }
                )
            }
        }
        
        Divider()
    }
}


@Composable
private fun PackingItemRow(
    item: PackingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isPacked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = Color(0xFF4CAF50)
            )
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (item.isSuggested) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "SUGERIDO",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
            
            if (item.isSuggested && item.suggestionReason != null) {
                Text(
                    text = item.suggestionReason,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
            }
        }
        
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Higiene") }
    var expanded by remember { mutableStateOf(false) }
    
    val categories = listOf("Higiene", "Ropa", "Tecnología", "Documentación")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar ítem") },
        text = {
            Column {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Nombre del ítem") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (itemName.isNotBlank()) {
                        onConfirm(itemName, selectedCategory)
                    }
                },
                enabled = itemName.isNotBlank()
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private fun getCategoryIcon(category: String) = when (category) {
    "Higiene" -> Icons.Default.Face
    "Ropa" -> Icons.Default.CheckCircle
    "Tecnología" -> Icons.Default.Phone
    "Documentación" -> Icons.Default.Info
    else -> Icons.Default.CheckCircle
}

@Composable
private fun PackingContextualNavigationBar(
    currentRoute: String,
    onNavigateToPacking: () -> Unit,
    onNavigateToExplore: () -> Unit,
    onNavigateToWeather: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        NavigationBarItem(
            selected = currentRoute == "packing",
            onClick = onNavigateToPacking,
            colors = colors,
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            label = { Text("Equipaje") }
        )
        NavigationBarItem(
            selected = currentRoute == "explore",
            onClick = onNavigateToExplore,
            colors = colors,
            icon = { Icon(Icons.Default.Explore, contentDescription = null) },
            label = { Text("Explorar") }
        )
        NavigationBarItem(
            selected = currentRoute == "weather",
            onClick = onNavigateToWeather,
            colors = colors,
            icon = { Icon(Icons.Default.WbSunny, contentDescription = null) },
            label = { Text("Clima") }
        )
        NavigationBarItem(
            selected = currentRoute == "profile",
            onClick = onNavigateToProfile,
            colors = colors,
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Perfil") }
        )
    }
}
