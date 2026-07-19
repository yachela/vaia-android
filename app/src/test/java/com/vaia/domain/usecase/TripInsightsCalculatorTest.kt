package com.vaia.domain.usecase

import com.vaia.domain.model.TripInsight
import com.vaia.domain.model.TripPhase
import com.vaia.domain.model.TripQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * El cálculo es puro: recibe los datos y la fecha de hoy, así que los casos de
 * borde (viaje terminado, sin gastos, presupuesto excedido) se cubren sin base
 * de datos ni mocks.
 */
class TripInsightsCalculatorTest {

    private val start = LocalDate.of(2026, 8, 10)
    private val end = LocalDate.of(2026, 8, 15)

    private fun snapshot(
        budget: Double = 1000.0,
        activities: List<ActivityRecord> = emptyList(),
        expenses: List<ExpenseRecord> = emptyList(),
        packing: List<PackingRecord> = emptyList(),
        startDate: LocalDate? = start,
        endDate: LocalDate? = end
    ) = TripSnapshot(startDate, endDate, budget, activities, expenses, packing)

    // ---------- fase del viaje ----------

    @Test
    fun `la fase distingue antes, durante y despues del viaje`() {
        val trip = snapshot()

        assertEquals(TripPhase.BEFORE, TripInsightsCalculator.phaseOf(trip, LocalDate.of(2026, 8, 1)))
        assertEquals(TripPhase.DURING, TripInsightsCalculator.phaseOf(trip, LocalDate.of(2026, 8, 12)))
        assertEquals(TripPhase.AFTER, TripInsightsCalculator.phaseOf(trip, LocalDate.of(2026, 9, 1)))
    }

    @Test
    fun `el primer y el ultimo dia del viaje cuentan como durante`() {
        val trip = snapshot()

        assertEquals(TripPhase.DURING, TripInsightsCalculator.phaseOf(trip, start))
        assertEquals(TripPhase.DURING, TripInsightsCalculator.phaseOf(trip, end))
    }

    // ---------- preguntas ofrecidas ----------

    @Test
    fun `sin gastos cargados no se ofrecen las preguntas de gasto`() {
        val questions = TripInsightsCalculator.availableQuestions(snapshot(), LocalDate.of(2026, 8, 1))

        assertFalse(questions.contains(TripQuestion.TOTAL_SPENT))
        assertFalse(questions.contains(TripQuestion.TOP_CATEGORY))
        // El presupuesto restante sí, porque todavía se puede gastar.
        assertTrue(questions.contains(TripQuestion.REMAINING_BUDGET))
    }

    @Test
    fun `con el viaje terminado solo quedan las preguntas de resumen de gasto`() {
        val trip = snapshot(expenses = listOf(ExpenseRecord(100.0, "Comida")))

        val questions = TripInsightsCalculator.availableQuestions(trip, LocalDate.of(2026, 9, 1))

        assertEquals(listOf(TripQuestion.TOTAL_SPENT, TripQuestion.TOP_CATEGORY), questions)
    }

    @Test
    fun `cuanto falta para el viaje solo se ofrece antes de salir`() {
        val trip = snapshot()

        assertTrue(
            TripInsightsCalculator.availableQuestions(trip, LocalDate.of(2026, 8, 1))
                .contains(TripQuestion.DAYS_UNTIL_TRIP)
        )
        assertFalse(
            TripInsightsCalculator.availableQuestions(trip, LocalDate.of(2026, 8, 12))
                .contains(TripQuestion.DAYS_UNTIL_TRIP)
        )
    }

    // ---------- cuánto falta ----------

    @Test
    fun `antes de salir cuenta los dias que faltan`() {
        val result = TripInsightsCalculator.answer(
            TripQuestion.DAYS_UNTIL_TRIP, snapshot(), LocalDate.of(2026, 8, 1)
        ) as TripInsight.DaysUntilTrip

        assertEquals(9, result.days)
        assertEquals(TripPhase.BEFORE, result.phase)
    }

    @Test
    fun `durante el viaje cuenta los dias que quedan`() {
        val result = TripInsightsCalculator.answer(
            TripQuestion.DAYS_UNTIL_TRIP, snapshot(), LocalDate.of(2026, 8, 13)
        ) as TripInsight.DaysUntilTrip

        assertEquals(2, result.days)
        assertEquals(TripPhase.DURING, result.phase)
    }

    // ---------- próximas actividades ----------

