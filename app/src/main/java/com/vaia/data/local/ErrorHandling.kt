package com.vaia.data.local

import android.util.Log
import com.vaia.data.network.ErrorMapper
import com.vaia.domain.model.AppError
import java.util.UUID

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

    /**
     * Registra la excepción y la envuelve en un [AppError] tipado con un id
     * de error rastreable. La clasificación la hace [ErrorMapper] por tipo
     * de excepción, sin comparar strings.
     */
    fun logAndWrap(
        feature: String,
        operation: String,
        throwable: Throwable,
        defaultMessage: String,
        metadata: Map<String, Any?> = emptyMap()
    ): AppError {
        val errorId = log(feature, operation, throwable, metadata)
        val wrapped = ErrorMapper.fromThrowable(throwable, defaultMessage)
        val withIdMessage = "[$errorId] ${wrapped.message ?: defaultMessage}"
        return when (wrapped) {
            is AppError.Network -> AppError.Network(withIdMessage, wrapped.cause)
            is AppError.Validation -> AppError.Validation(withIdMessage, wrapped.fieldErrors)
            is AppError.Unauthorized -> AppError.Unauthorized(withIdMessage)
            is AppError.Unknown -> AppError.Unknown(withIdMessage, wrapped.cause)
        }
    }
}
