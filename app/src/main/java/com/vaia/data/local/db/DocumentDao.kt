package com.vaia.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DocumentDao {

    // Document operations
    @Query("SELECT * FROM documents WHERE tripId = :tripId")
    suspend fun getByTripId(tripId: String): List<DocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<DocumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM documents WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: String)

    // ChecklistItem operations
    @Query("SELECT * FROM checklist_items WHERE tripId = :tripId ORDER BY position ASC")
    suspend fun getChecklistItemsByTripId(tripId: String): List<ChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItemById(id: String)

    @Query("DELETE FROM checklist_items WHERE tripId = :tripId")
    suspend fun deleteChecklistItemsByTripId(tripId: String)
}
