package com.vaia.presentation.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class AppExceptionMapperTest {

    @Test
    fun `un 502 se clasifica como servidor ocupado`() {
        val result = AppExceptionMapper.fromMessage("El servidor no está disponible (502)")

        assertEquals(AppExceptionType.SERVER_BUSY, result.type)
    }

    @Test
    fun `el html crudo de nginx tambien se reconoce`() {
        val result = AppExceptionMapper.fromMessage(
            "No se pudo cargar el checklist: <html><head><title>502 Bad Gateway</title></head></html>"
        )

        assertEquals(AppExceptionType.SERVER_BUSY, result.type)
    }

    @Test
    fun `503 y 504 comparten el mismo trato`() {
        assertEquals(
            AppExceptionType.SERVER_BUSY,
            AppExceptionMapper.fromMessage("El servidor no está disponible (503)").type
        )
        assertEquals(
            AppExceptionType.SERVER_BUSY,
            AppExceptionMapper.fromMessage("El servidor no está disponible (504)").type
        )
    }

    @Test
    fun `un 500 sigue siendo error de servidor comun`() {
        val result = AppExceptionMapper.fromMessage("Error 500 en el servidor")

        assertEquals(AppExceptionType.SERVER, result.type)
    }

    @Test
    fun `la clasificacion previa no se ve afectada`() {
        assertEquals(
            AppExceptionType.AUTH,
            AppExceptionMapper.fromMessage("Unauthorized: token inválido").type
        )
        assertEquals(
            AppExceptionType.NETWORK,
            AppExceptionMapper.fromMessage("Unable to resolve host api.vaia.app").type
        )
    }
}
