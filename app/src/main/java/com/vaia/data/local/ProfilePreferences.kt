package com.vaia.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Copia local de las preferencias del perfil que la app necesita antes de que
 * responda la red.
 *
 * `getCurrentUser()` es una llamada HTTP sin caché: sin esto, alguien de viaje y sin
 * señal creaba todos sus viajes en USD aunque su perfil dijera otra cosa, que es
 * justo el escenario donde más importa.
 */
interface ProfilePreferences {
    suspend fun getCurrency(): String?
    suspend fun setCurrency(code: String)
}

@Singleton
class DataStoreProfilePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ProfilePreferences {

    private val currencyKey = stringPreferencesKey("profile_currency")

    override suspend fun getCurrency(): String? = dataStore.data.first()[currencyKey]

    override suspend fun setCurrency(code: String) {
        dataStore.edit { prefs -> prefs[currencyKey] = code }
    }
}
