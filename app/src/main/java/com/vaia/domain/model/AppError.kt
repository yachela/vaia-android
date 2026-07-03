package com.vaia.domain.model

/**
 * Jerarquía sellada de errores de la aplicación.
 *
 * Toda falla que atraviese la capa data llega al dominio y a la presentación
 * como un AppError tipado; la clasificación ocurre en un único punto de la
 * capa data (ErrorMapper), nunca comparando strings.
 */
sealed class AppError(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /** Fallas de conectividad: sin red, timeouts, host inalcanzable. */
    class Network(
        message: String = "No hay conexión a internet. Verifica tu red e intenta de nuevo.",
        cause: Throwable? = null
    ) : AppError(message, cause)

    /** Errores de validación (HTTP 422) con los mensajes por campo del backend. */
    class Validation(
        message: String,
        val fieldErrors: Map<String, List<String>> = emptyMap()
    ) : AppError(message)

    /** Sesión inválida o expirada (HTTP 401). */
    class Unauthorized(
        message: String = "Tu sesión expiró. Inicia sesión nuevamente."
    ) : AppError(message)

    /** Cualquier otra falla (errores de servidor, respuestas inesperadas, bugs). */
    class Unknown(
        message: String = "Ocurrió un error inesperado.",
        cause: Throwable? = null
    ) : AppError(message, cause)
}
