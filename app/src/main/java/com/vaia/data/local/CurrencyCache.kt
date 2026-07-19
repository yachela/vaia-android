package com.vaia.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Última cotización conocida por moneda base, para poder convertir sin conexión.
 * Las tasas son un mapa chico, así que van serializadas en DataStore en vez de
 * agregar una tabla más a Room.
 */
@Singleton
class CurrencyCache @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private val gson = Gson()

    private fun ratesKey(base: String) = stringPreferencesKey("currency_rates_${base.lowercase()}")
    private fun ratesUpdatedKey(base: String) = longPreferencesKey("currency_rates_updated_${base.lowercase()}")
    private val currenciesKey = stringPreferencesKey("currency_list")

    suspend fun saveRates(base: String, rates: Map<String, Double>) {
        dataStore.edit { prefs ->
            prefs[ratesKey(base)] = gson.toJson(rates)
            prefs[ratesUpdatedKey(base)] = System.currentTimeMillis()
        }
    }

    /** Tasas guardadas con el momento en que se bajaron, o null si nunca se cachearon. */
    suspend fun getRates(base: String): Pair<Map<String, Double>, Long>? {
        val prefs = dataStore.data.first()
        val json = prefs[ratesKey(base)] ?: return null
        val updatedAt = prefs[ratesUpdatedKey(base)] ?: return null
        val type = object : TypeToken<Map<String, Double>>() {}.type
        return runCatching { gson.fromJson<Map<String, Double>>(json, type) }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.let { it to updatedAt }
    }

    suspend fun saveCurrencies(currencies: Map<String, String>) {
        dataStore.edit { prefs -> prefs[currenciesKey] = gson.toJson(currencies) }
    }

    suspend fun getCurrencies(): Map<String, String>? {
        val json = dataStore.data.first()[currenciesKey] ?: return null
        val type = object : TypeToken<Map<String, String>>() {}.type
        return runCatching { gson.fromJson<Map<String, String>>(json, type) }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
    }
}
