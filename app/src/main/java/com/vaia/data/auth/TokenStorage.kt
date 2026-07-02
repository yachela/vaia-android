package com.vaia.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persistencia del token de sesión.
 */
interface TokenStorage {
    fun getToken(): String?
    fun saveToken(token: String)
    fun clearToken()
}

/**
 * Implementación cifrada con EncryptedSharedPreferences.
 *
 * La clave maestra vive en el Android Keystore (AES-256 GCM), por lo que el
 * token nunca se escribe en disco en texto plano.
 */
class EncryptedTokenStorage(context: Context) : TokenStorage {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    override fun saveToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    override fun clearToken() {
        prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
    }

    private companion object {
        const val PREFS_FILE_NAME = "vaia_secure_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
    }
}
