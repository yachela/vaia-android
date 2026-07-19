package com.vaia.domain.model

import java.time.LocalDate

/**
 * Preguntas prearmadas que la app contesta con los datos que ya tiene cacheados
 * en Room, sin llamar a la IA. Responden al instante, funcionan sin conexión,
 * no consumen cupo del free tier y no pueden equivocar una cifra.
 *
 * Las preguntas que sí necesitan un modelo (recomendaciones, interpretación del
 * ritmo de gasto) llegan en Q2 — ver docs/spec-chat-viaje.md.
 */
enum class TripQuestion {
    DAYS_UNTIL_TRIP,
    NEXT_ACTIVITIES,
    FREE_DAYS,
    WHERE_I_STAY,
    TOTAL_SPENT,
    TOP_CATEGORY,
    REMAINING_BUDGET,
    PENDING_PACKING
}

/** Momento del viaje respecto de hoy; define qué preguntas tienen sentido ofrecer. */
enum class TripPhase { BEFORE, DURING, AFTER }

/** Una actividad ya resuelta a fecha/hora, lista para mostrar. */
data class PlannedActivity(
    val title: String,
    val time: String?,
    val location: String?
)

/** Un hospedaje del viaje, con el título ya sin el prefijo `[HOSPEDAJE]`. */
data class Stay(
    val name: String,
    val location: String?,
    val checkIn: LocalDate?
)

/**
 * Resultado de una pregunta. Son datos, no texto: el formato a español vive en la
 * UI. Así los tests afirman sobre cifras y no sobre redacción, que es lo que
 * importa cuando el criterio es "las cifras coinciden con la pantalla de gastos".
 */
sealed interface TripInsight {

    data class DaysUntilTrip(
        val days: Int,
        val phase: TripPhase
    ) : TripInsight

    data class NextActivities(
        val date: LocalDate?,
        val activities: List<PlannedActivity>
    ) : TripInsight

    data class FreeDays(
        val dates: List<LocalDate>,
        val totalDays: Int
    ) : TripInsight

    data class Accommodation(
        val stays: List<Stay>,
        /** El hospedaje que aplica hoy, si el viaje está en curso. */
        val current: Stay?
    ) : TripInsight

    data class TotalSpent(
        val total: Double,
        val budget: Double
    ) : TripInsight {
        /** Porcentaje del presupuesto consumido, o null si no hay presupuesto cargado. */
        val percentUsed: Int? = if (budget > 0) ((total / budget) * 100).toInt() else null
    }

    data class TopCategory(
        val category: String,
        val amount: Double,
        val percentOfTotal: Int
    ) : TripInsight

    data class RemainingBudget(
        val remaining: Double,
        val budget: Double,
        val daysLeft: Int?
    ) : TripInsight {
        /** Cuánto queda por día si el viaje sigue en curso. Null si ya terminó o no hay días. */
        val perDay: Double? = if (daysLeft != null && daysLeft > 0) remaining / daysLeft else null
        val isOverBudget: Boolean = remaining < 0
    }

    data class PendingPacking(
        val pending: Int,
        val total: Int,
        val byCategory: List<CategoryCount>
    ) : TripInsight

    /** La pregunta se ofreció pero los datos cacheados no alcanzan para contestarla. */
    data object NotEnoughData : TripInsight
}

data class CategoryCount(val category: String, val count: Int)
