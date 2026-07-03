package com.vaia.data.repository

import com.vaia.data.api.VaiaApiService
import com.vaia.data.local.db.TripDao
import com.vaia.data.local.db.TripEntity
import com.vaia.data.local.db.toEntity
import com.vaia.domain.model.AppError
import com.vaia.domain.model.Trip
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TripRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: TripRepositoryImpl
    private lateinit var tripDao: FakeTripDao

    private class FakeTripDao : TripDao {
        val trips = linkedMapOf<String, TripEntity>()
        override suspend fun getAll(): List<TripEntity> = trips.values.toList()
        override suspend fun getById(id: String): TripEntity? = trips[id]
        override suspend fun insertAll(trips: List<TripEntity>) {
            trips.forEach { this.trips[it.id] = it }
        }
        override suspend fun insert(trip: TripEntity) { trips[trip.id] = trip }
        override suspend fun deleteById(id: String) { trips.remove(id) }
        override suspend fun deleteAll() { trips.clear() }
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VaiaApiService::class.java)
        tripDao = FakeTripDao()
        repository = TripRepositoryImpl(apiService, tripDao)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `getTripsPage mapea los campos snake_case del backend`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "data": [
                        {
                            "id": "t1",
                            "title": "Viaje a París",
                            "destination": "París, Francia",
                            "start_date": "2026-04-15",
                            "end_date": "2026-04-22",
                            "budget": 2500.0,
                            "total_expenses": 1850.5,
                            "activities_count": 6,
                            "expenses_count": 4
                        }
                    ],
                    "meta": {"current_page": 1, "last_page": 3, "total": 30, "per_page": 15}
                }
                """.trimIndent()
            )
        )

        val result = repository.getTripsPage(1)

        assertTrue(result.isSuccess)
        val (trips, hasNext) = result.getOrThrow()
        assertEquals(1, trips.size)
        val trip = trips.first()
        assertEquals("t1", trip.id)
        assertEquals("2026-04-15", trip.startDate)
        assertEquals("2026-04-22", trip.endDate)
        assertEquals(1850.5, trip.totalExpenses, 0.0)
        assertEquals(6, trip.activitiesCount)
        assertTrue(hasNext)
        // Actualiza el caché local
        assertEquals(1, tripDao.trips.size)
    }

    @Test
    fun `getTripsPage sin más páginas devuelve hasNext false`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data": [], "meta": {"current_page": 2, "last_page": 2, "total": 20, "per_page": 15}}"""
            )
        )

        val result = repository.getTripsPage(2)

        assertFalse(result.getOrThrow().second)
    }

    @Test
    fun `getTripsPage con 500 y caché disponible devuelve el caché`() = runTest {
        val cached = Trip(
            id = "t-cache",
            title = "Cacheado",
            destination = "Roma",
            startDate = "2026-06-01",
            endDate = "2026-06-10",
            budget = 1000.0
        )
        tripDao.insert(cached.toEntity())
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        val result = repository.getTripsPage(1)

        assertTrue(result.isSuccess)
        assertEquals(listOf("t-cache"), result.getOrThrow().first.map { it.id })
    }

    @Test
    fun `getTripsPage con 500 y sin caché devuelve Unknown`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(500)
                .setBody("""{"message": "Error interno."}""")
        )

        val result = repository.getTripsPage(1)

        val error = result.exceptionOrNull()
        assertTrue(error is AppError.Unknown)
        assertEquals("Error interno.", error?.message)
    }

    @Test
    fun `createTrip con 422 devuelve Validation con el mensaje del backend`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(422).setBody(
                """
                {
                    "message": "Los datos son inválidos.",
                    "errors": {"start_date": ["La fecha de inicio debe ser posterior a hoy."]}
                }
                """.trimIndent()
            )
        )

        val result = repository.createTrip("X", "Y", "2020-01-01", "2020-01-02", 0.0)

        val error = result.exceptionOrNull()
        assertTrue(error is AppError.Validation)
        assertEquals("La fecha de inicio debe ser posterior a hoy.", error?.message)
    }

    @Test
    fun `createTrip exitoso guarda el viaje en el caché`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                    "data": {
                        "id": "t-nuevo",
                        "title": "Nuevo",
                        "destination": "Bali",
                        "start_date": "2026-08-15",
                        "end_date": "2026-08-25",
                        "budget": 4000.0
                    }
                }
                """.trimIndent()
            )
        )

        val result = repository.createTrip("Nuevo", "Bali", "2026-08-15", "2026-08-25", 4000.0)

        assertEquals("t-nuevo", result.getOrThrow().id)
        assertTrue("t-nuevo" in tripDao.trips)
    }

    @Test
    fun `getTrip con 401 y sin caché devuelve Unauthorized`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"message": "Unauthenticated."}""")
        )

        val result = repository.getTrip("t-x")

        assertTrue(result.exceptionOrNull() is AppError.Unauthorized)
    }
}
