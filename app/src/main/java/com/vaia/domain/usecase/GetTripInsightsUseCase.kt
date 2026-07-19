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
/**
 * La UI depende de esta interfaz y no de la implementación, así los tests del
 * ViewModel sustituyen la carga sin montar cuatro DAOs falsos.
 */
interface TripInsightsProvider {
    suspend fun snapshotOf(tripId: String): TripSnapshot?
}

class GetTripInsightsUseCase @Inject constructor(
    private val tripDao: TripDao,
    private val activityDao: ActivityDao,
    private val expenseDao: ExpenseDao,
    private val packingDao: PackingDao
) : TripInsightsProvider {

    override suspend fun snapshotOf(tripId: String): TripSnapshot? {
        val trip = tripDao.getById(tripId) ?: return null

        val activities = activityDao.getByTripId(tripId).map {
            val title = it.title.orEmpty()
            val isAccommodation = isAccommodation(title, it.description)
            ActivityRecord(
                title = if (isAccommodation) stripAccommodationPrefix(title) else title,
                date = parseDate(it.date),
                time = normalizeTime(it.time),
                location = it.location,
                isAccommodation = isAccommodation
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
        private const val ACCOMMODATION_PREFIX = "[HOSPEDAJE]"
        private const val ACCOMMODATION_TAG = "#alojamiento"

        /**
         * Misma convención que usa ActivitiesViewModel para separar hospedajes del
         * itinerario. Si allá cambia el criterio, acá tiene que cambiar también.
         */
        fun isAccommodation(title: String, description: String?): Boolean =
            title.startsWith(ACCOMMODATION_PREFIX, ignoreCase = true) ||
                description?.contains(ACCOMMODATION_TAG, ignoreCase = true) == true

        fun stripAccommodationPrefix(title: String): String =
            title.replaceFirst(Regex("^\\s*\\[HOSPEDAJE\\]\\s*", RegexOption.IGNORE_CASE), "").trim()

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
