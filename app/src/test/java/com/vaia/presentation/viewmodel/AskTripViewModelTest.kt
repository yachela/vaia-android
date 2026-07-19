package com.vaia.presentation.viewmodel

import com.vaia.domain.model.TripInsight
import com.vaia.domain.model.TripQuestion
import com.vaia.domain.model.UnavailableReason
import com.vaia.domain.usecase.ExpenseRecord
import com.vaia.domain.repository.AskTripRepository
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

    private class FakeAsk(
        private val result: TripInsight = TripInsight.Generated("Respuesta del modelo.")
    ) : AskTripRepository {
        var calls = 0
        var lastQuestion: TripQuestion? = null

        override suspend fun ask(tripId: String, question: TripQuestion): TripInsight {
            calls++
            lastQuestion = question
            return result
        }
    }

    private fun viewModel(
        snapshot: TripSnapshot? = this.snapshot,
        ask: AskTripRepository = FakeAsk()
    ) = AskTripViewModel(FakeInsights(snapshot), ask)

    @Test
    fun `sin el viaje en cache avisa que no hay datos`() = runTest {
        val vm = viewModel(snapshot = null)

        vm.load("trip-1")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.hasNoData)
        assertTrue(vm.uiState.value.available.isEmpty())
    }

    @Test
    fun `ofrece las preguntas que el viaje puede contestar`() = runTest {
        val vm = viewModel()

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
        val vm = viewModel()
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
        val vm = viewModel()
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

    // ---------- preguntas que van al modelo ----------

    @Test
    fun `una pregunta con IA va al repositorio y no al calculo local`() = runTest {
        val ask = FakeAsk()
        val vm = viewModel(ask = ask)
        vm.load("trip-1")
        advanceUntilIdle()

        vm.ask(TripQuestion.DOCUMENTATION)
        advanceUntilIdle()

        assertEquals(1, ask.calls)
        assertEquals(TripQuestion.DOCUMENTATION, ask.lastQuestion)
        assertEquals(
            "Respuesta del modelo.",
            (vm.uiState.value.turns.single().insight as TripInsight.Generated).answer
        )
    }

    @Test
    fun `una pregunta calculada no llama al backend`() = runTest {
        val ask = FakeAsk()
        val vm = viewModel(ask = ask)
        vm.load("trip-1")
        advanceUntilIdle()

        vm.ask(TripQuestion.TOTAL_SPENT)
        advanceUntilIdle()

        assertEquals(0, ask.calls)
    }

    @Test
    fun `sin conexion la pregunta con IA responde con el motivo`() = runTest {
        val ask = FakeAsk(TripInsight.Unavailable(UnavailableReason.OFFLINE))
        val vm = viewModel(ask = ask)
        vm.load("trip-1")
        advanceUntilIdle()

        vm.ask(TripQuestion.DOCUMENTATION)
        advanceUntilIdle()

        val insight = vm.uiState.value.turns.single().insight
        assertEquals(UnavailableReason.OFFLINE, (insight as TripInsight.Unavailable).reason)
    }

    @Test
    fun `la pregunta con IA no aplica el delay cosmetico`() = runTest {
        val vm = viewModel()
        vm.load("trip-1")
        advanceUntilIdle()

        vm.ask(TripQuestion.DOCUMENTATION)
        runCurrent()

        // El fake responde al instante: si se aplicara el delay de las calculadas,
        // acá el turno seguiría pendiente.
        assertNotNull(vm.uiState.value.turns.single().insight)
    }

    @Test
    fun `todas las preguntas con IA son sobre el destino`() = runTest {
        val vm = viewModel()
        vm.load("trip-1")
        advanceUntilIdle()

        // Si alguna se pudiera contestar con los datos del viaje, no debería
        // estar acá: eso ya lo cubren las calculadas y las features existentes.
        assertEquals(
            listOf(
                TripQuestion.DOCUMENTATION,
                TripQuestion.DAILY_COST,
                TripQuestion.LOCAL_TRANSPORT,
                TripQuestion.LOCAL_TIPS
            ),
            vm.uiState.value.available.filter { it.needsAi }
        )
    }

    @Test
    fun `las preguntas del destino no dependen de los datos cargados`() = runTest {
        // Un viaje sin gastos, sin actividades y sin presupuesto igual puede
        // preguntar por el destino: no salen de sus datos.
        val vacio = snapshot.copy(budget = 0.0, expenses = emptyList())
        val vm = viewModel(snapshot = vacio)
        vm.load("trip-1")
        advanceUntilIdle()

        assertEquals(4, vm.uiState.value.available.count { it.needsAi })
    }

    @Test
    fun `con el viaje terminado no se ofrece ninguna pregunta con IA`() = runTest {
        val terminado = snapshot.copy(
            startDate = LocalDate.of(2020, 1, 1),
            endDate = LocalDate.of(2020, 1, 5)
        )
        val vm = viewModel(snapshot = terminado)
        vm.load("trip-1")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.available.none { it.needsAi })
    }

    @Test
    fun `limpiar borra la conversacion pero deja las preguntas disponibles`() = runTest {
        val vm = viewModel()
        vm.load("trip-1")
        advanceUntilIdle()
        vm.ask(TripQuestion.TOTAL_SPENT)
        advanceUntilIdle()

        vm.clear()

        assertTrue(vm.uiState.value.turns.isEmpty())
        assertTrue(vm.uiState.value.available.isNotEmpty())
    }
}
