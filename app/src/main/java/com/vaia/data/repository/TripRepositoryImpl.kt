package com.vaia.data.repository

import com.vaia.data.api.CreateTripRequest
import com.vaia.data.api.UpdateTripRequest
import com.vaia.data.api.VaiaApiService
import com.vaia.domain.model.Trip
import com.vaia.domain.repository.TripRepository
import org.json.JSONObject

class TripRepositoryImpl(
    private val apiService: VaiaApiService
) : TripRepository {

    override suspend fun getTrips(): Result<List<Trip>> {
        return try {
            val response = apiService.getTrips()
            if (response.isSuccessful) {
                response.body()?.data?.let { trips ->
                    Result.success(trips)
                } ?: Result.failure(Exception("No trips data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to get trips: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
            Result.failure(e)
        }
    }

    override suspend fun createTrip(title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> {
        return try {
            val request = CreateTripRequest(title, destination, startDate, endDate, budget)
            val response = apiService.createTrip(request)
            if (response.isSuccessful) {
                response.body()?.data?.let { trip ->
                    Result.success(trip)
                } ?: Result.failure(Exception("No trip data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to create trip: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTrip(tripId: String, title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> {
        return try {
            val request = UpdateTripRequest(title, destination, startDate, endDate, budget)
            val response = apiService.updateTrip(tripId, request)
            if (response.isSuccessful) {
                response.body()?.data?.let { trip ->
                    Result.success(trip)
                } ?: Result.failure(Exception("No trip data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to update trip: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTrip(tripId: String): Result<Unit> {
        return try {
            val response = apiService.deleteTrip(tripId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to delete trip: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
