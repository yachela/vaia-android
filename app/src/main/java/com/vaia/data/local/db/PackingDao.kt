package com.vaia.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PackingDao {
    
    // PackingList operations
    @Query("SELECT * FROM packing_lists WHERE trip_id = :tripId")
    fun getPackingListByTripId(tripId: String): Flow<PackingListEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackingList(packingList: PackingListEntity)
    
    @Update
    suspend fun updatePackingList(packingList: PackingListEntity)
    
    @Query("DELETE FROM packing_lists WHERE trip_id = :tripId")
    suspend fun deletePackingListByTripId(tripId: String)
    
    // PackingItem operations
    @Query("SELECT * FROM packing_items WHERE packing_list_id = :packingListId")
    fun getPackingItemsByListId(packingListId: String): Flow<List<PackingItemEntity>>
    
    @Query("SELECT * FROM packing_items WHERE id = :itemId")
    suspend fun getPackingItemById(itemId: String): PackingItemEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackingItem(item: PackingItemEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackingItems(items: List<PackingItemEntity>)
    
    @Update
    suspend fun updatePackingItem(item: PackingItemEntity)
    
    @Query("DELETE FROM packing_items WHERE id = :itemId")
    suspend fun deletePackingItem(itemId: String)
    
    @Query("DELETE FROM packing_items WHERE packing_list_id = :packingListId")
    suspend fun deletePackingItemsByListId(packingListId: String)
    
    // Sync operations
    @Query("SELECT * FROM packing_items WHERE sync_status = 'pending'")
    suspend fun getPendingPackingItems(): List<PackingItemEntity>
    
    @Query("UPDATE packing_items SET sync_status = :status WHERE id = :itemId")
    suspend fun updatePackingItemSyncStatus(itemId: String, status: String)
    
    @Query("SELECT * FROM packing_lists WHERE sync_status = 'pending'")
    suspend fun getPendingPackingLists(): List<PackingListEntity>
    
    @Query("UPDATE packing_lists SET sync_status = :status WHERE id = :listId")
    suspend fun updatePackingListSyncStatus(listId: String, status: String)
}