    @Test
    fun `devuelve el proximo dia con actividades, no literalmente manana`() {
        val trip = snapshot(
            activities = listOf(
                ActivityRecord("Coliseo", LocalDate.of(2026, 8, 14), "10:00", "Roma"),
                ActivityRecord("Vaticano", LocalDate.of(2026, 8, 14), "15:00", "Roma"),
                ActivityRecord("Trastevere", LocalDate.of(2026, 8, 15), "20:00", "Roma")
            )
        )

        val result = TripInsightsCalculator.answer(
            TripQuestion.NEXT_ACTIVITIES, trip, LocalDate.of(2026, 8, 11)
        ) as TripInsight.NextActivities

        // Hay tres días sin nada entre hoy y el 14: igual encuentra el próximo día útil.
        assertEquals(LocalDate.of(2026, 8, 14), result.date)
        assertEquals(2, result.activities.size)
        assertEquals("Coliseo", result.activities.first().title)
    }

    @Test
    fun `ignora las actividades que ya pasaron`() {
        val trip = snapshot(
            activities = listOf(
                ActivityRecord("Ya pasó", LocalDate.of(2026, 8, 10), "10:00", null),
                ActivityRecord("Viene", LocalDate.of(2026, 8, 14), "10:00", null)
            )
        )

        val result = TripInsightsCalculator.answer(
            TripQuestion.NEXT_ACTIVITIES, trip, LocalDate.of(2026, 8, 12)
        ) as TripInsight.NextActivities

        assertEquals(LocalDate.of(2026, 8, 14), result.date)
        assertEquals("Viene", result.activities.single().title)
    }

    @Test
    fun `sin actividades futuras devuelve fecha nula`() {
        val result = TripInsightsCalculator.answer(
            TripQuestion.NEXT_ACTIVITIES, snapshot(), LocalDate.of(2026, 8, 12)
        ) as TripInsight.NextActivities

        assertNull(result.date)
        assertTrue(result.activities.isEmpty())
    }

    // ---------- días libres ----------

