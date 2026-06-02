package com.vaia.presentation.ui.common

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class TripVoiceData(
    val title: String? = null,
    val destination: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val budget: Double? = null,
    val tripType: String? = null
)

class VoiceInputParser {

    fun parseVoiceText(text: String): TripVoiceData {
        val lower = text.lowercase()
        
        // 1. Extraer tipo de viaje
        val tripType = when {
            "pareja" in lower || "novia" in lower || "novio" in lower || "esposo" in lower || "esposa" in lower || "romántico" in lower || "romantico" in lower -> "pareja"
            "amigo" in lower || "amigos" in lower || "grupal" in lower || "con los pibes" in lower -> "amigos"
            "solo" in lower || "solitario" in lower || "solitaria" in lower || "conmigo" in lower -> "solitario"
            "familia" in lower || "familiar" in lower || "hijo" in lower || "hijos" in lower || "padres" in lower || "niños" in lower -> "familiar"
            "aventura" in lower || "extremo" in lower || "montaña" in lower || "trekking" in lower -> "aventura"
            else -> "aventura"
        }

        // 2. Extraer presupuesto
        val budgetRegex = """(\d+[\.,]?\d*)\s*(dólares|dolares|usd|euros|usd|ars|pesos|\$)""".toRegex()
        val budgetMatch = budgetRegex.find(lower)
        val budget = budgetMatch?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 1000.0

        // 3. Extraer destino usando filtros de stopwords
        val stopwords = setOf(
            "quiero", "viajar", "viaje", "planificar", "ir", "un", "una", "el", "la", "los", "las",
            "en", "con", "de", "para", "a", "hacia", "por", "mi", "mis", "nuestro", "nuestros",
            "nuestra", "nuestras", "presupuesto", "dólares", "dolares", "pesos", "euros", "usd",
            "amigos", "pareja", "familia", "solitario", "solo", "sola", "vacaciones", "escapada",
            "turismo", "visitar", "conocer", "días", "dias", "semana", "mes", "del", "al", "de",
            "el", "la", "y", "o", "que", "debe", "ser", "con", "sin", "dolares", "pesos", "usd",
            "romántico", "romantico", "familiar", "solitaria", "extremo", "trekking", "aventura",
            "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto",
            "septiembre", "octubre", "noviembre", "diciembre"
        )
        
        val words = text.split(Regex("""[\s,;\.\-\?\!\(\)]+""")).filter { it.isNotBlank() }
        val remainingWords = words.filter { word ->
            val cleanWord = word.lowercase().trim()
            cleanWord !in stopwords && !cleanWord.matches(Regex("""\d+"""))
        }
        
        var destination = "Destino"
        if (remainingWords.isNotEmpty()) {
            destination = remainingWords.joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        // 4. Extraer fechas estimadas si se mencionan meses
        val months = listOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
        )
        
        val today = LocalDate.now()
        var startLocalDate = today.plusDays(7) // default
        var endLocalDate = startLocalDate.plusDays(5) // default 5 días de viaje

        for (i in months.indices) {
            val monthName = months[i]
            if (monthName in lower) {
                val dayRegex = """(\d+)\s+de\s+$monthName""".toRegex()
                val matches = dayRegex.findAll(lower).toList()
                if (matches.isNotEmpty()) {
                    val startDay = matches[0].groupValues[1].toIntOrNull()
                    if (startDay != null) {
                        startLocalDate = LocalDate.of(today.year, i + 1, startDay)
                        if (startLocalDate.isBefore(today)) {
                            startLocalDate = startLocalDate.plusYears(1)
                        }
                    }
                    if (matches.size > 1) {
                        val endDay = matches[1].groupValues[1].toIntOrNull()
                        if (endDay != null) {
                            endLocalDate = LocalDate.of(startLocalDate.year, i + 1, endDay)
                        }
                    } else {
                        endLocalDate = startLocalDate.plusDays(5)
                    }
                }
                break
            }
        }

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val startDateFormatted = startLocalDate.format(dateFormatter)
        val endDateFormatted = endLocalDate.format(dateFormatter)

        val title = when (tripType) {
            "pareja" -> "Escapada a $destination"
            "amigos" -> "Viaje con amigos a $destination"
            "familiar" -> "Viaje familiar a $destination"
            "solitario" -> "Aventura en solitario a $destination"
            else -> "Aventura en $destination"
        }

        return TripVoiceData(
            title = title,
            destination = destination,
            startDate = startDateFormatted,
            endDate = endDateFormatted,
            budget = budget,
            tripType = tripType
        )
    }
}
