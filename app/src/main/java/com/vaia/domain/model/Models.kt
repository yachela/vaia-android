package com.vaia.domain.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
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
    @SerializedName("avatar_url")
    val avatarUrl: String? = null
) : Parcelable

@Parcelize
data class Trip(
    val id: String,
    val title: String,
    val destination: String,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String,
    val budget: Double,
    @SerializedName("total_expenses")
    val totalExpenses: Double = 0.0,
    @SerializedName("activities_count")
    val activitiesCount: Int = 0,
    @SerializedName("expenses_count")
    val expensesCount: Int = 0,
    val activities: List<Activity> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val documents: List<Document> = emptyList()
) : Parcelable

@Parcelize
data class Activity(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val location: String,
    val cost: Double
) : Parcelable

@Parcelize
data class Expense(
    val id: String,
    val amount: Double,
    val description: String,
    val date: String,
    val category: String,
    @SerializedName("receipt_image_url")
    val receiptImageUrl: String? = null
) : Parcelable

@Parcelize
data class Document(
    val id: String,
    @SerializedName("trip_id") val tripId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("file_path") val filePath: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("mime_type") val mimeType: String,
    @SerializedName("file_size") val fileSize: Long,
    val description: String?
) : Parcelable

@Parcelize
data class AuthTokens(
    val accessToken: String,
    val tokenType: String = "Bearer"
) : Parcelable

@Parcelize
data class LoginResponse(
    val user: User,
    val accessToken: String,
    val tokenType: String = "Bearer"
) : Parcelable

@Parcelize
data class LoginRequest(
    val email: String,
    val password: String
) : Parcelable

@Parcelize
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val passwordConfirmation: String
) : Parcelable
