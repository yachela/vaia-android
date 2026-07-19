package com.vaia.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vaia.domain.model.Expense

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId")]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val amount: Double? = 0.0,
    val description: String? = null,
    val date: String? = null,
    val category: String? = null,
    @ColumnInfo(name = "receipt_image_url") val receiptImageUrl: String? = null,
    @ColumnInfo(name = "sync_status") val syncStatus: String? = "synced" // "synced", "pending", "error"
)

fun ExpenseEntity.toExpense(): Expense = Expense(
    id = id,
    amount = amount ?: 0.0,
    description = description ?: "",
    date = date ?: "",
    category = category ?: "",
    receiptImageUrl = receiptImageUrl
)

fun Expense.toEntity(tripId: String, syncStatus: String = "synced"): ExpenseEntity = ExpenseEntity(
    id = id,
    tripId = tripId,
    amount = amount,
    description = description,
    date = date,
    category = category,
    receiptImageUrl = receiptImageUrl,
    syncStatus = syncStatus
)
