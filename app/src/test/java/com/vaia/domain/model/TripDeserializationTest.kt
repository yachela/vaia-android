package com.vaia.domain.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gson construye los objetos por reflexión y **no aplica los valores por defecto de
 * Kotlin**: un campo que no viene en el JSON queda `null` aunque el tipo sea no-nulo.
 *
 * Al editar un viaje, la respuesta del backend no trae `activities`, `expenses` ni
 * `documents`, y el `copy()` que completa la moneda reventaba con
 * "Parameter specified as non-null is null: parameter activities".
 */
class TripDeserializationTest {

    /** Respuesta real de `PUT /api/trips/{id}`: sin colecciones ni contadores. */
    private val updateResponseJson = """
        {
          "id": "019fc3cf-23b7-71b8-9bb7-5d450fbe5b65",
          "title": "Japon",
          "destination": "Japón, China",
          "start_date": "2026-08-08T00:00:00.000000Z",
          "end_date": "2026-08-29T00:00:00.000000Z",
          "budget": "10000.00",
          "total_expenses": 0,
          "created_at": "2026-08-02T18:49:20.000000Z",
          "updated_at": "2026-08-02T18:49:20.000000Z"
        }
    """.trimIndent()

    @Test
    fun `copiar un viaje recien deserializado no revienta`() {
        val trip = Gson().fromJson(updateResponseJson, Trip::class.java)

        // Es lo que hace TripRepositoryImpl.withCurrency al completar la moneda.
        val copied = trip.copy(currency = "ARS")

        assertEquals("ARS", copied.currency)
        assertEquals("Japon", copied.title)
    }

    @Test
    fun `las colecciones ausentes se leen como listas vacias`() {
        val trip = Gson().fromJson(updateResponseJson, Trip::class.java)

        assertTrue(trip.activityList.isEmpty())
        assertTrue(trip.expenseList.isEmpty())
        assertTrue(trip.documentList.isEmpty())
    }

    @Test
    fun `cuando el backend si manda las colecciones se conservan`() {
        val json = """
            {
              "id": "t1",
              "title": "Japón",
              "destination": "Japón",
              "start_date": "2026-08-08",
              "end_date": "2026-08-29",
              "budget": 1000.0,
              "activities": [
                {
                  "id": "a1",
                  "title": "Templo",
                  "description": "",
                  "date": "2026-08-09",
                  "location": "Kioto",
                  "cost": 0.0
                }
              ]
            }
        """.trimIndent()

        val trip = Gson().fromJson(json, Trip::class.java)

        assertEquals(1, trip.activityList.size)
        assertEquals("Templo", trip.activityList.first().title)
    }
}
