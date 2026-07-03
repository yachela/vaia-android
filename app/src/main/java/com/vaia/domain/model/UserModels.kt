package com.vaia.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String,
    val name: String,
    val email: String,
    val bio: String? = null,
    val country: String? = null,
    val language: String? = null,
    val currency: String? = null,
    val avatarUrl: String? = null
) : Parcelable

@Parcelize
data class AuthTokens(
    val accessToken: String,
    val tokenType: String = "Bearer"
) : Parcelable
