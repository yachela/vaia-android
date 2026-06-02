package com.vaia.presentation.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class VoiceInputParserTest {

    private val parser = VoiceInputParser()

    @Test
    fun `parseVoiceText extracts destination and couple trip type`() {
        val voiceInput = "Quiero planificar un viaje en pareja a París con presupuesto de 1500 dólares"
        val result = parser.parseVoiceText(voiceInput)

        assertNotNull(result)
        assertEquals("París", result.destination)
        assertEquals("pareja", result.tripType)
        assertEquals(1500.0, result.budget ?: 0.0, 0.01)
        assertEquals("Escapada a París", result.title)
    }

    @Test
    fun `parseVoiceText extracts friends trip type and budget`() {
        val voiceInput = "Viaje con amigos a Londres de 800 usd"
        val result = parser.parseVoiceText(voiceInput)

        assertNotNull(result)
        assertEquals("Londres", result.destination)
        assertEquals("amigos", result.tripType)
        assertEquals(800.0, result.budget ?: 0.0, 0.01)
    }

    @Test
    fun `parseVoiceText parses specific date`() {
        val voiceInput = "Viaje a Roma el 12 de octubre en pareja"
        val result = parser.parseVoiceText(voiceInput)
        
        assertNotNull(result)
        assertEquals("Roma", result.destination)
        assertEquals("pareja", result.tripType)
        
        val today = LocalDate.now()
        var expectedStartDate = LocalDate.of(today.year, 10, 12)
        if (expectedStartDate.isBefore(today)) {
            expectedStartDate = expectedStartDate.plusYears(1)
        }
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        assertEquals(expectedStartDate.format(formatter), result.startDate)
    }
}
