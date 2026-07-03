package com.vaia.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Expense(
    val id: String,
    val amount: Double,
    val description: String,
    val date: String,
    val category: String,
    val receiptImageUrl: String? = null
) : Parcelable
