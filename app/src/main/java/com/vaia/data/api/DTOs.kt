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

data class PaginationMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
    @SerializedName("per_page") val perPage: Int = 15
)

data class PaginatedResponse<T>(
    val data: List<T>,
    val links: Map<String, String?>? = null,
    val meta: PaginationMeta? = null
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
    val currency: String?
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

data class SuggestionsResponse(
    val data: List<com.vaia.domain.model.ActivitySuggestion>
)

// Packing List DTOs
data class AddPackingItemRequest(
    val name: String,
    val category: String
)

data class PackingItemResponse(
    val item: PackingItem
)

data class WeatherSuggestionsResponse(
    val suggestions: List<WeatherSuggestion>
)

// Notification Preferences
data class NotificationPreferencesRequest(
    @SerializedName("activity_reminders")
    val activityReminders: Boolean? = null,
    @SerializedName("trip_reminders")
    val tripReminders: Boolean? = null
)

data class NotificationPreferencesResponse(
    @SerializedName("activity_reminders")
    val activityReminders: Boolean,
    @SerializedName("trip_reminders")
    val tripReminders: Boolean
)
