package com.vaia.domain.model

import com.vaia.presentation.ui.common.formatMoney
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrenciesTest {

    @Test
    fun `normalize acepta minusculas y espacios de codigos validos`() {
        assertEquals("ARS", Currencies.normalize("ars"))
        assertEquals("EUR", Currencies.normalize("EUR"))
    }

    @Test
    fun `normalize cae al default con codigos nulos o desconocidos`() {
        assertEquals(Currencies.DEFAULT_CODE, Currencies.normalize(null))
        assertEquals(Currencies.DEFAULT_CODE, Currencies.normalize(""))
        assertEquals(Currencies.DEFAULT_CODE, Currencies.normalize("XYZ"))
    }

    @Test
    fun `el catalogo no tiene codigos repetidos y trae el default`() {
        val codes = Currencies.all.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        assertNotNull(Currencies.optionOf(Currencies.DEFAULT_CODE))
    }

    @Test
    fun `cada moneda tiene simbolo propio`() {
        assertTrue(Currencies.all.all { it.symbol.isNotBlank() })
        assertEquals("$", Currencies.symbolOf("ARS"))
        assertEquals("US$", Currencies.symbolOf("USD"))
        assertEquals("€", Currencies.symbolOf("EUR"))
    }

    @Test
    fun `formatMoney usa el simbolo del viaje y no asume dolares`() {
        assertEquals("$1.250", formatMoney(1250.0, "ARS"))
        assertEquals("US$1.250", formatMoney(1250.0, "USD"))
        assertEquals("€1.250,55", formatMoney(1250.55, "EUR"))
    }

    @Test
    fun `formatMoney tolera una moneda sin definir`() {
        assertEquals("US$100", formatMoney(100.0, null))
    }

    // ── suggestedFor ─────────────────────────────────────────────────────────

    @Test
    fun `suggestedFor abre con la moneda del viaje y sigue con la del perfil`() {
        val codes = Currencies.suggestedFor(profileCode = "ARS", currentCode = "JPY")
            .map { it.code }
        assertEquals("JPY", codes[0])
        assertEquals("ARS", codes[1])
    }

    @Test
    fun `suggestedFor no repite cuando el perfil coincide con la moneda del viaje`() {
        val codes = Currencies.suggestedFor(profileCode = "ars", currentCode = "ARS")
            .map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        assertEquals("ARS", codes.first())
    }

    @Test
    fun `suggestedFor siempre ofrece algo aunque no haya perfil ni viaje`() {
        val codes = Currencies.suggestedFor(profileCode = null, currentCode = null)
            .map { it.code }
        assertTrue(codes.isNotEmpty())
        assertTrue(codes.contains(Currencies.DEFAULT_CODE))
    }

    @Test
    fun `suggestedFor ignora codigos desconocidos en vez de romper`() {
        val codes = Currencies.suggestedFor(profileCode = "XYZ", currentCode = "???")
            .map { it.code }
        assertTrue(codes.none { it == "XYZ" || it == "???" })
        assertTrue(codes.contains(Currencies.DEFAULT_CODE))
    }

    @Test
    fun `suggestedFor es corta para que no haya que scrollear`() {
        val codes = Currencies.suggestedFor(profileCode = "JPY", currentCode = "THB")
        assertTrue(codes.size <= 6)
    }

    @Test
    fun `currencyCode de un viaje sin moneda guardada cae al default`() {
        val trip = Trip(
            id = "t1",
            title = "Viaje",
            destination = "Bariloche",
            startDate = "2026-04-10",
            endDate = "2026-04-14",
            budget = 1000.0
        )
        assertEquals(Currencies.DEFAULT_CODE, trip.currencyCode)
        assertEquals("ARS", trip.copy(currency = "ars").currencyCode)
    }
}
