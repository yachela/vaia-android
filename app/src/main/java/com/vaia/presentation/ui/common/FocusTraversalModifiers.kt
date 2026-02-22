package com.vaia.presentation.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

fun Modifier.moveFocusOnEnterOrTab(
    focusManager: FocusManager,
    isDoneField: Boolean = false,
    onDone: (() -> Unit)? = null
): Modifier {
    return onPreviewKeyEvent { event ->
        val isKeyDown = event.type == KeyEventType.KeyDown
        val isTraverseKey = event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Tab
        if (!isKeyDown || !isTraverseKey) return@onPreviewKeyEvent false

        if (isDoneField) {
            focusManager.clearFocus()
            onDone?.invoke()
        } else {
            focusManager.moveFocus(FocusDirection.Down)
        }
        true
    }
}
