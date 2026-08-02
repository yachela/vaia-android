package com.vaia.presentation.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.vaia.R

enum class AppExceptionType {
    DATE_VALIDATION,
    VALIDATION,
    NETWORK,
    AUTH,
    SERVER,
    FILE_TOO_LARGE,
    FILE_TYPE,
    SERVER_BUSY,
    UNKNOWN
}

data class AppExceptionUi(
    val type: AppExceptionType,
    val rawMessage: String
)

object AppExceptionMapper {
    fun fromMessage(message: String): AppExceptionUi {
        val normalized = message.lowercase()
        return when {
            // Antes que el resto: un 502 es intermitente y merece su propio mensaje, no el
            // "error del servidor" genérico ni, peor, el HTML de nginx en pantalla.
            normalized.contains("502") ||
                normalized.contains("503") ||
                normalized.contains("504") ||
                normalized.contains("bad gateway") ||
                normalized.contains("gateway time") ->
                AppExceptionUi(AppExceptionType.SERVER_BUSY, message)
            normalized.contains("fecha de inicio") && normalized.contains("posterior a hoy") ->
                AppExceptionUi(AppExceptionType.DATE_VALIDATION, message)
            normalized.contains("no internet") ||
                normalized.contains("timeout") ||
                normalized.contains("failed to connect") ||
                normalized.contains("unable to resolve host") ->
                AppExceptionUi(AppExceptionType.NETWORK, message)
            normalized.contains("unauthorized") || normalized.contains("401") || normalized.contains("token") ->
                AppExceptionUi(AppExceptionType.AUTH, message)
            normalized.contains("413") || normalized.contains("too large") || normalized.contains("grande") ->
                AppExceptionUi(AppExceptionType.FILE_TOO_LARGE, message)
            normalized.contains("extension") || normalized.contains("formato") || normalized.contains("type") || normalized.contains("ppt") ->
                AppExceptionUi(AppExceptionType.FILE_TYPE, message)
            normalized.contains("422") || normalized.contains("valid") || normalized.contains("obligatoria") ->
                AppExceptionUi(AppExceptionType.VALIDATION, message)
            normalized.contains("500") || normalized.contains("server") ->
                AppExceptionUi(AppExceptionType.SERVER, message)
            else ->
                AppExceptionUi(AppExceptionType.UNKNOWN, message)
        }
    }
}

@Composable
fun AppExceptionDialog(
    exception: AppExceptionUi,
    onDismiss: () -> Unit
) {
    val title = when (exception.type) {
        AppExceptionType.DATE_VALIDATION -> stringResource(R.string.exception_title_date)
        AppExceptionType.VALIDATION -> stringResource(R.string.exception_title_validation)
        AppExceptionType.NETWORK -> stringResource(R.string.exception_title_network)
        AppExceptionType.AUTH -> stringResource(R.string.exception_title_auth)
        AppExceptionType.SERVER -> stringResource(R.string.exception_title_server)
        AppExceptionType.FILE_TOO_LARGE -> stringResource(R.string.exception_title_file_too_large)
        AppExceptionType.FILE_TYPE -> stringResource(R.string.exception_title_file_type)
        AppExceptionType.SERVER_BUSY -> stringResource(R.string.exception_title_server_busy)
        AppExceptionType.UNKNOWN -> stringResource(R.string.exception_title_unknown)
    }

    // Se elige uno al azar por cada error, pero se fija mientras el diálogo esté abierto para
    // que no cambie de texto en cada recomposición.
    val serverBusyMessages = stringArrayResource(R.array.exception_msg_server_busy)
    val serverBusyMessage = remember(exception) { serverBusyMessages.random() }

    val body = when (exception.type) {
        AppExceptionType.DATE_VALIDATION -> stringResource(R.string.exception_msg_date_after_today)
        AppExceptionType.NETWORK -> stringResource(R.string.exception_msg_network)
        AppExceptionType.AUTH -> stringResource(R.string.exception_msg_auth)
        AppExceptionType.SERVER -> stringResource(R.string.exception_msg_server)
        AppExceptionType.FILE_TOO_LARGE -> stringResource(R.string.exception_msg_file_too_large)
        AppExceptionType.FILE_TYPE -> stringResource(R.string.exception_msg_file_type)
        AppExceptionType.VALIDATION -> exception.rawMessage
        AppExceptionType.SERVER_BUSY -> serverBusyMessage
        AppExceptionType.UNKNOWN -> exception.rawMessage.ifBlank { stringResource(R.string.exception_msg_unknown) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.accept))
            }
        }
    )
}
