package com.vaia.data.network

import com.google.gson.JsonParser
import com.vaia.domain.model.AppError
import retrofit2.Response
import java.io.IOException

/**
 * Punto único de mapeo de errores Retrofit/HTTP → [AppError].
 *
 * Centraliza el parseo del cuerpo de error de Laravel ({message, errors})
 * que antes estaba duplicado en cada repositorio y en ErrorInterceptor.
 */
object ErrorMapper {

    /** Mapea una respuesta HTTP no exitosa a un AppError tipado. */
    fun <T> fromResponse(response: Response<T>, fallbackMessage: String): AppError {
        val rawBody = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        return fromHttpError(response.code(), rawBody, fallbackMessage)
    }

    /** Mapea un código HTTP y su cuerpo de error a un AppError tipado. */
    fun fromHttpError(code: Int, rawBody: String?, fallbackMessage: String): AppError {
        val parsed = parseErrorBody(rawBody)
        val message = parsed?.message ?: fallbackMessage
        return when (code) {
            401 -> AppError.Unauthorized()
            422 -> AppError.Validation(message, parsed?.fieldErrors ?: emptyMap())
            else -> AppError.Unknown(message)
        }
    }

    /** Mapea una excepción a un AppError según su tipo (nunca por strings). */
    fun fromThrowable(throwable: Throwable, fallbackMessage: String? = null): AppError {
        return when (throwable) {
            is AppError -> throwable
            is IOException -> AppError.Network(cause = throwable)
            else -> AppError.Unknown(
                message = fallbackMessage ?: throwable.message ?: "Ocurrió un error inesperado.",
                cause = throwable
            )
        }
    }

    private data class ParsedError(
        val message: String?,
        val fieldErrors: Map<String, List<String>>
    )

    // Extrae message y errors del formato de error de la API:
    // { "message": "...", "errors": { "campo": ["mensaje", ...] } }
    private fun parseErrorBody(rawBody: String?): ParsedError? {
        if (rawBody.isNullOrBlank()) return null
        return try {
            val json = JsonParser.parseString(rawBody).asJsonObject
            val fieldErrors = mutableMapOf<String, List<String>>()
            json.get("errors")?.takeIf { it.isJsonObject }?.asJsonObject?.entrySet()
                ?.forEach { (key, value) ->
                    if (value.isJsonArray) {
                        fieldErrors[key] = value.asJsonArray
                            .filter { it.isJsonPrimitive }
                            .map { it.asString }
                    }
                }
            val firstFieldMessage = fieldErrors.values.firstOrNull()?.firstOrNull()
            val message = firstFieldMessage
                ?: json.get("message")?.takeIf { it.isJsonPrimitive }?.asString
                    ?.takeIf { it.isNotBlank() }
            ParsedError(message, fieldErrors)
        } catch (_: Exception) {
            null
        }
    }
}
