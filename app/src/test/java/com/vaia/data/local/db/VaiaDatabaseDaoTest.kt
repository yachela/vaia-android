package com.vaia.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests de los DAOs de Room sobre una base en memoria (Robolectric).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VaiaDatabaseDaoTest {

    private lateinit var db: VaiaDatabase
    private lateinit var tripDao: TripDao
    private lateinit var activityDao: ActivityDao
    private lateinit var packingDao: PackingDao
    private lateinit var documentDao: DocumentDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, VaiaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        tripDao = db.tripDao()
        activityDao = db.activityDao()
        packingDao = db.packingDao()
        documentDao = db.documentDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun trip(id: String, startDate: String = "2026-04-15") = TripEntity(
        id = id,
        title = "Viaje $id",
        destination = "Destino",
        startDate = startDate,
        endDate = "2026-04-22",
        budget = 1000.0
    )

    private fun activity(
        id: String,
        tripId: String,
        date: String = "2026-04-16",
        time: String = "10:00",
        syncStatus: String = "synced"
    ) = ActivityEntity(
        id = id,
        tripId = tripId,
        title = "Actividad $id",
        description = "",
        date = date,
        time = time,
        location = "",
        cost = 0.0,
        syncStatus = syncStatus
    )

    // ── TripDao ───────────────────────────────────────────────────────────────

    @Test
    fun `tripDao inserta y ordena por fecha de inicio`() = runBlocking {
        tripDao.insertAll(listOf(trip("t2", "2026-06-01"), trip("t1", "2026-04-15")))

        val all = tripDao.getAll()

        assertEquals(listOf("t1", "t2"), all.map { it.id })
    }

    @Test
    fun `tripDao reemplaza en conflicto de id`() = runBlocking {
        tripDao.insert(trip("t1"))
        tripDao.insert(trip("t1").copy(title = "Actualizado"))

        assertEquals("Actualizado", tripDao.getById("t1")?.title)
        assertEquals(1, tripDao.getAll().size)
    }

    @Test
    fun `tripDao elimina por id y elimina todo`() = runBlocking {
        tripDao.insertAll(listOf(trip("t1"), trip("t2")))

        tripDao.deleteById("t1")
        assertNull(tripDao.getById("t1"))
        assertEquals(1, tripDao.getAll().size)

        tripDao.deleteAll()
        assertTrue(tripDao.getAll().isEmpty())
    }

    // ── ActivityDao ───────────────────────────────────────────────────────────

    @Test
    fun `activityDao filtra por viaje y ordena por fecha y hora`() = runBlocking {
        tripDao.insert(trip("t1"))
        tripDao.insert(trip("t2"))
        activityDao.insertAll(
            listOf(
                activity("a2", "t1", date = "2026-04-17", time = "09:00"),
                activity("a1", "t1", date = "2026-04-16", time = "20:00"),
                activity("a3", "t1", date = "2026-04-17", time = "08:00"),
                activity("a4", "t2")
            )
        )

        val activities = activityDao.getByTripId("t1")

        assertEquals(listOf("a1", "a3", "a2"), activities.map { it.id })
    }

    @Test
    fun `activityDao devuelve las pendientes de sincronizar y actualiza su estado`() = runBlocking {
        tripDao.insert(trip("t1"))
        activityDao.insertAll(
            listOf(
                activity("a1", "t1", syncStatus = "pending"),
                activity("a2", "t1", syncStatus = "synced")
            )
        )

        assertEquals(listOf("a1"), activityDao.getPendingSync().map { it.id })

        activityDao.updateSyncStatus("a1", "synced")
        assertTrue(activityDao.getPendingSync().isEmpty())
    }

    @Test
    fun `al borrar un viaje se borran sus actividades en cascada`() = runBlocking {
        tripDao.insert(trip("t1"))
        activityDao.insert(activity("a1", "t1"))

        tripDao.deleteById("t1")

        assertTrue(activityDao.getByTripId("t1").isEmpty())
    }

    // ── PackingDao ────────────────────────────────────────────────────────────

    @Test
    fun `packingDao guarda listas e ítems y consulta por lista`() = runBlocking {
        val list = PackingListEntity(
            id = "pl1",
            tripId = "t1",
            totalItems = 2,
            packedItems = 1,
            syncStatus = "synced",
            createdAt = "2026-01-01",
            updatedAt = "2026-01-01"
        )
        packingDao.insertPackingList(list)
        packingDao.insertPackingItems(
            listOf(
                packingItem("p1", "pl1", isPacked = true),
                packingItem("p2", "pl1"),
                packingItem("p3", "otra-lista")
            )
        )

        assertEquals("pl1", packingDao.getPackingListByTripIdSync("t1")?.id)
        assertEquals(2, packingDao.getPackingItemsByListIdSync("pl1").size)
    }

    @Test
    fun `packingDao consulta pendientes y actualiza estado de sincronización`() = runBlocking {
        packingDao.insertPackingItem(packingItem("p1", "pl1", syncStatus = "pending"))
        packingDao.insertPackingItem(packingItem("p2", "pl1", syncStatus = "synced"))

        assertEquals(listOf("p1"), packingDao.getPendingPackingItems().map { it.id })

        packingDao.updatePackingItemSyncStatus("p1", "synced")
        assertTrue(packingDao.getPendingPackingItems().isEmpty())
    }

    @Test
    fun `packingDao elimina ítems por id`() = runBlocking {
        packingDao.insertPackingItem(packingItem("p1", "pl1"))

        packingDao.deletePackingItem("p1")

        assertNull(packingDao.getPackingItemById("p1"))
    }

    private fun packingItem(
        id: String,
        listId: String,
        isPacked: Boolean = false,
        syncStatus: String = "synced"
    ) = PackingItemEntity(
        id = id,
        packingListId = listId,
        name = "Item $id",
        category = "ROPA",
        isPacked = isPacked,
        isSuggested = false,
        suggestionReason = null,
        syncStatus = syncStatus,
        createdAt = "2026-01-01",
        updatedAt = "2026-01-01"
    )

    // ── DocumentDao ───────────────────────────────────────────────────────────

    @Test
    fun `documentDao guarda y consulta documentos por viaje`() = runBlocking {
        val doc = DocumentEntity(
            id = "d1",
            tripId = "t1",
            userId = "u1",
            filePath = "documents/d1.pdf",
            fileName = "pasaporte.pdf",
            mimeType = "application/pdf",
            fileSize = 1024L,
            description = null,
            category = "id"
        )
        documentDao.insert(doc)
        documentDao.insert(doc.copy(id = "d2", tripId = "t2"))

        assertEquals(listOf("d1"), documentDao.getByTripId("t1").map { it.id })

        documentDao.deleteByTripId("t1")
        assertTrue(documentDao.getByTripId("t1").isEmpty())
    }
}
