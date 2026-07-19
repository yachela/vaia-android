package com.vaia.domain.usecase

import com.vaia.data.local.db.ActivityDao
import com.vaia.data.local.db.ExpenseDao
import com.vaia.data.local.db.PackingDao
import com.vaia.data.local.db.TripDao
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Arma el [TripSnapshot] leyendo **solo de Room**, nunca de la API.
 *
 * Es a propósito: estas preguntas tienen que contestarse en modo avión, y los
 * repositorios de la app son network-first. Los datos ya llegan al cache por los
 * refrescos normales de las pantallas de viaje, gastos y valija.
 */
class GetTripInsightsUseCase @Inject constructor(
    private val tripDao: TripDao,
    private val activityDao: ActivityDao,
    private val expenseDao: ExpenseDao,
    private val packingDao: PackingDao
) {

    suspend operator fun invoke(tripId: String): TripSnapshot? {
        val trip = tripDao.getById(tripId) ?: return null

        val activities = activityDao.getByTripId(tripId).map {
            ActivityRecord(
                title = it.title.orEmpty(),
                date = parseDate(it.date),
                time = normalizeTime(it.time),
                location = it.location
            )
        }

        val expenses = expenseDao.getByTripId(tripId).map {
            ExpenseRecord(amount = it.amount ?: 0.0, category = it.category)
        }

        val packingItems = packingDao.getPackingListByTripIdSync(tripId)
            ?.let { packingDao.getPackingItemsByListIdSync(it.id) }
            .orEmpty()
            .map { PackingRecord(name = it.name, category = it.category, isPacked = it.isPacked) }

        return TripSnapshot(
            startDate = parseDate(trip.startDate),
            endDate = parseDate(trip.endDate),
            budget = trip.budget,
            activities = activities,
            expenses = expenses,
            packingItems = packingItems
        )
    }

    companion object {
        // El backend devuelve fechas en varios formatos según el endpoint; mismos
        // candidatos que usa DateTimeUiUtils para normalizar.
        private val DATE_PATTERNS = listOf(
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )

        private val ISO_OUTPUT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

        fun parseDate(raw: String?): LocalDate? {
            val value = raw?.trim().orEmpty()
            if (value.isEmpty()) return null

            DATE_PATTERNS.forEach { pattern ->
                val parser = SimpleDateFormat(pattern, Locale.US).apply {
                    isLenient = false
                    if (pattern.contains("'Z'")) timeZone = TimeZone.getTimeZone("UTC")
                }
                try {
                    val parsed = parser.parse(value) ?: return@forEach
                    return LocalDate.parse(ISO_OUTPUT.format(parsed))
                } catch (_: ParseException) {
                    // probar el siguiente patrón
                }
            }
            return null
        }

        /** Deja la hora en HH:mm; descarta lo que no parezca una hora. */
        fun normalizeTime(raw: String?): String? {
            val value = raw?.trim().orEmpty()
            if (value.isEmpty()) return null
            return Regex("^(\\d{1,2}):(\\d{2})").find(value)?.let { match ->
                val hour = match.groupValues[1].padStart(2, '0')
                "$hour:${match.groupValues[2]}"
            }
        }
    }
}
