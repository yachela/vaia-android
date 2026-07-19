package com.vaia.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * El backend devuelve fechas en distintos formatos según el endpoint. Si el
 * parseo falla, la pregunta contesta cualquier cosa en vez de fallar visible,
 * así que conviene cubrir cada formato que llega.
 */
class TripDateParsingTest {

    @Test
    fun `parsea los formatos de fecha que devuelve el backend`() {
        val esperado = LocalDate.of(2026, 8, 10)

        assertEquals(esperado, GetTripInsightsUseCase.parseDate("2026-08-10"))
        assertEquals(esperado, GetTripInsightsUseCase.parseDate("2026-08-10T00:00:00Z"))
        assertEquals(esperado, GetTripInsightsUseCase.parseDate("2026-08-10T00:00:00.000Z"))
        assertEquals(esperado, GetTripInsightsUseCase.parseDate("2026-08-10 12:30:00"))
    }

    @Test
    fun `una fecha vacia o invalida devuelve null en vez de romper`() {
        assertNull(GetTripInsightsUseCase.parseDate(null))
        assertNull(GetTripInsightsUseCase.parseDate(""))
        assertNull(GetTripInsightsUseCase.parseDate("   "))
        assertNull(GetTripInsightsUseCase.parseDate("mañana"))
        assertNull(GetTripInsightsUseCase.parseDate("10/08/2026"))
    }

    @Test
    fun `normaliza la hora a HH mm`() {
        assertEquals("09:30", GetTripInsightsUseCase.normalizeTime("9:30"))
        assertEquals("09:30", GetTripInsightsUseCase.normalizeTime("09:30"))
        assertEquals("14:00", GetTripInsightsUseCase.normalizeTime("14:00:00"))
    }

    @Test
    fun `una hora vacia o sin formato devuelve null`() {
        assertNull(GetTripInsightsUseCase.normalizeTime(null))
        assertNull(GetTripInsightsUseCase.normalizeTime(""))
        assertNull(GetTripInsightsUseCase.normalizeTime("por la tarde"))
    }
}
