package com.vaia.presentation.ui.common

import androidx.compose.ui.res.stringResource
import com.vaia.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaiaTimePickerField(
    value: String,
    label: String,
    enabled: Boolean,
    onTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val initialHour = value.substringBefore(":").toIntOrNull() ?: 12
    val initialMinute = value.substringAfter(":", "00").toIntOrNull() ?: 0
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )

    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        enabled = enabled,
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = stringResource(R.string.select_time)
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged {
                if (enabled && it.isFocused) showTimePicker = true
            }
            .clickable(enabled = enabled) { showTimePicker = true }
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.select_time)) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeSelected(
                            String.format(
                                Locale.US,
                                "%02d:%02d",
                                timePickerState.hour,
                                timePickerState.minute
                            )
                        )
                        showTimePicker = false
                    }
                ) { Text(stringResource(R.string.accept)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
