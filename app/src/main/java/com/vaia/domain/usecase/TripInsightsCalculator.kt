package com.vaia.domain.usecase

import com.vaia.domain.model.CategoryCount
import com.vaia.domain.model.PlannedActivity
import com.vaia.domain.model.TripInsight
import com.vaia.domain.model.TripPhase
import com.vaia.domain.model.TripQuestion
import java.time.LocalDate

/** Datos de un viaje ya normalizados, sin acoplar el cálculo a Room ni a la API. */
data class TripSnapshot(
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val budget: Double,
    val activities: List<ActivityRecord>,
    val expenses: List<ExpenseRecord>,
    val packingItems: List<PackingRecord>
)

data class ActivityRecord(
    val title: String,
    val date: LocalDate?,
    val time: String?,
    val location: String?
)

data class ExpenseRecord(
    val amount: Double,
    val category: String?
)

data class PackingRecord(
    val name: String,
    val category: String?,
    val isPacked: Boolean
)

/**
 * Cálculo puro de las respuestas: sin DAOs, sin corrutinas, sin `LocalDate.now()`.
 * Recibe todo lo que necesita, así los tests cubren casos de borde (viaje ya
 * terminado, sin gastos, presupuesto en cero) sin montar una base ni mockear nada.
 */
object TripInsightsCalculator {

    /** Preguntas que tiene sentido ofrecer según lo que el viaje realmente tiene cargado. */
    fun availableQuestions(snapshot: TripSnapshot, today: LocalDate): List<TripQuestion> {
        val phase = phaseOf(snapshot, today)
        val hasExpenses = snapshot.expenses.isNotEmpty()
        val questions = mutableListOf<TripQuestion>()

        if (phase == TripPhase.BEFORE) questions += TripQuestion.DAYS_UNTIL_TRIP
        if (phase != TripPhase.AFTER && snapshot.activities.isNotEmpty()) {
            questions += TripQuestion.NEXT_ACTIVITIES
        }
        if (phase != TripPhase.AFTER && snapshot.startDate != null && snapshot.endDate != null) {
            questions += TripQuestion.FREE_DAYS
        }
        if (hasExpenses) {
            questions += TripQuestion.TOTAL_SPENT
            questions += TripQuestion.TOP_CATEGORY
        }
        // El presupuesto restante importa mientras se pueda gastar, aunque todavía
        // no haya ningún gasto cargado.
        if (phase != TripPhase.AFTER && snapshot.budget > 0) {
            questions += TripQuestion.REMAINING_BUDGET
        }
        if (phase != TripPhase.AFTER && snapshot.packingItems.isNotEmpty()) {
            questions += TripQuestion.PENDING_PACKING
        }
        return questions
    }

    fun answer(question: TripQuestion, snapshot: TripSnapshot, today: LocalDate): TripInsight =
        when (question) {
            TripQuestion.DAYS_UNTIL_TRIP -> daysUntilTrip(snapshot, today)
            TripQuestion.NEXT_ACTIVITIES -> nextActivities(snapshot, today)
            TripQuestion.FREE_DAYS -> freeDays(snapshot, today)
            TripQuestion.TOTAL_SPENT -> totalSpent(snapshot)
            TripQuestion.TOP_CATEGORY -> topCategory(snapshot)
            TripQuestion.REMAINING_BUDGET -> remainingBudget(snapshot, today)
            TripQuestion.PENDING_PACKING -> pendingPacking(snapshot)
        }

    fun phaseOf(snapshot: TripSnapshot, today: LocalDate): TripPhase {
        val start = snapshot.startDate ?: return TripPhase.BEFORE
        val end = snapshot.endDate ?: start
        return when {
            today.isBefore(start) -> TripPhase.BEFORE
            today.isAfter(end) -> TripPhase.AFTER
            else -> TripPhase.DURING
        }
    }

