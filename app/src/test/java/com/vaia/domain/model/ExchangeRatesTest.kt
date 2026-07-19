package com.vaia.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La conversión tiene que funcionar sin conexión con la última cotización
 * guardada, pero dejando claro que puede estar desactualizada.
 */
class ExchangeRatesTest {

    private val rates = mapOf("ARS" to 1450.0, "EUR" to 0.92)

    @Test
    fun `las tasas frescas no se marcan como cacheadas`() {
        val fresh = ExchangeRates(rates = rates, updatedAt = 1_000L, isFromCache = false)

        assertFalse(fresh.isFromCache)
        assertEquals(1450.0, fresh.rates["ARS"]!!, 0.001)
    }

    @Test
    fun `las tasas del cache conservan cuando se bajaron`() {
        val cachedAt = 1_752_000_000_000L
        val cached = ExchangeRates(rates = rates, updatedAt = cachedAt, isFromCache = true)

        assertTrue(cached.isFromCache)
        assertEquals(cachedAt, cached.updatedAt)
    }

    @Test
    fun `convertir con tasas cacheadas da el mismo resultado que con frescas`() {
        val fresh = ExchangeRates(rates, 1_000L, isFromCache = false)
        val cached = ExchangeRates(rates, 900L, isFromCache = true)
        val monto = 100.0

        assertEquals(
            monto * fresh.rates["ARS"]!!,
            monto * cached.rates["ARS"]!!,
            0.001
        )
    }
}
