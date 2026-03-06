package com.vaia.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.vaia.data.api.UpdateUserProfileRequest
import com.vaia.data.api.VaiaApiService
import com.vaia.domain.model.AuthTokens
import com.vaia.domain.model.User
import com.vaia.domain.repository.AuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuthRepositoryImpl(
    private val apiService: VaiaApiService,
    private val dataStore: DataStore<Preferences>,
    private val onTokenUpdated: (String?) -> Unit = {}
) : AuthRepository {
    private val accessTokenKey = stringPreferencesKey("access_token")

    override suspend fun login(email: String, password: String): Result<AuthTokens> {
        return try {
            val response = apiService.login(com.vaia.domain.model.LoginRequest(email, password))
            if (response.isSuccessful) {
                response.body()?.data?.let { loginData ->
                    val tokens = AuthTokens(
                        accessToken = loginData.access_token,
                        tokenType = loginData.token_type
                    )
                    saveAccessToken(tokens.accessToken)
                    Result.success(tokens)
                } ?: Result.failure(Exception("Login failed: No data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Login failed: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(name: String, email: String, password: String, passwordConfirmation: String): Result<AuthTokens> {
        return try {
            val response = apiService.register(
                com.vaia.domain.model.RegisterRequest(name, email, password, passwordConfirmation)
            )
            if (response.isSuccessful) {
                response.body()?.data?.let { tokens ->
                    saveAccessToken(tokens.accessToken)
                    Result.success(tokens)
                } ?: Result.failure(Exception("Registration failed: No data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Registration failed: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val response = apiService.logout()
            if (response.isSuccessful) {
                clearAccessToken()
                Result.success(Unit)
            } else {
                // Even if API call fails, clear local token
                clearAccessToken()
                Result.success(Unit)
            }
        } catch (e: Exception) {
            // Clear local token even if API call fails
            clearAccessToken()
            Result.success(Unit)
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful) {
                response.body()?.data?.let { user ->
                    Result.success(user)
                } ?: Result.failure(Exception("No se pudo cargar el perfil"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        name: String,
        bio: String?,
        country: String?,
        language: String?,
        currency: String?
    ): Result<User> {
        return try {
            val response = apiService.updateCurrentUser(
                UpdateUserProfileRequest(
                    name = name,
                    bio = bio,
                    country = country,
                    language = language,
                    currency = currency
                )
            )
            if (response.isSuccessful) {
                response.body()?.data?.let { user ->
                    Result.success(user)
                } ?: Result.failure(Exception("No se pudo actualizar el perfil"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Result<User> {
        return try {
            val requestBody = imageBytes.toRequestBody(mimeType.toMediaType())
            val part = MultipartBody.Part.createFormData("avatar", "avatar.jpg", requestBody)
            val response = apiService.uploadAvatar(part)
            if (response.isSuccessful) {
                response.body()?.data?.let { user ->
                    Result.success(user)
                } ?: Result.failure(Exception("No se pudo actualizar el avatar"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isLoggedIn(): Boolean {
        return runBlocking {
            dataStore.data.map { preferences ->
                preferences[accessTokenKey]?.isNotEmpty() == true
            }.first()
        }
    }

    override fun getAccessToken(): String? {
        return runBlocking {
            dataStore.data.map { preferences ->
                preferences[accessTokenKey]
            }.first()
        }
    }

    private suspend fun saveAccessToken(token: String) {
        dataStore.edit { preferences ->
            preferences[accessTokenKey] = token
        }
        onTokenUpdated(token)
    }

    private suspend fun clearAccessToken() {
        dataStore.edit { preferences ->
            preferences.remove(accessTokenKey)
        }
        onTokenUpdated(null)
    }

    private fun parseApiError(rawBody: String?, fallback: String): String {
        if (rawBody.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(rawBody)
            when {
                json.has("errors") -> {
                    val errors = json.optJSONObject("errors")
                    val firstField = errors?.keys()?.asSequence()?.firstOrNull()
                    val firstMessage = firstField
                        ?.let { key -> errors.optJSONArray(key)?.optString(0) }
                    firstMessage ?: json.optString("message", fallback)
                }
                json.has("message") -> json.optString("message", fallback)
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
