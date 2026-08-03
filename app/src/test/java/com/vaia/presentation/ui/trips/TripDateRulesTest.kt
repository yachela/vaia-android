package com.vaia.presentation.ui.trips

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TripDateRulesTest {

    // ── adjustEndDate ────────────────────────────────────────────────────────

    @Test
    fun `sin fecha de fin la lleva al dia siguiente del inicio`() {
        assertEquals("2026-06-02", adjustEndDate("2026-06-01", ""))
    }

    @Test
    fun `una fecha de fin anterior al inicio se corrige`() {
        assertEquals("2026-06-02", adjustEndDate("2026-06-01", "2026-05-20"))
    }

    @Test
    fun `una fecha de fin igual al inicio se corrige porque el backend la rechaza`() {
        assertEquals("2026-06-02", adjustEndDate("2026-06-01", "2026-06-01"))
    }

    @Test
    fun `una fecha de fin ya posterior no se toca`() {
        assertEquals("2026-06-20", adjustEndDate("2026-06-01", "2026-06-20"))
    }

    @Test
    fun `corrige cruzando fin de mes y fin de anio`() {
        assertEquals("2026-07-01", adjustEndDate("2026-06-30", ""))
        assertEquals("2027-01-01", adjustEndDate("2026-12-31", ""))
    }

    @Test
    fun `corrige en anio bisiesto`() {
        assertEquals("2028-02-29", adjustEndDate("2028-02-28", ""))
    }

    @Test
    fun `un inicio ilegible deja la fecha de fin como estaba`() {
        assertEquals("2026-06-20", adjustEndDate("no es una fecha", "2026-06-20"))
    }

    // ── minEndDate ───────────────────────────────────────────────────────────

    @Test
    fun `la fecha minima de fin es el dia siguiente al inicio`() {
        assertEquals("2026-06-02", minEndDate("2026-06-01"))
    }

    @Test
    fun `sin inicio no hay minimo en vez de romper`() {
        assertNull(minEndDate(""))
        assertNull(minEndDate("cualquier cosa"))
    }

    // ── validateTripDateRange ────────────────────────────────────────────────

    private val today = LocalDate.of(2026, 6, 1)

    @Test
    fun `un rango posterior a hoy es valido`() {
        assertNull(validateTripDateRange("2026-06-10", "2026-06-20", today, requireFutureStart = true))
    }

    @Test
    fun `fin igual a inicio es invalido igual que en el backend`() {
        assertEquals(
            TripDateError.END_NOT_AFTER_START,
            validateTripDateRange("2026-06-10", "2026-06-10", today, requireFutureStart = true)
        )
    }

    @Test
    fun `fin anterior a inicio es invalido`() {
        assertEquals(
            TripDateError.END_NOT_AFTER_START,
            validateTripDateRange("2026-06-10", "2026-06-05", today, requireFutureStart = true)
        )
    }

    @Test
    fun `un viaje nuevo no puede empezar hoy`() {
        assertEquals(
            TripDateError.START_NOT_AFTER_TODAY,
            validateTripDateRange("2026-06-01", "2026-06-05", today, requireFutureStart = true)
        )
    }

    @Test
    fun `al editar se acepta un viaje que ya empezo`() {
        assertNull(
            validateTripDateRange("2026-05-01", "2026-06-05", today, requireFutureStart = false)
        )
    }

    @Test
    fun `al editar el orden de las fechas se sigue validando`() {
        assertEquals(
            TripDateError.END_NOT_AFTER_START,
            validateTripDateRange("2026-05-01", "2026-04-01", today, requireFutureStart = false)
        )
    }

    @Test
    fun `fechas incompletas no se marcan como error de rango`() {
        assertNull(validateTripDateRange("", "2026-06-05", today, requireFutureStart = true))
        assertNull(validateTripDateRange("2026-06-05", "", today, requireFutureStart = true))
    }
}
