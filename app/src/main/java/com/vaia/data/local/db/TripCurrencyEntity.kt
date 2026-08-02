package com.vaia.data.local.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * Moneda elegida para el presupuesto de un viaje.
 *
 * Va en su propia tabla y no como columna de [TripEntity] a propósito: los viajes
 * se reescriben enteros en cada refresco desde la API, que todavía no devuelve la
 * moneda, así que la columna quedaría pisada por el default en cada sincronización.
 * Sin FK ni CASCADE por lo mismo: el evict de caché no debe perder la elección.
 */
@Entity(tableName = "trip_currencies")
data class TripCurrencyEntity(
    @PrimaryKey val tripId: String,
    val currency: String
)

@Dao
interface TripCurrencyDao {

    @Query("SELECT * FROM trip_currencies")
    suspend fun getAll(): List<TripCurrencyEntity>

    @Query("SELECT currency FROM trip_currencies WHERE tripId = :tripId")
    suspend fun getCurrency(tripId: String): String?

    @Upsert
    suspend fun upsert(entity: TripCurrencyEntity)

    @Query("DELETE FROM trip_currencies WHERE tripId = :tripId")
    suspend fun deleteById(tripId: String)
}
