package com.vaia.data.repository

import com.vaia.domain.model.ActivitySuggestion

/**
 * Valida y sanea las sugerencias devueltas por el modelo de IA.
 * El backend no valida la salida del modelo y el router free-tier puede
 * devolver campos malformados; acá se filtra/normaliza antes de mostrar.
 */
object SuggestionValidator {

    private val TIME_REGEX = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

    fun sanitize(raw: List<ActivitySuggestion>): List<ActivitySuggestion> {
        return raw.mapNotNull { suggestion ->
            // Gson puede dejar null en campos non-null de Kotlin
            val title = (suggestion.title as Any?)?.toString()?.trim().orEmpty()
            val location = (suggestion.location as Any?)?.toString()?.trim().orEmpty()
            if (title.isEmpty() || location.isEmpty()) return@mapNotNull null

            val description = (suggestion.description as Any?)?.toString()?.trim().orEmpty()
            val time = (suggestion.time as Any?)?.toString()?.trim().orEmpty()

            ActivitySuggestion(
                title = title.take(100),
                description = description.take(200),
                location = location.take(100),
                cost = suggestion.cost.coerceAtLeast(0.0),
                time = if (TIME_REGEX.matches(time)) time else ""
            )
        }
    }
}
