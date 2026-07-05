package com.vaia.data.network

import com.vaia.domain.model.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ErrorMapperTest {

    // ── fromHttpError ─────────────────────────────────────────────────────────

    @Test
    fun `422 con errores de campos mapea a Validation con el primer mensaje`() {
        val body = """
            {
                "message": "Los datos son inválidos.",
                "errors": {
                    "email": ["El correo ya está registrado.", "Formato inválido."],
                    "password": ["La contraseña es obligatoria."]
                }
            }
        """.trimIndent()

        val error = ErrorMapper.fromHttpError(422, body, "fallback")

        assertTrue(error is AppError.Validation)
        error as AppError.Validation
        assertEquals("El correo ya está registrado.", error.message)
        assertEquals(
            listOf("El correo ya está registrado.", "Formato inválido."),
            error.fieldErrors["email"]
        )
        assertEquals(listOf("La contraseña es obligatoria."), error.fieldErrors["password"])
    }

    @Test
    fun `422 sin mapa de errores usa el message del body`() {
        val body = """{"message": "Datos inválidos."}"""

        val error = ErrorMapper.fromHttpError(422, body, "fallback")

        assertTrue(error is AppError.Validation)
        assertEquals("Datos inválidos.", error.message)
    }

    @Test
    fun `401 mapea a Unauthorized`() {
        val error = ErrorMapper.fromHttpError(401, """{"message":"Unauthenticated."}""", "fallback")

        assertTrue(error is AppError.Unauthorized)
    }

    @Test
    fun `500 mapea a Unknown con el message del body`() {
        val error = ErrorMapper.fromHttpError(500, """{"message":"Error interno."}""", "fallback")

        assertTrue(error is AppError.Unknown)
        assertEquals("Error interno.", error.message)
    }

    @Test
    fun `500 sin body usa el mensaje por defecto`() {
        val error = ErrorMapper.fromHttpError(500, null, "No se pudo completar la operación")

        assertTrue(error is AppError.Unknown)
        assertEquals("No se pudo completar la operación", error.message)
    }

    @Test
    fun `body malformado no rompe y usa el mensaje por defecto`() {
        val error = ErrorMapper.fromHttpError(422, "<html>not json</html>", "fallback")

        assertTrue(error is AppError.Validation)
        assertEquals("fallback", error.message)
    }

    // ── fromThrowable ─────────────────────────────────────────────────────────

    @Test
    fun `IOException mapea a Network`() {
        val error = ErrorMapper.fromThrowable(IOException("timeout"))

        assertTrue(error is AppError.Network)
    }

    @Test
    fun `un AppError existente se devuelve sin envolver`() {
        val original = AppError.Unauthorized()

        val error = ErrorMapper.fromThrowable(original)

        assertEquals(original, error)
    }

    @Test
    fun `una excepción cualquiera mapea a Unknown con su mensaje`() {
        val error = ErrorMapper.fromThrowable(IllegalStateException("algo salió mal"))

        assertTrue(error is AppError.Unknown)
        assertEquals("algo salió mal", error.message)
    }

    @Test
    fun `una excepción sin mensaje usa el mensaje por defecto`() {
        val error = ErrorMapper.fromThrowable(RuntimeException(), "mensaje por defecto")

        assertTrue(error is AppError.Unknown)
        assertEquals("mensaje por defecto", error.message)
    }
}
