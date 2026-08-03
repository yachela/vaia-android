package com.vaia.presentation.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vaia.R
import com.vaia.domain.model.CurrencyOption
import com.vaia.domain.model.Currencies

/**
 * Campo compacto para elegir la moneda: muestra símbolo y código, y abre el buscador.
 *
 * Va al lado del presupuesto en vez de ocupar una fila propia; el catálogo entero
 * queda detrás del buscador para que crear un viaje no obligue a mirar 50 países.
 */
@Composable
fun CurrencyPickerField(
    code: String,
    enabled: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.currency_label),
    profileCurrency: String? = null
) {
    var showPicker by remember { mutableStateOf(false) }
    val option = Currencies.optionOf(code)

    Box(modifier = modifier) {
        OutlinedTextField(
            // Sin moneda elegida el campo queda vacío: mostrar USD sugeriría algo que el
            // usuario no eligió. Con código guardado siempre resuelve por [Currencies.normalize].
            value = option?.let { "${it.symbol} ${it.code}" } ?: "",
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            // La flecha es la única señal de que el campo abre algo: sin ella se lee
            // como un campo de texto deshabilitado, igual que País e Idioma al lado.
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = stringResource(R.string.currency_picker_open)
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { showPicker = true }
            )
        }
    }

    if (showPicker) {
        CurrencyPickerDialog(
            current = code,
            profileCurrency = profileCurrency,
            onDismiss = { showPicker = false },
            onSelect = {
                onSelect(it)
                showPicker = false
            }
        )
    }
}

/**
 * Buscador de monedas a pantalla completa. Sin texto en la búsqueda arranca por las
 * sugeridas, que es lo que resuelve casi todos los viajes; el resto del catálogo va
 * abajo y se filtra por código o país.
 *
 * Es un [Dialog] y no un bottom sheet a propósito: se abre desde adentro del sheet
 * del formulario de viaje, y anidar sheets en Material 3 se rompe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerDialog(
    current: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    profileCurrency: String? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    val suggested = remember(current, profileCurrency) {
        Currencies.suggestedFor(profileCurrency, current)
    }
    val matches = remember(searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) emptyList()
        else Currencies.all.filter {
            it.code.contains(query, ignoreCase = true) ||
                it.countryName.contains(query, ignoreCase = true)
        }
    }
    val isSearching = searchQuery.isNotBlank()

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
                            placeholder = { Text(stringResource(R.string.currency_picker_search_hint)) },
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
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = stringResource(R.string.currency_picker_clear_search)
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
                                contentDescription = stringResource(R.string.currency_picker_close)
                            )
                        }
                    }
                )

                if (isSearching && matches.isEmpty()) {
                    Text(
                        text = stringResource(R.string.currency_picker_empty, searchQuery.trim()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                    return@Column
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (isSearching) {
                        items(matches, key = { it.code }) { CurrencyRow(it, current, onSelect) }
                    } else {
                        item { CurrencySectionHeader(stringResource(R.string.currency_picker_suggested)) }
                        items(suggested, key = { "suggested-${it.code}" }) {
                            CurrencyRow(it, current, onSelect)
                        }
                        item { CurrencySectionHeader(stringResource(R.string.currency_picker_all)) }
                        items(Currencies.all, key = { it.code }) { CurrencyRow(it, current, onSelect) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencySectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun CurrencyRow(
    option: CurrencyOption,
    current: String?,
    onSelect: (String) -> Unit
) {
    val isSelected = option.code.equals(current, ignoreCase = true)
    Column(verticalArrangement = Arrangement.Top) {
        ListItem(
            headlineContent = {
                Text(
                    text = option.countryName,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = { Text("${option.symbol} · ${option.code}") },
            modifier = Modifier.clickable { onSelect(option.code) }
        )
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
