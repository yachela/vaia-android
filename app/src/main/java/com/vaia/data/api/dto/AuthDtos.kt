package com.vaia.data.api.dto

import com.google.gson.annotations.SerializedName
import com.vaia.domain.model.AuthTokens
import com.vaia.domain.model.User

// DTOs de red para autenticación y usuario. La capa domain no conoce Gson.

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String,
    @SerializedName("password_confirmation")
    val passwordConfirmation: String
)

data class UserDto(
    val id: String?,
    val name: String?,
    val email: String?,
    val bio: String? = null,
    val country: String? = null,
    val language: String? = null,
    val currency: String? = null,
    @SerializedName("avatar_url")
    val avatarUrl: String? = null
)

data class AuthTokensDto(
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("token_type")
    val tokenType: String? = null
)

data class LoginDataDto(
    val user: UserDto? = null,
    @SerializedName("access_token")
    val accessToken: String?,
    @SerializedName("token_type")
    val tokenType: String? = null
)

fun UserDto.toDomain(): User = User(
    id = id.orEmpty(),
    name = name.orEmpty(),
    email = email.orEmpty(),
    bio = bio,
    country = country,
    language = language,
    currency = currency,
    avatarUrl = avatarUrl
)

fun AuthTokensDto.toDomain(): AuthTokens = AuthTokens(
    accessToken = accessToken.orEmpty(),
    tokenType = tokenType ?: "Bearer"
)

fun LoginDataDto.toDomainTokens(): AuthTokens = AuthTokens(
    accessToken = accessToken.orEmpty(),
    tokenType = tokenType ?: "Bearer"
)
