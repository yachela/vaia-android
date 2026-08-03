package com.vaia.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.vaia.data.network.ServerUnavailableException
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ErrorHandlingTest {

    @Test
    fun `unknown host se traduce a mensaje de sin conexion`() {
        val e = UnknownHostException("Unable to resolve host \"api.vaia.app\": No address associated")

        val result = e.toAppException("fallback")

        assertTrue(result is AppException.Network)
        assertEquals("Sin conexión a internet. Verificá tu conexión e intentá de nuevo.", result.message)
    }

    @Test
    fun `connect exception se traduce a mensaje de sin conexion`() {
        val e = ConnectException("Failed to connect to /10.0.2.2:8000")

        val result = e.toAppException("fallback")

        assertTrue(result is AppException.Network)
        assertEquals("Sin conexión a internet. Verificá tu conexión e intentá de nuevo.", result.message)
    }

    @Test
    fun `timeout tiene su propio mensaje`() {
        val result = SocketTimeoutException("timeout").toAppException("fallback")

        assertTrue(result is AppException.Network)
        assertEquals("El servidor está tardando mucho en responder. Intentá de nuevo más tarde.", result.message)
    }

    @Test
    fun `el mensaje crudo de red nunca llega al usuario`() {
        val e = UnknownHostException("Unable to resolve host \"api.vaia.app\"")

        val message = e.toAppException("fallback").message ?: ""

        assertTrue(!message.contains("Unable to resolve host"))
    }

    @Test
    fun `errores no de red conservan su clasificacion previa`() {
        assertTrue(Exception("401 unauthorized").toAppException("x") is AppException.Authentication)
        assertTrue(Exception("422 campo obligatorio").toAppException("x") is AppException.Validation)
        assertTrue(Exception("algo raro").toAppException("x") is AppException.Unknown)
    }

    @Test
    fun `logAndWrap conserva el mensaje traducido y guarda el id de error aparte`() {
        val wrapped = ErrorLogger.logAndWrap(
            feature = "Test",
            operation = "op",
            throwable = ConnectException("Failed to connect"),
            defaultMessage = "fallback"
        )

        assertTrue(wrapped is AppException.Network)
        assertTrue(wrapped.message!!.contains("Sin conexión a internet"))
        assertTrue(wrapped.errorId!!.startsWith("ERR-"))
    }

    @Test
    fun `el id de error nunca llega al texto que ve el usuario`() {
        val wrapped = ErrorLogger.logAndWrap(
            feature = "trips",
            operation = "getTrips",
            throwable = SocketTimeoutException("timeout"),
            defaultMessage = "No se pudieron obtener los viajes"
        )

        assertEquals(
            "El servidor está tardando mucho en responder. Intentá de nuevo más tarde.",
            wrapped.message
        )
    }

    @Test
    fun `server unavailable conserva el codigo para que la UI lo reconozca`() {
        val e = ServerUnavailableException(502)

        val result = e.toAppException("fallback")

        assertTrue(result is AppException.Api)
        assertTrue(result.message!!.contains("502"))
    }
}
