package com.vaia.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ActivityDao {

    @Query("SELECT * FROM activities WHERE tripId = :tripId ORDER BY date ASC, time ASC")
    suspend fun getByTripId(tripId: String): List<ActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(activities: List<ActivityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM activities WHERE tripId = :tripId")
    suspend fun deleteByTripId(tripId: String)

    @Query("SELECT * FROM activities WHERE sync_status = 'pending'")
    suspend fun getPendingSync(): List<ActivityEntity>

    @Query("UPDATE activities SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
