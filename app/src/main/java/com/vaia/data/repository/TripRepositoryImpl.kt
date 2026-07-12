package com.vaia.data.repository

import com.vaia.data.api.CreateTripRequest
import com.vaia.data.api.UpdateTripRequest
import com.vaia.data.api.VaiaApiService
import com.vaia.data.local.ErrorLogger
import com.vaia.data.local.db.TripDao
import com.vaia.data.local.db.toEntity
import com.vaia.data.local.db.toTrip
import com.vaia.domain.model.Trip
import com.vaia.domain.model.BudgetAdvice
import com.vaia.domain.repository.TripRepository
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                Result.failure(Exception("No se pudieron obtener los viajes: $errorMessage"))
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
                    tripDao.insert(trip.toEntity())
                    Result.success(trip)
                } ?: Result.failure(Exception("No trip data received"))
            } else {
                val cached = tripDao.getById(tripId)
                if (cached != null) {
                    Result.success(cached.toTrip())
                } else {
                    val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                    Result.failure(Exception("No se pudo obtener el viaje: $errorMessage"))
                }
            }
        } catch (e: Exception) {
            val cached = tripDao.getById(tripId)
            if (cached != null) {
                Result.success(cached.toTrip())
            } else {
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
                Result.failure(Exception("No se pudo crear el viaje: $errorMessage"))
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
                Result.failure(Exception("No se pudo actualizar el viaje: $errorMessage"))
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
                Result.failure(Exception("No se pudo eliminar el viaje: $errorMessage"))
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

    override suspend fun exportItineraryPdf(tripId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
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

    override suspend fun exportExpensesCsv(tripId: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
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

    override suspend fun getBudgetAdvice(tripId: String): Result<BudgetAdvice> {
        return try {
            val response = apiService.getBudgetAdvice(tripId)
            if (response.isSuccessful) {
                response.body()?.data?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("No se pudo obtener el consejo del presupuesto"))
            } else {
                val code = response.code()
                if (code == 503 || code >= 500) {
                    getLocalBudgetAdviceFallback(tripId)
                } else {
                    val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                    Result.failure(Exception("No se pudo obtener el consejo de presupuesto: $errorMessage"))
                }
            }
        } catch (e: Exception) {
            getLocalBudgetAdviceFallback(tripId)
        }
    }

    private suspend fun getLocalBudgetAdviceFallback(tripId: String): Result<BudgetAdvice> {
        val entity = tripDao.getById(tripId)
        if (entity != null) {
            val trip = entity.toTrip()
            val budget = trip.budget
            val totalExpenses = trip.totalExpenses

            // Calcular días del viaje
            val totalDays = try {
                val start = java.time.LocalDate.parse(trip.startDate)
                val end = java.time.LocalDate.parse(trip.endDate)
                java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
            } catch (_: Exception) {
                7
            }

            val daysElapsed = try {
                val start = java.time.LocalDate.parse(trip.startDate)
                val today = java.time.LocalDate.now()
                java.time.temporal.ChronoUnit.DAYS.between(start, today).toInt().coerceIn(0, totalDays)
            } catch (_: Exception) {
                1
            }

            val spentPercentage = if (budget > 0.0) (totalExpenses / budget) * 100.0 else 0.0

            val (status, message) = when {
                spentPercentage >= 100.0 -> {
                    Pair(
                        "critical",
                        "¡Alerta! Has superado el presupuesto asignado ($totalExpenses de $budget USD). Te recomendamos recortar los gastos no esenciales para el resto del viaje."
                    )
                }
                spentPercentage >= 85.0 -> {
                    Pair(
                        "critical",
                        "Presupuesto crítico: Has consumido el ${spentPercentage.toInt()}% del total ($totalExpenses de $budget USD). Intenta buscar actividades gratuitas y economizar en comidas."
                    )
                }
                spentPercentage >= 70.0 -> {
                    Pair(
                        "warning",
                        "Gastos elevados: Llevas consumido el ${spentPercentage.toInt()}% del presupuesto en $daysElapsed días. Te sugerimos revisar las compras impulsivas."
                    )
                }
                daysElapsed > 0 && (spentPercentage / daysElapsed) > (100.0 / totalDays) * 1.15 -> {
                    Pair(
                        "warning",
                        "Ritmo acelerado: Estás gastando más rápido de lo planificado por día. Ajusta tus gastos de transporte y comidas para mantener el presupuesto."
                    )
                }
                spentPercentage > 0.0 -> {
                    Pair(
                        "on_track",
                        "¡Buen trabajo! Tu ritmo de gasto del ${spentPercentage.toInt()}% en $daysElapsed días se encuentra dentro del rango saludable."
                    )
                }
                else -> {
                    Pair(
                        "on_track",
                        "Aún no registraste gastos para este viaje. ¡Comienza a cargar consumos para ver tu estado!"
                    )
                }
            }

            return Result.success(
                BudgetAdvice(
                    status = status,
                    message = message,
                    spentPercentage = spentPercentage,
                    totalExpenses = totalExpenses,
                    budget = budget,
                    daysElapsed = daysElapsed,
                    totalDays = totalDays
                )
            )
        }
        
        // Fallback genérico por si no se encuentra el viaje
        return Result.success(
            BudgetAdvice(
                status = "on_track",
                message = "Planifica y controla tus gastos diarios para optimizar tu presupuesto de viaje.",
                spentPercentage = 0.0,
                totalExpenses = 0.0,
                budget = 0.0,
                daysElapsed = 1,
                totalDays = 7
            )
        )
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
