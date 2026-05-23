package com.vaia.presentation.ui.currency

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaia.R
import com.vaia.presentation.ui.theme.SunAccent
import com.vaia.presentation.viewmodel.CurrencyInfo
import com.vaia.presentation.viewmodel.CurrencyUiState
import com.vaia.presentation.viewmodel.CurrencyViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyCalculatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: CurrencyViewModel
) {
    var amount by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("ARS") }
    
    var showFromSelector by remember { mutableStateOf(false) }
    var showToSelector by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    val rates by viewModel.rates.collectAsState()
    val availableCurrencies by viewModel.availableCurrencies.collectAsState()
    
    val result = remember(amount, fromCurrency, toCurrency, rates) {
        val input = amount.toDoubleOrNull() ?: 0.0
        val rate = rates[toCurrency] ?: 0.0
        input * rate
    }

    LaunchedEffect(fromCurrency) {
        viewModel.loadRates(fromCurrency)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversor de Divisas", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState is CurrencyUiState.Loading && rates.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Monto a convertir",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    TextField(
                        value = amount,
                        onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 40.sp
                        ),
                        placeholder = { 
                            Text(
                                "0.00", 
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            ) 
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // From Currency
                        CurrencyButton(
                            code = fromCurrency,
                            name = availableCurrencies.find { it.code == fromCurrency }?.countryName ?: "",
                            onClick = { showFromSelector = true },
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = {
                                val temp = fromCurrency
                                fromCurrency = toCurrency
                                toCurrency = temp
                            },
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .background(SunAccent.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Swap", tint = SunAccent)
                        }

                        // To Currency
                        CurrencyButton(
                            code = toCurrency,
                            name = availableCurrencies.find { it.code == toCurrency }?.countryName ?: "",
                            onClick = { showToSelector = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (uiState is CurrencyUiState.Error) {
                Text(
                    text = (uiState as CurrencyUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(onClick = { viewModel.loadRates(fromCurrency) }) {
                    Text("Reintentar")
                }
            }

            if (amount.isNotEmpty() && rates.isNotEmpty()) {
                Text(
                    text = "Resultado",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format(Locale.getDefault(), "%.2f", result)} $toCurrency",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val currentRate = rates[toCurrency] ?: 0.0
                if (currentRate != 0.0) {
                    Text(
                        text = "1 $fromCurrency = ${String.format(Locale.getDefault(), "%.4f", currentRate)} $toCurrency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showFromSelector) {
        CurrencySearchDialog(
            onDismiss = { showFromSelector = false },
            onSelect = { selected ->
                fromCurrency = selected
                showFromSelector = false
            },
            currencies = availableCurrencies
        )
    }

    if (showToSelector) {
        CurrencySearchDialog(
            onDismiss = { showToSelector = false },
            onSelect = { selected ->
                toCurrency = selected
                showToSelector = false
            },
            currencies = availableCurrencies
        )
    }
}

@Composable
fun CurrencyButton(
    code: String,
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = code, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Text(
                text = name, 
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySearchDialog(
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    currencies: List<CurrencyInfo>
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(searchQuery, currencies) {
        if (searchQuery.isEmpty()) {
            currencies
        } else {
            currencies.filter { 
                it.code.contains(searchQuery, ignoreCase = true) || 
                it.countryName.contains(searchQuery, ignoreCase = true)
            }
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
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Borrar")
                                    }
                                }
                            },
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cerrar")
                        }
                    }
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredCurrencies) { currency ->
                        ListItem(
                            headlineContent = { Text(currency.countryName, fontWeight = FontWeight.SemiBold) },
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
