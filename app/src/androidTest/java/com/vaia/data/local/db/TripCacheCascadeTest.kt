package com.vaia.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regresión: refrescar la lista de viajes borraba en cascada las actividades y
 * gastos cacheados de todos los viajes, dejando la app sin datos que mostrar
 * al quedarse sin conexión.
 *
 * Las dos causas eran `deleteAll()` en la página 1 y `@Insert(REPLACE)`, que en
 * SQLite es DELETE + INSERT y por lo tanto dispara el ON DELETE CASCADE.
 */
@RunWith(AndroidJUnit4::class)
class TripCacheCascadeTest {

    private lateinit var db: VaiaDatabase
    private lateinit var tripDao: TripDao
    private lateinit var activityDao: ActivityDao
    private lateinit var expenseDao: ExpenseDao

    private val tripId = "trip-1"

    private fun trip(id: String = tripId, title: String = "Roma") = TripEntity(
        id = id,
        title = title,
        destination = "Roma, Italia",
        startDate = "2026-06-01",
        endDate = "2026-06-10",
        budget = 2000.0
    )

    private fun activity(id: String = "act-1") = ActivityEntity(
        id = id,
        tripId = tripId,
        title = "Coliseo",
        date = "2026-06-01"
    )

    private fun expense(id: String = "exp-1") = ExpenseEntity(
        id = id,
        tripId = tripId,
        amount = 100.0,
        description = "Cena"
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            VaiaDatabase::class.java
        ).build()
        tripDao = db.tripDao()
        activityDao = db.activityDao()
        expenseDao = db.expenseDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun refrescar_un_viaje_existente_no_borra_sus_actividades_ni_gastos() = runBlocking {
        tripDao.insert(trip())
        activityDao.insert(activity())
        expenseDao.insert(expense())

        // Mismo viaje con datos actualizados, como al refrescar desde la API
        tripDao.insertAll(listOf(trip(title = "Roma (editado)")))

        assertEquals(1, activityDao.getByTripId(tripId).size)
        assertEquals(1, expenseDao.getByTripId(tripId).size)
        assertEquals("Roma (editado)", tripDao.getById(tripId)?.title)
    }

    @Test
    fun evict_selectivo_conserva_los_hijos_de_los_viajes_vigentes() = runBlocking {
        tripDao.insertAll(listOf(trip(), trip(id = "trip-2", title = "París")))
        activityDao.insert(activity())
        expenseDao.insert(expense())

        // La API devuelve solo trip-1: trip-2 se evicta, trip-1 sobrevive
        tripDao.deleteNotIn(listOf(tripId))

        assertEquals(1, tripDao.getAll().size)
        assertEquals(1, activityDao.getByTripId(tripId).size)
        assertEquals(1, expenseDao.getByTripId(tripId).size)
    }

    @Test
    fun borrar_un_viaje_si_arrastra_sus_hijos() = runBlocking {
        tripDao.insert(trip())
        activityDao.insert(activity())
        expenseDao.insert(expense())

        tripDao.deleteById(tripId)

        assertEquals(0, activityDao.getByTripId(tripId).size)
        assertEquals(0, expenseDao.getByTripId(tripId).size)
    }
}
