package com.vaia.data.repository

import com.vaia.domain.model.ActivitySuggestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionValidatorTest {

    @Test
    fun `valid suggestion passes through unchanged`() {
        val input = listOf(ActivitySuggestion("Coliseo", "Tour guiado", "Roma", 18.0, "09:00"))
        assertEquals(input, SuggestionValidator.sanitize(input))
    }

    @Test
    fun `drops suggestions with blank title or location`() {
        val input = listOf(
            ActivitySuggestion("", "desc", "Roma", 10.0, "09:00"),
            ActivitySuggestion("   ", "desc", "Roma", 10.0, "09:00"),
            ActivitySuggestion("Coliseo", "desc", "", 10.0, "09:00"),
            ActivitySuggestion("Válida", "desc", "Roma", 10.0, "09:00")
        )
        val result = SuggestionValidator.sanitize(input)
        assertEquals(1, result.size)
        assertEquals("Válida", result[0].title)
    }

    @Test
    fun `truncates overly long fields`() {
        val longTitle = "x".repeat(300)
        val result = SuggestionValidator.sanitize(
            listOf(ActivitySuggestion(longTitle, "d".repeat(500), "Roma", 5.0, "09:00"))
        )
        assertEquals(100, result[0].title.length)
        assertEquals(200, result[0].description.length)
    }

    @Test
    fun `negative cost is clamped to zero`() {
        val result = SuggestionValidator.sanitize(
            listOf(ActivitySuggestion("Paseo", "desc", "Roma", -5.0, "09:00"))
        )
        assertEquals(0.0, result[0].cost, 0.001)
    }

    @Test
    fun `invalid time format becomes empty`() {
        val result = SuggestionValidator.sanitize(
            listOf(
                ActivitySuggestion("A", "d", "Roma", 0.0, "9am"),
                ActivitySuggestion("B", "d", "Roma", 0.0, "25:99"),
                ActivitySuggestion("C", "d", "Roma", 0.0, "14:30")
            )
        )
        assertEquals("", result[0].time)
        assertEquals("", result[1].time)
        assertEquals("14:30", result[2].time)
    }

    @Test
    fun `stableId is equal for same title and location ignoring case`() {
        val a = ActivitySuggestion("Coliseo", "x", "Roma", 1.0, "09:00")
        val b = ActivitySuggestion("  coliseo ", "otra desc", "ROMA", 99.0, "18:00")
        assertEquals(a.stableId, b.stableId)
        assertTrue(a.stableId != ActivitySuggestion("Vaticano", "x", "Roma", 1.0, "09:00").stableId)
    }
}
