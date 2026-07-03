package com.vaia.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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
data class BudgetAdvice(
    val status: String,
    val message: String,
    val spentPercentage: Double,
    val totalExpenses: Double,
    val budget: Double,
    val daysElapsed: Int,
    val totalDays: Int
) : Parcelable

// Convierte destination (puede ser "Paris, Roma, Barcelona") a lista de paradas
fun Trip.destinationList(): List<String> =
    destination.split(",").map { it.trim() }.filter { it.isNotBlank() }

// Retorna solo el primer destino para imágenes de portada y stats
fun Trip.primaryDestination(): String = destinationList().firstOrNull() ?: destination
