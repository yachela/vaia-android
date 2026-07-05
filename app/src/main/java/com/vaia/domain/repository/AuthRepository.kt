package com.vaia.domain.repository

import com.vaia.domain.model.AuthTokens
import com.vaia.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthTokens>
    suspend fun register(name: String, email: String, password: String, passwordConfirmation: String): Result<AuthTokens>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Result<User>
    suspend fun updateProfile(
        name: String,
        bio: String?,
        country: String?,
        language: String?,
        currency: String?
    ): Result<User>
    suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Result<User>
    fun isLoggedIn(): Boolean
    fun getAccessToken(): String?
}
