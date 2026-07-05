package com.vaia.data.api.dto

import com.google.gson.annotations.SerializedName
import com.vaia.domain.model.Expense

// DTOs de red para gastos. La capa domain no conoce Gson.

data class ExpenseDto(
    val id: String?,
    val amount: Double? = null,
    val description: String? = null,
    val date: String? = null,
    val category: String? = null,
    @SerializedName("receipt_image_url")
    val receiptImageUrl: String? = null
)

fun ExpenseDto.toDomain(): Expense = Expense(
    id = id.orEmpty(),
    amount = amount ?: 0.0,
    description = description.orEmpty(),
    date = date.orEmpty(),
    category = category.orEmpty(),
    receiptImageUrl = receiptImageUrl
)
