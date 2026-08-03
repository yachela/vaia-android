package com.vaia.presentation.ui.trips

import com.vaia.presentation.ui.common.normalizeDateForApi
import java.time.LocalDate

/**
 * Reglas de fechas del formulario de viaje, separadas del composable para poder testearlas.
 *
 * Espejan lo que valida el backend (`after:today` y `after:start_date` en StoreTripRequest):
 * si acá fueran más permisivas, el formulario dejaría pasar un viaje que el servidor rechaza
 * y el error aparecería recién al tocar "Crear".
 */
enum class TripDateError {
    /** El backend usa `after:start_date`, estricto: un viaje no puede empezar y terminar el mismo día. */
    END_NOT_AFTER_START,

    /** El backend usa `after:today`: un viaje nuevo no puede empezar hoy ni antes. */
    START_NOT_AFTER_TODAY
}

/** Primera fecha de fin válida para [startIso]: el día siguiente. `null` si la fecha no se entiende. */
fun minEndDate(startIso: String): String? =
    parseIsoDate(startIso)?.plusDays(1)?.toString()

/**
 * Al elegir la fecha de inicio, acomoda la de fin en vez de esperar a que la persona
 * descubra sola que quedó inválida. Solo la mueve si hace falta: una fecha de fin ya
 * posterior se respeta.
 */
fun adjustEndDate(startIso: String, currentEndIso: String): String {
    val start = parseIsoDate(startIso) ?: return currentEndIso
    val end = parseIsoDate(currentEndIso)
    return if (end == null || !end.isAfter(start)) start.plusDays(1).toString() else currentEndIso
}

/**
 * `null` si el rango es válido. [requireFutureStart] solo aplica al crear: al editar, un
 * viaje que ya empezó tiene una fecha de inicio pasada que es legítima.
 */
fun validateTripDateRange(
    startIso: String,
    endIso: String,
    today: LocalDate,
    requireFutureStart: Boolean
): TripDateError? {
    val start = parseIsoDate(startIso) ?: return null
    val end = parseIsoDate(endIso) ?: return null

    return when {
        requireFutureStart && !start.isAfter(today) -> TripDateError.START_NOT_AFTER_TODAY
        !end.isAfter(start) -> TripDateError.END_NOT_AFTER_START
        else -> null
    }
}

/** Fechas sin parsear no son un error acá: de eso ya se encarga la validación de campo requerido. */
private fun parseIsoDate(rawValue: String): LocalDate? =
    normalizeDateForApi(rawValue)?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            null
        }
    }
