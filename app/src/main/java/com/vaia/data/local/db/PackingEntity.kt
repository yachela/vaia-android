package com.vaia.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "packing_lists")
data class PackingListEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "trip_id")
    val tripId: String,
    @ColumnInfo(name = "total_items")
    val totalItems: Int,
    @ColumnInfo(name = "packed_items")
    val packedItems: Int,
    @ColumnInfo(name = "sync_status")
    val syncStatus: String, // pending, synced, error
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)

@Entity(tableName = "packing_items")
data class PackingItemEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "packing_list_id")
    val packingListId: String,
    val name: String,
    val category: String,
    @ColumnInfo(name = "is_packed")
    val isPacked: Boolean,
    @ColumnInfo(name = "is_suggested")
    val isSuggested: Boolean,
    @ColumnInfo(name = "suggestion_reason")
    val suggestionReason: String?,
    @ColumnInfo(name = "sync_status")
    val syncStatus: String, // pending, synced, error
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