    @Test
    fun `los dias libres excluyen los que ya tienen actividades`() {
        val trip = snapshot(
            activities = listOf(
                ActivityRecord("Coliseo", LocalDate.of(2026, 8, 11), null, null),
                ActivityRecord("Museo", LocalDate.of(2026, 8, 13), null, null)
            )
        )

        val result = TripInsightsCalculator.answer(
            TripQuestion.FREE_DAYS, trip, LocalDate.of(2026, 8, 1)
        ) as TripInsight.FreeDays

        assertEquals(6, result.totalDays)
        assertEquals(
            listOf(
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 12),
                LocalDate.of(2026, 8, 14),
                LocalDate.of(2026, 8, 15)
            ),
            result.dates
        )
    }

    @Test
    fun `con el viaje en curso los dias libres arrancan hoy y no antes`() {
        val result = TripInsightsCalculator.answer(
            TripQuestion.FREE_DAYS, snapshot(), LocalDate.of(2026, 8, 13)
        ) as TripInsight.FreeDays

        assertEquals(LocalDate.of(2026, 8, 13), result.dates.first())
        assertEquals(3, result.dates.size)
    }

    // ---------- hospedaje ----------

    private fun stay(name: String, date: LocalDate?, location: String? = null) =
        ActivityRecord(name, date, null, location, isAccommodation = true)

    @Test
    fun `los hospedajes no cuentan como actividades del itinerario`() {
        val trip = snapshot(
            activities = listOf(
                stay("Hotel Ibis", LocalDate.of(2026, 8, 11)),
                ActivityRecord("Coliseo", LocalDate.of(2026, 8, 14), "10:00", "Roma")
            )
        )

        val result = TripInsightsCalculator.answer(
            TripQuestion.NEXT_ACTIVITIES, trip, LocalDate.of(2026, 8, 10)
        ) as TripInsight.NextActivities

        // Sin el filtro, el hotel del 11 sería "lo próximo en el itinerario".
        assertEquals(LocalDate.of(2026, 8, 14), result.date)
        assertEquals("Coliseo", result.activities.single().title)
    }

    @Test
    fun `el dia de check-in sigue contando como dia libre`() {
        val trip = snapshot(activities = listOf(stay("Hotel Ibis", LocalDate.of(2026, 8, 11))))

        val result = TripInsightsCalculator.answer(
            TripQuestion.FREE_DAYS, trip, LocalDate.of(2026, 8, 1)
        ) as TripInsight.FreeDays

        // Dormir en algún lado no es un plan: los 6 días siguen libres.
        assertEquals(6, result.dates.size)
        assertTrue(result.dates.contains(LocalDate.of(2026, 8, 11)))
    }

    @Test
    fun `con un solo hospedaje lo devuelve sin marcar uno actual`() {
        val trip = snapshot(activities = listOf(stay("Hotel Ibis", null, "Rue de Tolbiac 15")))

        val result = TripInsightsCalculator.answer(
            TripQuestion.WHERE_I_STAY, trip, LocalDate.of(2026, 8, 1)
        ) as TripInsight.Accommodation

        assertEquals(1, result.stays.size)
        assertEquals("Hotel Ibis", result.stays.single().name)
        assertEquals("Rue de Tolbiac 15", result.stays.single().location)
        assertNull(result.current)
    }

    @Test
    fun `durante el viaje el hospedaje actual es el ultimo check-in cumplido`() {
        val trip = snapshot(
            activities = listOf(
                stay("Hotel Roma", LocalDate.of(2026, 8, 10)),
                stay("Hostel Florencia", LocalDate.of(2026, 8, 13)),
                stay("Hotel Venecia", LocalDate.of(2026, 8, 15))
            )
        )

        val result = TripInsightsCalculator.answer(
            TripQuestion.WHERE_I_STAY, trip, LocalDate.of(2026, 8, 14)
        ) as TripInsight.Accommodation

        assertEquals("Hostel Florencia", result.current?.name)
        assertEquals(3, result.stays.size)
    }

    @Test
    fun `antes de salir no hay hospedaje actual aunque haya varios`() {
        val trip = snapshot(
            activities = listOf(
                stay("Hotel Roma", LocalDate.of(2026, 8, 10)),
                stay("Hostel Florencia", LocalDate.of(2026, 8, 13))
            )
        )

        val result = TripInsightsCalculator.answer(
            TripQuestion.WHERE_I_STAY, trip, LocalDate.of(2026, 8, 1)
        ) as TripInsight.Accommodation

        assertNull(result.current)
        // Ordenados por fecha de check-in.
        assertEquals("Hotel Roma", result.stays.first().name)
    }

    @Test
    fun `la pregunta de hospedaje solo se ofrece si hay alguno cargado`() {
        val sinHospedaje = snapshot(
            activities = listOf(ActivityRecord("Coliseo", LocalDate.of(2026, 8, 14), null, null))
        )
        val conHospedaje = snapshot(activities = listOf(stay("Hotel Ibis", LocalDate.of(2026, 8, 11))))

        val hoy = LocalDate.of(2026, 8, 1)
        assertFalse(
            TripInsightsCalculator.availableQuestions(sinHospedaje, hoy)
                .contains(TripQuestion.WHERE_I_STAY)
        )
        assertTrue(
            TripInsightsCalculator.availableQuestions(conHospedaje, hoy)
                .contains(TripQuestion.WHERE_I_STAY)
        )
    }

    @Test
    fun `un viaje con solo hospedajes no ofrece la pregunta de itinerario`() {
        val trip = snapshot(activities = listOf(stay("Hotel Ibis", LocalDate.of(2026, 8, 11))))

        val questions = TripInsightsCalculator.availableQuestions(trip, LocalDate.of(2026, 8, 1))

        assertFalse(questions.contains(TripQuestion.NEXT_ACTIVITIES))
        assertTrue(questions.contains(TripQuestion.WHERE_I_STAY))
    }

    // ---------- gastos ----------

    @Test
    fun `el total gastado suma todos los gastos y calcula el porcentaje`() {
        val trip = snapshot(
            budget = 1000.0,
            expenses = listOf(
                ExpenseRecord(250.0, "Comida"),
                ExpenseRecord(150.0, "Transporte")
            )
        )

        val result = TripInsightsCalculator.answer(
            TripQuestion.TOTAL_SPENT, trip, LocalDate.of(2026, 8, 12)
        ) as TripInsight.TotalSpent

        assertEquals(400.0, result.total, 0.001)
        assertEquals(40, result.percentUsed)
    }

    @Test
    fun `sin presupuesto cargado no se calcula porcentaje`() {
        val trip = snapshot(budget = 0.0, expenses = listOf(ExpenseRecord(100.0, "Comida")))

        val result = TripInsightsCalculator.answer(
            TripQuestion.TOTAL_SPENT, trip, LocalDate.of(2026, 8, 12)
        ) as TripInsight.TotalSpent

        assertNull(result.percentUsed)
    }

    @Test
    fun `la categoria top es la de mayor monto, no la de mas gastos`() {
        val trip = snapshot(
            expenses = listOf(
                ExpenseRecord(10.0, "Comida"),
                ExpenseRecord(10.0, "Comida"),
                ExpenseRecord(10.0, "Comida"),
                ExpenseRecord(70.0, "Hospedaje")
            )
        )

        val result = TripInsightsCalculator.answer(
            TripQuestion.TOP_CATEGORY, trip, LocalDate.of(2026, 8, 12)
        ) as TripInsight.TopCategory

        assertEquals("Hospedaje", result.category)
        assertEquals(70.0, result.amount, 0.001)
        assertEquals(70, result.percentOfTotal)
    }

    @Test
    fun `los gastos sin categoria se agrupan bajo una etiqueta propia`() {
        val trip = snapshot(expenses = listOf(ExpenseRecord(50.0, null), ExpenseRecord(20.0, "")))

        val result = TripInsightsCalculator.answer(
            TripQuestion.TOP_CATEGORY, trip, LocalDate.of(2026, 8, 12)
        ) as TripInsight.TopCategory

        assertEquals("Sin categoría", result.category)
        assertEquals(70.0, result.amount, 0.001)
    }

    // ---------- presupuesto restante ----------

    @Test
    fun `el presupuesto restante se reparte entre los dias que quedan`() {
        val trip = snapshot(budget = 1000.0, expenses = listOf(ExpenseRecord(400.0, "Comida")))

        // 13, 14 y 15 de agosto: quedan 3 días contando hoy.
        val result = TripInsightsCalculator.answer(
            TripQuestion.REMAINING_BUDGET, trip, LocalDate.of(2026, 8, 13)
        ) as TripInsight.RemainingBudget

        assertEquals(600.0, result.remaining, 0.001)
        assertEquals(3, result.daysLeft)
        assertEquals(200.0, result.perDay!!, 0.001)
        assertFalse(result.isOverBudget)
    }

    @Test
    fun `antes de salir el presupuesto se reparte sobre el viaje entero`() {
        val trip = snapshot(budget = 600.0)

        val result = TripInsightsCalculator.answer(
            TripQuestion.REMAINING_BUDGET, trip, LocalDate.of(2026, 8, 1)
        ) as TripInsight.RemainingBudget

        assertEquals(6, result.daysLeft)
        assertEquals(100.0, result.perDay!!, 0.001)
    }

    @Test
    fun `gastar de mas se marca como excedido`() {
        val trip = snapshot(budget = 100.0, expenses = listOf(ExpenseRecord(150.0, "Comida")))

        val result = TripInsightsCalculator.answer(
            TripQuestion.REMAINING_BUDGET, trip, LocalDate.of(2026, 8, 12)
        ) as TripInsight.RemainingBudget

        assertTrue(result.isOverBudget)
        assertEquals(-50.0, result.remaining, 0.001)
    }

    // ---------- valija ----------

    @Test
    fun `lo que falta empacar se agrupa por categoria de mayor a menor`() {
        val trip = snapshot(
            packing = listOf(
                PackingRecord("Remera", "Ropa", isPacked = false),
                PackingRecord("Pantalón", "Ropa", isPacked = false),
                PackingRecord("Cargador", "Electrónica", isPacked = false),
                PackingRecord("Pasaporte", "Documentos", isPacked = true)
            )
        )

        val result = TripInsightsCalculator.answer(
            TripQuestion.PENDING_PACKING, trip, LocalDate.of(2026, 8, 1)
        ) as TripInsight.PendingPacking

        assertEquals(3, result.pending)
        assertEquals(4, result.total)
        assertEquals("Ropa", result.byCategory.first().category)
        assertEquals(2, result.byCategory.first().count)
    }

    @Test
    fun `con todo empacado no quedan pendientes`() {
        val trip = snapshot(
            packing = listOf(
                PackingRecord("Remera", "Ropa", isPacked = true),
                PackingRecord("Pasaporte", "Documentos", isPacked = true)
            )
        )

        val result = TripInsightsCalculator.answer(
            TripQuestion.PENDING_PACKING, trip, LocalDate.of(2026, 8, 1)
        ) as TripInsight.PendingPacking

        assertEquals(0, result.pending)
        assertEquals(2, result.total)
        assertTrue(result.byCategory.isEmpty())
    }

    // ---------- datos incompletos ----------

    @Test
    fun `sin fechas cargadas las preguntas de calendario no rompen`() {
        val trip = snapshot(startDate = null, endDate = null)

        assertEquals(
            TripInsight.NotEnoughData,
            TripInsightsCalculator.answer(TripQuestion.FREE_DAYS, trip, LocalDate.of(2026, 8, 1))
        )
        assertEquals(
            TripInsight.NotEnoughData,
            TripInsightsCalculator.answer(TripQuestion.DAYS_UNTIL_TRIP, trip, LocalDate.of(2026, 8, 1))
        )
    }
}
