package com.vaia.presentation.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.vaia.R

enum class AppExceptionType {
    DATE_VALIDATION,
    VALIDATION,
    NETWORK,
    AUTH,
    SERVER,
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
            normalized.contains("fecha de inicio") && normalized.contains("posterior a hoy") ->
                AppExceptionUi(AppExceptionType.DATE_VALIDATION, message)
            normalized.contains("no internet") ||
                normalized.contains("timeout") ||
                normalized.contains("failed to connect") ||
                normalized.contains("unable to resolve host") ->
                AppExceptionUi(AppExceptionType.NETWORK, message)
            normalized.contains("unauthorized") || normalized.contains("401") || normalized.contains("token") ->
                AppExceptionUi(AppExceptionType.AUTH, message)
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
        AppExceptionType.UNKNOWN -> stringResource(R.string.exception_title_unknown)
    }

    val body = when (exception.type) {
        AppExceptionType.DATE_VALIDATION -> stringResource(R.string.exception_msg_date_after_today)
        AppExceptionType.NETWORK -> stringResource(R.string.exception_msg_network)
        AppExceptionType.AUTH -> stringResource(R.string.exception_msg_auth)
        AppExceptionType.SERVER -> stringResource(R.string.exception_msg_server)
        AppExceptionType.VALIDATION -> exception.rawMessage
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
