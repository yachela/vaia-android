package com.vaia.data.api

import com.google.gson.annotations.SerializedName
import com.vaia.domain.model.*

// API Response wrappers
data class ApiResponse<T>(
    val data: T? = null,
    val message: String? = null,
    val errors: Map<String, List<String>>? = null
)

data class LoginResponseData(
    val user: User,
    val access_token: String,
    val token_type: String
)

data class PaginatedResponse<T>(
    val data: List<T>,
    val links: Map<String, String>? = null,
    val meta: Map<String, Any>? = null
)

// Request DTOs
data class CreateTripRequest(
    val title: String,
    val destination: String,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    val budget: Double
)

data class UpdateTripRequest(
    val title: String,
    val destination: String,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    val budget: Double
)

data class CreateActivityRequest(
    val title: String,
    val description: String,
    val date: String,
    val time: String?,
    val location: String,
    val cost: Double
)

data class UpdateActivityRequest(
    val title: String,
    val description: String,
    val date: String,
    val time: String?,
    val location: String,
    val cost: Double
)

data class UpdateUserProfileRequest(
    val name: String,
    val bio: String?,
    val country: String?,
    val language: String?,
    val currency: String?,
    @SerializedName("avatar_url")
    val avatarUrl: String?
)

// Checklist DTOs
data class AddChecklistItemRequest(
    val name: String
)

data class ToggleCompleteRequest(
    @SerializedName("is_completed")
    val isCompleted: Boolean
)

data class ImportFromDriveRequest(
    @SerializedName("file_id")
    val fileId: String,
    @SerializedName("access_token")
    val accessToken: String
)

data class DocumentPreviewResponse(
    val url: String,
    @SerializedName("expires_at")
    val expiresAt: String
)
