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
data class Trip(
    val id: String,
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val budget: Double,
    val totalExpenses: Double = 0.0,
    val activitiesCount: Int = 0,
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
    val time: String = "",
    val location: String,
    val cost: Double
) : Parcelable

data class ActivitySuggestion(
    val title: String,
    val description: String,
    val location: String,
    val cost: Double,
    val time: String = ""
)

@Parcelize
data class Expense(
    val id: String,
    val amount: Double,
    val description: String,
    val date: String,
    val category: String,
    val receiptImageUrl: String? = null
) : Parcelable

@Parcelize
data class Document(
    val id: String,
    val tripId: String,
    val userId: String,
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val description: String? = null,
    val category: String? = null
) : Parcelable

@Parcelize
data class AuthTokens(
    val accessToken: String,
    val tokenType: String = "Bearer"
) : Parcelable

// Document Checklist Models
@Parcelize
data class TripDocumentChecklist(
    val id: String,
    val tripId: String,
    val items: List<ChecklistItem> = emptyList(),
    val progress: DocumentProgress? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) : Parcelable

@Parcelize
data class ChecklistItem(
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val isCompleted: Boolean = false,
    val position: Int = 0,
    val document: ChecklistDocument? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) : Parcelable

@Parcelize
data class ChecklistDocument(
    val id: String,
    val checklistItemId: String,
    val fileName: String,
    val filePath: String? = null,
    val mimeType: String,
    val fileSize: Long,
    val source: String,
    val googleDriveFileId: String? = null,
    val uploadedBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) : Parcelable

@Parcelize
data class BudgetAdvice(
    val status: String,
    val message: String,
    val spentPercentage: Double,
    val totalExpenses: Double,
    val budget: Double,
    val daysElapsed: Int,
    val totalDays: Int
) : Parcelable

@Parcelize
data class DocumentProgress(
    val completed: Int,
    val total: Int,
    val percentage: Int
) : Parcelable

// Convierte destination (puede ser "Paris, Roma, Barcelona") a lista de paradas
fun Trip.destinationList(): List<String> =
    destination.split(",").map { it.trim() }.filter { it.isNotBlank() }

// Retorna solo el primer destino para imágenes de portada y stats
fun Trip.primaryDestination(): String = destinationList().firstOrNull() ?: destination
