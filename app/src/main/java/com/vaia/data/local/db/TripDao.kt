package com.vaia.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TripDao {

    @Query("SELECT * FROM trips ORDER BY startDate ASC")
    suspend fun getAll(): List<TripEntity>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: String): TripEntity?

    // @Upsert y no @Insert(REPLACE): en SQLite REPLACE es DELETE + INSERT, así que
    // borraba la fila del viaje y el ON DELETE CASCADE se llevaba puestas sus
    // actividades y gastos cacheados en cada refresco. @Upsert hace UPDATE real.
    @Upsert
    suspend fun insertAll(trips: List<TripEntity>)

    @Upsert
    suspend fun insert(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Evict de viajes que ya no existen en el servidor, sin tocar los que siguen vigentes. */
    @Query("DELETE FROM trips WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<String>)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}