    private fun daysUntilTrip(snapshot: TripSnapshot, today: LocalDate): TripInsight {
        val start = snapshot.startDate ?: return TripInsight.NotEnoughData
        val phase = phaseOf(snapshot, today)
        val days = when (phase) {
            TripPhase.BEFORE -> java.time.temporal.ChronoUnit.DAYS.between(today, start).toInt()
            TripPhase.DURING -> java.time.temporal.ChronoUnit.DAYS.between(today, snapshot.endDate ?: start).toInt()
            TripPhase.AFTER -> 0
        }
        return TripInsight.DaysUntilTrip(days = days, phase = phase)
    }

    /**
     * El próximo día con actividades, no literalmente mañana: preguntar por mañana
     * devuelve "nada" la mayoría de las veces y no le sirve a nadie.
     */
    private fun nextActivities(snapshot: TripSnapshot, today: LocalDate): TripInsight {
        val upcoming = snapshot.activities
            .filter { it.date != null && !it.date.isBefore(today) }
            .sortedWith(compareBy({ it.date }, { it.time ?: "" }))

        val nextDate = upcoming.firstOrNull()?.date
            ?: return TripInsight.NextActivities(date = null, activities = emptyList())

        return TripInsight.NextActivities(
            date = nextDate,
            activities = upcoming
                .filter { it.date == nextDate }
                .map { PlannedActivity(it.title, it.time?.takeIf { t -> t.isNotBlank() }, it.location) }
        )
    }

    private fun freeDays(snapshot: TripSnapshot, today: LocalDate): TripInsight {
        val start = snapshot.startDate ?: return TripInsight.NotEnoughData
        val end = snapshot.endDate ?: return TripInsight.NotEnoughData
        if (end.isBefore(start)) return TripInsight.NotEnoughData

        val busy = snapshot.activities.mapNotNull { it.date }.toSet()
        // Solo días que todavía se pueden aprovechar.
        val from = if (today.isAfter(start)) today else start

        val free = generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(end) }
            .filter { it !in busy }
            .toList()

        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
        return TripInsight.FreeDays(dates = free, totalDays = totalDays)
    }

    private fun totalSpent(snapshot: TripSnapshot): TripInsight {
        val total = snapshot.expenses.sumOf { it.amount }
        return TripInsight.TotalSpent(total = total, budget = snapshot.budget)
    }

    private fun topCategory(snapshot: TripSnapshot): TripInsight {
        val total = snapshot.expenses.sumOf { it.amount }
        if (snapshot.expenses.isEmpty() || total <= 0) return TripInsight.NotEnoughData

        val (category, amount) = snapshot.expenses
            .groupBy { it.category?.takeIf { c -> c.isNotBlank() } ?: SIN_CATEGORIA }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .maxByOrNull { it.value }
            ?: return TripInsight.NotEnoughData

        return TripInsight.TopCategory(
            category = category,
            amount = amount,
            percentOfTotal = ((amount / total) * 100).toInt()
        )
    }

    private fun remainingBudget(snapshot: TripSnapshot, today: LocalDate): TripInsight {
        val spent = snapshot.expenses.sumOf { it.amount }
        val end = snapshot.endDate
        val start = snapshot.startDate
        val daysLeft = when {
            end == null -> null
            // Antes de salir, el presupuesto se reparte sobre el viaje entero.
            start != null && today.isBefore(start) ->
                java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt() + 1
            today.isAfter(end) -> null
            else -> java.time.temporal.ChronoUnit.DAYS.between(today, end).toInt() + 1
        }
        return TripInsight.RemainingBudget(
            remaining = snapshot.budget - spent,
            budget = snapshot.budget,
            daysLeft = daysLeft
        )
    }

    private fun pendingPacking(snapshot: TripSnapshot): TripInsight {
        val pending = snapshot.packingItems.filter { !it.isPacked }
        return TripInsight.PendingPacking(
            pending = pending.size,
            total = snapshot.packingItems.size,
            byCategory = pending
                .groupBy { it.category?.takeIf { c -> c.isNotBlank() } ?: SIN_CATEGORIA }
                .map { (category, items) -> CategoryCount(category, items.size) }
                .sortedByDescending { it.count }
        )
    }

    private const val SIN_CATEGORIA = "Sin categoría"
}
