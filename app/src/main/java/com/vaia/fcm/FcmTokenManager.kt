package com.vaia.fcm

import android.util.Log
import com.vaia.data.api.VaiaApiService
import kotlinx.coroutines.tasks.await
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class FcmTokenManager @Inject constructor(
    private val apiService: VaiaApiService
) {
    /**
     * Obtener el token FCM actual del dispositivo
     */
    open suspend fun getCurrentToken(): String? {
        return try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener token FCM", e)
            null
        }
    }

    /**
     * Enviar token FCM al backend
     */
    open suspend fun sendTokenToBackend(token: String) {
        try {
            val response = apiService.storeFcmToken(mapOf("fcm_token" to token))
            if (response.isSuccessful) {
                Log.d(TAG, "Token FCM enviado exitosamente al backend")
            } else {
                Log.e(TAG, "Error al enviar token FCM: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al enviar token FCM al backend", e)
            throw e
        }
    }

    /**
     * Eliminar token FCM del backend (al cerrar sesión)
     */
    open suspend fun deleteTokenFromBackend() {
        try {
            val response = apiService.deleteFcmToken()
            if (response.isSuccessful) {
                Log.d(TAG, "Token FCM eliminado exitosamente del backend")
            } else {
                Log.e(TAG, "Error al eliminar token FCM: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al eliminar token FCM del backend", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "FcmTokenManager"
    }
}
