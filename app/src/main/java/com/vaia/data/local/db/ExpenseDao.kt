package com.vaia.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE tripId = :tripId ORDER BY date DESC")
    suspend fun getByTripId(tripId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM expenses WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: String)

    @Query("SELECT * FROM expenses WHERE sync_status = 'pending'")
    suspend fun getPendingSync(): List<ExpenseEntity>

    @Query("UPDATE expenses SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
