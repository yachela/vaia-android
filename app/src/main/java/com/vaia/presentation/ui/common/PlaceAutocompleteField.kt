package com.vaia.presentation.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@Composable
fun PlaceAutocompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onImeAction: () -> Unit = {}
) {
    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var showDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(value) {
        // Debounce: esperamos 500ms antes de disparar la búsqueda
        if (value.length > 2 && showDropdown) {
            delay(500)
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setQuery(value)
                .build()
            
            try {
                val response = placesClient.findAutocompletePredictions(request).await()
                predictions = response.autocompletePredictions
            } catch (e: Exception) {
                predictions = emptyList()
            }
        } else {
            predictions = emptyList()
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                showDropdown = true
            },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Next
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onNext = { onImeAction() }
            )
        )

        if (showDropdown && predictions.isNotEmpty()) {
            Popup(
                properties = PopupProperties(focusable = false),
                onDismissRequest = { showDropdown = false }
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 4.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(predictions) { prediction ->
                            ListItem(
                                headlineContent = { Text(prediction.getPrimaryText(null).toString()) },
                                supportingContent = { Text(prediction.getSecondaryText(null).toString()) },
                                modifier = Modifier.clickable {
                                    onValueChange(prediction.getFullText(null).toString())
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
