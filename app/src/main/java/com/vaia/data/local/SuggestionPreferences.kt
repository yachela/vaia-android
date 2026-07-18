package com.vaia.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.vaia.domain.model.SuggestionIntensity
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferencias de la feature de sugerencias IA: intensidad elegida y
 * descartes por viaje (para no volver a mostrar sugerencias rechazadas).
 */
@Singleton
class SuggestionPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val intensityKey = stringPreferencesKey("suggestions_intensity")

    private fun dismissedKey(tripId: String) =
        stringSetPreferencesKey("dismissed_suggestions_$tripId")

    suspend fun getIntensity(): SuggestionIntensity {
        val stored = dataStore.data.first()[intensityKey]
        return SuggestionIntensity.entries.firstOrNull { it.apiValue == stored }
            ?: SuggestionIntensity.MODERATE
    }

    suspend fun setIntensity(intensity: SuggestionIntensity) {
        dataStore.edit { prefs -> prefs[intensityKey] = intensity.apiValue }
    }

    suspend fun getDismissedIds(tripId: String): Set<String> {
        return dataStore.data.first()[dismissedKey(tripId)] ?: emptySet()
    }

    suspend fun addDismissedId(tripId: String, suggestionId: String) {
        dataStore.edit { prefs ->
            val current = prefs[dismissedKey(tripId)] ?: emptySet()
            // Tope para no acumular descartes indefinidamente por viaje
            prefs[dismissedKey(tripId)] = (current + suggestionId).takeLast50()
        }
    }

    private fun Set<String>.takeLast50(): Set<String> =
        if (size <= 50) this else toList().takeLast(50).toSet()
}
