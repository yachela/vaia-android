package com.vaia.presentation.viewmodel

import com.vaia.domain.model.TripInsight
import com.vaia.domain.model.TripQuestion
import com.vaia.domain.usecase.ExpenseRecord
import com.vaia.domain.usecase.TripInsightsProvider
import com.vaia.domain.usecase.TripSnapshot
import com.vaia.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AskTripViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val snapshot = TripSnapshot(
        startDate = LocalDate.of(2026, 8, 10),
        endDate = LocalDate.of(2026, 8, 15),
        budget = 1000.0,
        activities = emptyList(),
        expenses = listOf(ExpenseRecord(250.0, "Comida")),
        packingItems = emptyList()
    )

    /** Sustituye la carga desde Room; el cálculo se prueba aparte, sin dependencias. */
    private class FakeInsights(private val result: TripSnapshot?) : TripInsightsProvider {
        override suspend fun snapshotOf(tripId: String): TripSnapshot? = result
    }

    @Test
    fun `sin el viaje en cache avisa que no hay datos`() = runTest {
        val vm = AskTripViewModel(FakeInsights(null))

        vm.load("trip-1")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.hasNoData)
        assertTrue(vm.uiState.value.available.isEmpty())
    }

    @Test
    fun `ofrece las preguntas que el viaje puede contestar`() = runTest {
        val vm = AskTripViewModel(FakeInsights(snapshot))

        vm.load("trip-1")
        advanceUntilIdle()

        val available = vm.uiState.value.available
        assertTrue(available.contains(TripQuestion.TOTAL_SPENT))
        assertTrue(available.contains(TripQuestion.DAYS_UNTIL_TRIP))
        // Sin ítems de equipaje cargados, esa pregunta no se ofrece.
        assertTrue(!available.contains(TripQuestion.PENDING_PACKING))
    }

    @Test
    fun `la respuesta aparece primero como pendiente y despues resuelta`() = runTest {
        val vm = AskTripViewModel(FakeInsights(snapshot))
        vm.load("trip-1")
        advanceUntilIdle()

        vm.ask(TripQuestion.TOTAL_SPENT)
        runCurrent()

        // El turno ya está en pantalla con el indicador, todavía sin respuesta.
        assertEquals(1, vm.uiState.value.turns.size)
        assertNull(vm.uiState.value.turns.single().insight)

        advanceTimeBy(AskTripViewModel.ANSWER_REVEAL_DELAY_MS + 1)
        runCurrent()

        val turn = vm.uiState.value.turns.single()
        assertNotNull(turn.insight)
        assertEquals(250.0, (turn.insight as TripInsight.TotalSpent).total, 0.001)
    }

    @Test
    fun `preguntar dos veces lo mismo resuelve los dos turnos`() = runTest {
        val vm = AskTripViewModel(FakeInsights(snapshot))
        vm.load("trip-1")
        advanceUntilIdle()

        // Dos turnos idénticos y dos corrutinas en vuelo a la vez: ninguno tiene
        // que quedarse colgado en el indicador.
        vm.ask(TripQuestion.TOTAL_SPENT)
        vm.ask(TripQuestion.TOTAL_SPENT)
        advanceUntilIdle()

        val turns = vm.uiState.value.turns
        assertEquals(2, turns.size)
        assertTrue(turns.all { it.insight != null })
    }

    @Test
    fun `limpiar borra la conversacion pero deja las preguntas disponibles`() = runTest {
        val vm = AskTripViewModel(FakeInsights(snapshot))
        vm.load("trip-1")
        advanceUntilIdle()
        vm.ask(TripQuestion.TOTAL_SPENT)
        advanceUntilIdle()

        vm.clear()

        assertTrue(vm.uiState.value.turns.isEmpty())
        assertTrue(vm.uiState.value.available.isNotEmpty())
    }
}
