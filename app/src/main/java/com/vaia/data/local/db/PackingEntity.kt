package com.vaia.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vaia.domain.model.*

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

fun PackingItemEntity.toPackingItem(): PackingItem = PackingItem(
    id = id,
    name = name,
    category = category,
    isPacked = isPacked,
    isSuggested = isSuggested,
    suggestionReason = suggestionReason,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PackingItem.toEntity(packingListId: String, syncStatus: String = "synced"): PackingItemEntity = PackingItemEntity(
    id = id,
    packingListId = packingListId,
    name = name,
    category = category,
    isPacked = isPacked,
    isSuggested = isSuggested,
    suggestionReason = suggestionReason,
    syncStatus = syncStatus,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PackingList.toEntity(syncStatus: String = "synced"): PackingListEntity = PackingListEntity(
    id = id,
    tripId = tripId,
    totalItems = progress.total,
    packedItems = progress.packed,
    syncStatus = syncStatus,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun PackingListEntity.toPackingList(itemEntities: List<PackingItemEntity>): PackingList {
    val items = itemEntities.map { it.toPackingItem() }
    val grouped = items.groupBy { it.category }
    val categories = grouped.map { (cat, list) -> PackingCategory(cat, list) }
    val total = itemEntities.size
    val packed = itemEntities.count { it.isPacked }
    val percentage = if (total > 0) (packed * 100) / total else 0
    return PackingList(
        id = id,
        tripId = tripId,
        itemsByCategory = categories,
        progress = PackingProgress(total, packed, percentage),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
