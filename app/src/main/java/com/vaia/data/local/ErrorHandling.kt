package com.vaia.data.local

import android.util.Log
import java.util.UUID

/**
 * [errorId] es el código de correlación con el log (`ERR-XXXXXXXX`). Va como campo y no
 * dentro de [message]: el mensaje se muestra tal cual en pantalla y a quien está de viaje
 * el código no le sirve para nada.
 */
sealed class AppException(
    message: String,
    cause: Throwable? = null,
    val errorId: String? = null
) : Exception(message, cause) {
    class Network(message: String, cause: Throwable? = null, errorId: String? = null) :
        AppException(message, cause, errorId)

    class Api(message: String, cause: Throwable? = null, errorId: String? = null) :
        AppException(message, cause, errorId)

    class Validation(message: String, cause: Throwable? = null, errorId: String? = null) :
        AppException(message, cause, errorId)

    class Authentication(message: String, cause: Throwable? = null, errorId: String? = null) :
        AppException(message, cause, errorId)

    class Unknown(message: String, cause: Throwable? = null, errorId: String? = null) :
        AppException(message, cause, errorId)
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
        // El id queda en el log y en el campo errorId. Antes se anteponía al mensaje
        // ("[ERR-C94C0FD4] El servidor está tardando..."), que es el texto que ven las
        // pantallas: además de ruido, sus 8 dígitos hex podían contener "502" y hacer
        // que AppExceptionMapper clasificara mal el error.
        val errorId = log(feature, operation, throwable, metadata)
        val wrapped = throwable.toAppException(defaultMessage)
        val message = wrapped.message ?: defaultMessage
        return when (wrapped) {
            is AppException.Network -> AppException.Network(message, wrapped.cause, errorId)
            is AppException.Api -> AppException.Api(message, wrapped.cause, errorId)
            is AppException.Validation -> AppException.Validation(message, wrapped.cause, errorId)
            is AppException.Authentication -> AppException.Authentication(message, wrapped.cause, errorId)
            is AppException.Unknown -> AppException.Unknown(message, wrapped.cause, errorId)
        }
    }
}

fun Throwable.toAppException(defaultMessage: String): AppException {
    // Los fallos de red se clasifican por tipo (más confiable que matchear el texto) y
    // se traducen a un mensaje para el usuario: antes llegaba a la pantalla el crudo
    // de la excepción, del estilo "Unable to resolve host api.vaia...".
    when (this) {
        is java.net.UnknownHostException, is java.net.ConnectException ->
            return AppException.Network("Sin conexión a internet. Verificá tu conexión e intentá de nuevo.", this)

        is java.net.SocketTimeoutException ->
            return AppException.Network("El servidor está tardando mucho en responder. Intentá de nuevo más tarde.", this)

        // El mensaje lleva el código (502/503/504) para que AppExceptionMapper lo reconozca y la
        // pantalla muestre el aviso de servidor ocupado en vez de un error genérico.
        is com.vaia.data.network.ServerUnavailableException ->
            return AppException.Api(message ?: defaultMessage, this)
    }

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
