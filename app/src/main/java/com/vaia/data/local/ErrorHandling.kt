package com.vaia.data.local

import android.util.Log
import java.util.UUID

sealed class AppException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    class Network(message: String, cause: Throwable? = null) : AppException(message, cause)
    class Api(message: String, cause: Throwable? = null) : AppException(message, cause)
    class Validation(message: String, cause: Throwable? = null) : AppException(message, cause)
    class Authentication(message: String, cause: Throwable? = null) : AppException(message, cause)
    class Unknown(message: String, cause: Throwable? = null) : AppException(message, cause)
}

object ErrorLogger {
    private const val BASE_TAG = "VAIA_ERROR"

    fun log(
        feature: String,
        operation: String,
        throwable: Throwable,
        metadata: Map<String, Any?> = emptyMap()
    ): String {
        val errorId = "ERR-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        val meta = if (metadata.isEmpty()) "" else metadata.entries.joinToString(
            prefix = " | ",
            separator = ", "
        ) { "${it.key}=${it.value}" }

        val message = "id=$errorId feature=$feature operation=$operation${meta} cause=${throwable.message}"
        Log.e(BASE_TAG, message, throwable)
        return errorId
    }

    fun logAndWrap(
        feature: String,
        operation: String,
        throwable: Throwable,
        defaultMessage: String,
        metadata: Map<String, Any?> = emptyMap()
    ): AppException {
        val errorId = log(feature, operation, throwable, metadata)
        val wrapped = throwable.toAppException(defaultMessage)
        val withIdMessage = "[$errorId] ${wrapped.message ?: defaultMessage}"
        return when (wrapped) {
            is AppException.Network -> AppException.Network(withIdMessage, wrapped.cause)
            is AppException.Api -> AppException.Api(withIdMessage, wrapped.cause)
            is AppException.Validation -> AppException.Validation(withIdMessage, wrapped.cause)
            is AppException.Authentication -> AppException.Authentication(withIdMessage, wrapped.cause)
            is AppException.Unknown -> AppException.Unknown(withIdMessage, wrapped.cause)
        }
    }
}

fun Throwable.toAppException(defaultMessage: String): AppException {
    val currentMessage = message ?: defaultMessage
    val normalized = currentMessage.lowercase()

    return when {
        normalized.contains("timeout") ||
            normalized.contains("unable to resolve host") ||
            normalized.contains("failed to connect") ->
            AppException.Network(currentMessage, this)

        normalized.contains("unauthorized") || normalized.contains("401") ->
            AppException.Authentication(currentMessage, this)

        normalized.contains("422") ||
            normalized.contains("validación") ||
            normalized.contains("obligatorio") ->
            AppException.Validation(currentMessage, this)

        normalized.contains("failed") ||
            normalized.contains("error") ->
            AppException.Api(currentMessage, this)

        else -> AppException.Unknown(currentMessage, this)
    }
}
