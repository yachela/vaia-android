package com.vaia.data.api.dto

import com.google.gson.annotations.SerializedName
import com.vaia.domain.model.BudgetAdvice
import com.vaia.domain.model.Trip

// DTOs de red para viajes. La capa domain no conoce Gson.

data class TripDto(
    val id: String?,
    val title: String?,
    val destination: String?,
    @SerializedName("start_date")
    val startDate: String?,
    @SerializedName("end_date")
    val endDate: String?,
    val budget: Double? = null,
    @SerializedName("total_expenses")
    val totalExpenses: Double? = null,
    @SerializedName("activities_count")
    val activitiesCount: Int? = null,
    @SerializedName("expenses_count")
    val expensesCount: Int? = null,
    val activities: List<ActivityDto>? = null,
    val expenses: List<ExpenseDto>? = null,
    val documents: List<DocumentDto>? = null
)

data class BudgetAdviceDto(
    val status: String?,
    val message: String?,
    @SerializedName("spent_percentage")
    val spentPercentage: Double? = null,
    @SerializedName("total_expenses")
    val totalExpenses: Double? = null,
    val budget: Double? = null,
    @SerializedName("days_elapsed")
    val daysElapsed: Int? = null,
    @SerializedName("total_days")
    val totalDays: Int? = null
)

fun TripDto.toDomain(): Trip = Trip(
    id = id.orEmpty(),
    title = title.orEmpty(),
    destination = destination.orEmpty(),
    startDate = startDate.orEmpty(),
    endDate = endDate.orEmpty(),
    budget = budget ?: 0.0,
    totalExpenses = totalExpenses ?: 0.0,
    activitiesCount = activitiesCount ?: 0,
    expensesCount = expensesCount ?: 0,
    activities = activities?.map { it.toDomain() } ?: emptyList(),
    expenses = expenses?.map { it.toDomain() } ?: emptyList(),
    documents = documents?.map { it.toDomain() } ?: emptyList()
)

fun BudgetAdviceDto.toDomain(): BudgetAdvice = BudgetAdvice(
    status = status.orEmpty(),
    message = message.orEmpty(),
    spentPercentage = spentPercentage ?: 0.0,
    totalExpenses = totalExpenses ?: 0.0,
    budget = budget ?: 0.0,
    daysElapsed = daysElapsed ?: 0,
    totalDays = totalDays ?: 0
)

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
