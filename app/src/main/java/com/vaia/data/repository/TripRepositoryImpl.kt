package com.vaia.data.repository

import com.vaia.data.api.CreateTripRequest
import com.vaia.data.api.UpdateTripRequest
import com.vaia.data.api.VaiaApiService
import com.vaia.data.local.ErrorLogger
import com.vaia.data.local.db.TripDao
import com.vaia.data.local.db.toEntity
import com.vaia.data.local.db.toTrip
import com.vaia.domain.model.Trip
import com.vaia.domain.repository.TripRepository
import org.json.JSONObject

class TripRepositoryImpl(
    private val apiService: VaiaApiService,
    private val tripDao: TripDao
) : TripRepository {

    override suspend fun getTrips(): Result<List<Trip>> {
        return getTripsPage(1).map { (trips, _) -> trips }
    }

    override suspend fun getTripsPage(page: Int): Result<Pair<List<Trip>, Boolean>> {
        return try {
            val response = apiService.getTrips(page)
            if (response.isSuccessful) {
                val body = response.body()
                val trips = body?.data ?: emptyList()
                val meta = body?.meta
                val hasNextPage = (meta?.currentPage ?: 1) < (meta?.lastPage ?: 1)
                // Actualizar caché: reemplazar por completo en la página 1, acumular en las siguientes
                if (page == 1) tripDao.deleteAll()
                tripDao.insertAll(trips.map { it.toEntity() })
                Result.success(Pair(trips, hasNextPage))
            } else {
                // Fallback a caché en página 1 (offline)
                if (page == 1) {
                    val cached = tripDao.getAll()
                    if (cached.isNotEmpty()) return Result.success(Pair(cached.map { it.toTrip() }, false))
                }
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to get trips: $errorMessage"))
            }
        } catch (e: Exception) {
            // Sin red: devolver caché si está disponible
            if (page == 1) {
                val cached = tripDao.getAll()
                if (cached.isNotEmpty()) return Result.success(Pair(cached.map { it.toTrip() }, false))
            }
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "trips",
                    operation = "getTrips",
                    throwable = e,
                    defaultMessage = "No se pudieron obtener los viajes"
                )
            )
        }
    }

    override suspend fun getTrip(tripId: String): Result<Trip> {
        return try {
            val response = apiService.getTrip(tripId)
            if (response.isSuccessful) {
                response.body()?.data?.let { trip ->
                    Result.success(trip)
                } ?: Result.failure(Exception("No trip data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to get trip: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "trips",
                    operation = "getTrip",
                    throwable = e,
                    defaultMessage = "No se pudo obtener el viaje",
                    metadata = mapOf("tripId" to tripId)
                )
            )
        }
    }

    override suspend fun createTrip(title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> {
        return try {
            val request = CreateTripRequest(title, destination, startDate, endDate, budget)
            val response = apiService.createTrip(request)
            if (response.isSuccessful) {
                response.body()?.data?.let { trip ->
                    tripDao.insert(trip.toEntity())
                    Result.success(trip)
                } ?: Result.failure(Exception("No trip data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to create trip: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "trips",
                    operation = "createTrip",
                    throwable = e,
                    defaultMessage = "No se pudo crear el viaje",
                    metadata = mapOf("destination" to destination)
                )
            )
        }
    }

    override suspend fun updateTrip(tripId: String, title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> {
        return try {
            val request = UpdateTripRequest(title, destination, startDate, endDate, budget)
            val response = apiService.updateTrip(tripId, request)
            if (response.isSuccessful) {
                response.body()?.data?.let { trip ->
                    tripDao.insert(trip.toEntity())
                    Result.success(trip)
                } ?: Result.failure(Exception("No trip data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to update trip: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "trips",
                    operation = "updateTrip",
                    throwable = e,
                    defaultMessage = "No se pudo actualizar el viaje",
                    metadata = mapOf("tripId" to tripId)
                )
            )
        }
    }

    override suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            val response = apiService.deleteTrip(tripId)
            if (response.isSuccessful) {
                tripDao.deleteById(tripId)
                Result.success(Unit)
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to delete trip: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "trips",
                    operation = "deleteTrip",
                    throwable = e,
                    defaultMessage = "No se pudo eliminar el viaje",
                    metadata = mapOf("tripId" to tripId)
                )
            )
        }
    }

    override suspend fun exportItineraryPdf(tripId: String): Result<ByteArray> {
        return try {
            val response = apiService.exportItineraryPdf(tripId)
            if (response.isSuccessful) {
                val bytes = response.body()?.bytes()
                if (bytes != null) Result.success(bytes)
                else Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                Result.failure(Exception("No se pudo generar el PDF"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "trips",
                    operation = "exportItineraryPdf",
                    throwable = e,
                    defaultMessage = "No se pudo exportar el itinerario"
                )
            )
        }
    }

    override suspend fun exportExpensesCsv(tripId: String): Result<ByteArray> {
        return try {
            val response = apiService.exportExpensesCsv(tripId)
            if (response.isSuccessful) {
                val bytes = response.body()?.bytes()
                if (bytes != null) Result.success(bytes)
                else Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                Result.failure(Exception("No se pudo generar el CSV"))
            }
        } catch (e: Exception) {
            Result.failure(
                ErrorLogger.logAndWrap(
                    feature = "trips",
                    operation = "exportExpensesCsv",
                    throwable = e,
                    defaultMessage = "No se pudo exportar los gastos"
                )
            )
        }
    }

    private fun parseApiError(rawBody: String?, fallback: String): String {
        if (rawBody.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(rawBody)
            when {
                json.has("errors") -> {
                    val errors = json.optJSONObject("errors")
                    val firstField = errors?.keys()?.asSequence()?.firstOrNull()
                    val firstMessage = firstField
                        ?.let { key -> errors.optJSONArray(key)?.optString(0) }
                    firstMessage ?: json.optString("message", fallback)
                }
                json.has("message") -> json.optString("message", fallback)
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
