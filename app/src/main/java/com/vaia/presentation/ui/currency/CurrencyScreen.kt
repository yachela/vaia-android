package com.vaia.presentation.ui.currency

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaia.R
import com.vaia.domain.model.Trip
import com.vaia.presentation.ui.common.AppQuickBar
import com.vaia.presentation.ui.common.TopBar
import com.vaia.presentation.ui.common.WaypathButton
import com.vaia.presentation.viewmodel.CurrencyInfo
import com.vaia.presentation.viewmodel.CurrencyUiState
import com.vaia.presentation.viewmodel.CurrencyViewModel
import com.vaia.presentation.viewmodel.ExpensesViewModel
import com.vaia.presentation.viewmodel.TripsViewModel
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyScreen(
    onNavigateHome: () -> Unit,
    onNavigateTrips: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateOrganizer: () -> Unit,
    onNavigateCalendar: () -> Unit,
    onNavigateCurrency: () -> Unit,
    onNavigateNotifications: () -> Unit = {},
    viewModel: CurrencyViewModel,
    tripsViewModel: TripsViewModel,
    expensesViewModel: ExpensesViewModel
) {
    val trips by tripsViewModel.trips.collectAsState()
    val isLoadingTrips by tripsViewModel.isLoading.collectAsState()
    val createState by expensesViewModel.createState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val rates by viewModel.rates.collectAsState()
    val availableCurrencies by viewModel.availableCurrencies.collectAsState()

    var converterAmount by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("ARS") }
    var showFromSelector by remember { mutableStateOf(false) }
    var showToSelector by remember { mutableStateOf(false) }

    val conversionResult = remember(converterAmount, fromCurrency, toCurrency, rates) {
        val input = converterAmount.toDoubleOrNull() ?: 0.0
        input * (rates[toCurrency] ?: 0.0)
    }

    var expenseAccordionExpanded by remember { mutableStateOf(false) }
    var selectedTrip by remember { mutableStateOf<Trip?>(null) }
    var tripExpanded by remember { mutableStateOf(false) }
    var expenseTitle by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val expenseCategories = stringArrayResource(R.array.expense_categories).toList()
    var selectedCategory by remember(expenseCategories) {
        mutableStateOf(expenseCategories.firstOrNull() ?: "")
    }
    var categoryExpanded by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now().toString() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(fromCurrency) { viewModel.loadRates(fromCurrency) }
    LaunchedEffect(Unit) { if (trips.isEmpty()) tripsViewModel.loadTrips() }
    LaunchedEffect(trips) { if (selectedTrip == null && trips.isNotEmpty()) selectedTrip = trips.first() }

    LaunchedEffect(createState) {
        when (createState) {
            is ExpensesViewModel.CreateState.Success -> {
                snackbarHostState.showSnackbar("Gasto guardado correctamente")
                expenseTitle = ""
                expenseAmount = ""
                selectedCategory = expenseCategories.firstOrNull() ?: ""
                localError = null
                expenseAccordionExpanded = false
                expensesViewModel.resetCreateState()
            }
            is ExpensesViewModel.CreateState.Error -> {
                snackbarHostState.showSnackbar((createState as ExpensesViewModel.CreateState.Error).message)
                expensesViewModel.resetCreateState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                onNotificationsClick = onNavigateNotifications,
                onProfileClick = onNavigateProfile
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
                    currentRoute = "currency",
                    onHome = onNavigateHome,
                    onMap = onNavigateOrganizer,
                    onTrips = onNavigateTrips,
                    onCalendar = onNavigateCalendar,
                    onCurrency = onNavigateCurrency
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Header de sección ────────────────────────────────────────────
            Text(
                text = stringResource(R.string.currency_section_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = stringResource(R.string.currency_section_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            CurrencyConverterCard(
                amount = converterAmount,
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                result = conversionResult,
                rates = rates,
                uiState = uiState,
                availableCurrencies = availableCurrencies,
                onAmountChange = { converterAmount = it },
                onSwap = { val tmp = fromCurrency; fromCurrency = toCurrency; toCurrency = tmp },
                onFromClick = { showFromSelector = true },
                onToClick = { showToSelector = true },
                onRetry = { viewModel.loadRates(fromCurrency) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            ExpenseAccordion(
                expanded = expenseAccordionExpanded,
                onToggle = { expenseAccordionExpanded = !expenseAccordionExpanded },
                trips = trips,
                isLoadingTrips = isLoadingTrips,
                selectedTrip = selectedTrip,
                tripExpanded = tripExpanded,
                onTripExpandedChange = { tripExpanded = it },
                onTripSelected = { selectedTrip = it },
                expenseTitle = expenseTitle,
                onExpenseTitleChange = { expenseTitle = it },
                expenseAmount = expenseAmount,
                onExpenseAmountChange = { expenseAmount = it },
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                categoryExpanded = categoryExpanded,
                onCategoryExpandedChange = { categoryExpanded = it },
                expenseCategories = expenseCategories,
                localError = localError,
                isSaving = createState is ExpensesViewModel.CreateState.Loading,
                onSave = {
                    val amountValue = expenseAmount.replace(",", ".").toDoubleOrNull()
                    localError = when {
                        expenseTitle.isBlank() -> "La descripción es obligatoria"
                        amountValue == null -> "Ingresá un monto numérico válido"
                        selectedTrip == null -> "Seleccioná un viaje"
                        else -> null
                    }
                    if (localError == null && amountValue != null && selectedTrip != null) {
                        expensesViewModel.createExpenseForTrip(
                            forTripId = selectedTrip!!.id,
                            description = expenseTitle.trim(),
                            amount = amountValue,
                            category = selectedCategory,
                            date = today
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showFromSelector) {
        CurrencySearchDialog(
            onDismiss = { showFromSelector = false },
            onSelect = { fromCurrency = it; showFromSelector = false },
            currencies = availableCurrencies
        )
    }
    if (showToSelector) {
        CurrencySearchDialog(
            onDismiss = { showToSelector = false },
            onSelect = { toCurrency = it; showToSelector = false },
            currencies = availableCurrencies
        )
    }
}

// ── Conversor card ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyConverterCard(
    amount: String,
    fromCurrency: String,
    toCurrency: String,
    result: Double,
    rates: Map<String, Double>,
    uiState: CurrencyUiState,
    availableCurrencies: List<CurrencyInfo>,
    onAmountChange: (String) -> Unit,
    onSwap: () -> Unit,
    onFromClick: () -> Unit,
    onToClick: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Sección 1: Monto ──────────────────────────────────────────────
            Text(
                text = stringResource(R.string.converter_amount_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState is CurrencyUiState.Loading && rates.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.padding(12.dp))
            }

            TextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onAmountChange(it) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                placeholder = {
                    Text(
                        "0.00",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(20.dp))

            // ── Sección 2: Paneles de divisa ──────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    NotchedCurrencyPanel(
                        label = stringResource(R.string.currency_from_label),
                        code = fromCurrency,
                        name = availableCurrencies.find { it.code == fromCurrency }?.countryName ?: "",
                        onClick = onFromClick,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(44.dp))
                    NotchedCurrencyPanel(
                        label = stringResource(R.string.currency_to_label),
                        code = toCurrency,
                        name = availableCurrencies.find { it.code == toCurrency }?.countryName ?: "",
                        onClick = onToClick,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Swap centrado en el cuerpo del panel (offset compensa el label de encima)
                Surface(
                    onClick = onSwap,
                    modifier = Modifier
                        .size(40.dp)
                        .offset(y = 10.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = stringResource(R.string.swap_currencies),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (uiState is CurrencyUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (uiState as CurrencyUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }

            // ── Sección 3: Resultado ──────────────────────────────────────────
            if (amount.isNotEmpty() && rates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.converter_result_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${String.format(Locale.getDefault(), "%.2f", result)} $toCurrency",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )
                val currentRate = rates[toCurrency] ?: 0.0
                if (currentRate != 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1 $fromCurrency = ${String.format(Locale.getDefault(), "%.4f", currentRate)} $toCurrency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Panel de divisa con etiqueta flotante encima ──────────────────────────────

@Composable
private fun NotchedCurrencyPanel(
    label: String,
    code: String,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Etiqueta encima del panel, alineada a la izquierda
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(start = 10.dp, bottom = 4.dp)
        )

        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ── Acordeón: Registrar Gasto ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseAccordion(
    expanded: Boolean,
    onToggle: () -> Unit,
    trips: List<Trip>,
    isLoadingTrips: Boolean,
    selectedTrip: Trip?,
    tripExpanded: Boolean,
    onTripExpandedChange: (Boolean) -> Unit,
    onTripSelected: (Trip) -> Unit,
    expenseTitle: String,
    onExpenseTitleChange: (String) -> Unit,
    expenseAmount: String,
    onExpenseAmountChange: (String) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    categoryExpanded: Boolean,
    onCategoryExpandedChange: (Boolean) -> Unit,
    expenseCategories: List<String>,
    localError: String?,
    isSaving: Boolean,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Surface(
                onClick = onToggle,
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.register_expense),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Colapsar" else "Expandir",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                    Divider(
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Text(
                        text = stringResource(R.string.select_trip),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoadingTrips && trips.isEmpty()) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(8.dp)
                        )
                    } else {
                        TripSelectorDropdown(
                            trips = trips,
                            selectedTrip = selectedTrip,
                            expanded = tripExpanded,
                            onExpandedChange = onTripExpandedChange,
                            onTripSelected = onTripSelected
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = expenseTitle,
                        onValueChange = onExpenseTitleChange,
                        label = { Text(stringResource(R.string.expense_description_hint)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = expenseAmount,
                        onValueChange = onExpenseAmountChange,
                        label = { Text(stringResource(R.string.amount)) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.AttachMoney,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = onCategoryExpandedChange
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.category)) },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { onCategoryExpandedChange(false) }
                        ) {
                            expenseCategories.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        onCategorySelected(option)
                                        onCategoryExpandedChange(false)
                                    }
                                )
                            }
                        }
                    }

                    if (!localError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = localError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    WaypathButton(
                        text = stringResource(R.string.save_expense),
                        enabled = !isSaving && selectedTrip != null,
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ── Selector de viaje ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripSelectorDropdown(
    trips: List<Trip>,
    selectedTrip: Trip?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTripSelected: (Trip) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = selectedTrip?.title ?: stringResource(R.string.no_trips_available),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            enabled = trips.isNotEmpty()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            trips.forEach { trip ->
                DropdownMenuItem(
                    text = { Text(trip.title) },
                    onClick = { onTripSelected(trip); onExpandedChange(false) }
                )
            }
        }
    }
}

// ── Diálogo de búsqueda de divisas ───────────────────────────────────────────
// (movido desde CurrencyCalculatorScreen al unificar ambas pantallas)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySearchDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    currencies: List<CurrencyInfo>
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(searchQuery, currencies) {
        if (searchQuery.isEmpty()) currencies
        else currencies.filter {
            it.code.contains(searchQuery, ignoreCase = true) ||
            it.countryName.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar país o divisa...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Borrar"
                                        )
                                    }
                                }
                            },
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Cerrar"
                            )
                        }
                    }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCurrencies) { currency ->
                        ListItem(
                            headlineContent = {
                                Text(currency.countryName, fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = { Text(currency.code) },
                            modifier = Modifier.clickable { onSelect(currency.code) }
                        )
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}
