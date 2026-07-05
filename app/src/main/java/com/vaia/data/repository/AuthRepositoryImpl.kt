package com.vaia.data.repository

import com.vaia.data.api.VaiaApiService
import com.vaia.data.api.dto.UpdateUserProfileRequest
import com.vaia.data.api.dto.LoginRequestDto
import com.vaia.data.api.dto.RegisterRequestDto
import com.vaia.data.api.dto.toDomain
import com.vaia.data.api.dto.toDomainTokens
import com.vaia.data.auth.TokenProvider
import com.vaia.data.auth.TokenStorage
import com.vaia.data.network.ErrorMapper
import com.vaia.domain.model.AuthTokens
import com.vaia.domain.model.User
import com.vaia.domain.repository.AuthRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class AuthRepositoryImpl(
    private val apiService: VaiaApiService,
    private val tokenStorage: TokenStorage,
    private val tokenProvider: TokenProvider
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthTokens> {
        return try {
            val response = apiService.login(LoginRequestDto(email, password))
            if (response.isSuccessful) {
                response.body()?.data?.let { loginData ->
                    val tokens = loginData.toDomainTokens()
                    saveAccessToken(tokens.accessToken)
                    Result.success(tokens)
                } ?: Result.failure(Exception("Login failed: No data received"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo iniciar sesión"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun register(name: String, email: String, password: String, passwordConfirmation: String): Result<AuthTokens> {
        return try {
            val response = apiService.register(
                RegisterRequestDto(name, email, password, passwordConfirmation)
            )
            if (response.isSuccessful) {
                response.body()?.data?.let { tokensDto ->
                    val tokens = tokensDto.toDomain()
                    saveAccessToken(tokens.accessToken)
                    Result.success(tokens)
                } ?: Result.failure(Exception("Registration failed: No data received"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo completar el registro"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
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
                    Result.success(user.toDomain())
                } ?: Result.failure(Exception("No se pudo cargar el perfil"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo cargar el perfil"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
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
                    Result.success(user.toDomain())
                } ?: Result.failure(Exception("No se pudo actualizar el perfil"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo actualizar el perfil"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Result<User> {
        return try {
            val requestBody = imageBytes.toRequestBody(mimeType.toMediaType())
            val part = MultipartBody.Part.createFormData("avatar", "avatar.jpg", requestBody)
            val response = apiService.uploadAvatar(part)
            if (response.isSuccessful) {
                response.body()?.data?.let { user ->
                    Result.success(user.toDomain())
                } ?: Result.failure(Exception("No se pudo actualizar el avatar"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo actualizar el avatar"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override fun isLoggedIn(): Boolean {
        return tokenProvider.isLoggedIn()
    }

    override fun getAccessToken(): String? {
        return tokenProvider.token
    }

    private fun saveAccessToken(token: String) {
        tokenStorage.saveToken(token)
        tokenProvider.token = token
    }

    private fun clearAccessToken() {
        tokenStorage.clearToken()
        tokenProvider.token = null
    }

}
