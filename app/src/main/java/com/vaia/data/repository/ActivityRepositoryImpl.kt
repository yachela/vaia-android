package com.vaia.data.repository

import com.vaia.data.api.CreateActivityRequest
import com.vaia.data.api.UpdateActivityRequest
import com.vaia.data.api.VaiaApiService
import com.vaia.domain.model.Activity
import com.vaia.domain.repository.ActivityRepository
import org.json.JSONObject

class ActivityRepositoryImpl(
    private val apiService: VaiaApiService
) : ActivityRepository {

    override suspend fun getActivities(tripId: String): Result<List<Activity>> {
        return try {
            val response = apiService.getActivities(tripId)
            if (response.isSuccessful) {
                response.body()?.data?.let { activities ->
                    Result.success(activities)
                } ?: Result.failure(Exception("No activities data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to get activities: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActivity(tripId: String, activityId: String): Result<Activity> {
        return try {
            val response = apiService.getActivity(tripId, activityId)
            if (response.isSuccessful) {
                response.body()?.data?.let { activity ->
                    Result.success(activity)
                } ?: Result.failure(Exception("No activity data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to get activity: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createActivity(tripId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> {
        return try {
            val request = CreateActivityRequest(title, description, date, time.takeIf { it.isNotBlank() }, location, cost)
            val response = apiService.createActivity(tripId, request)
            if (response.isSuccessful) {
                response.body()?.data?.let { activity ->
                    Result.success(activity)
                } ?: Result.failure(Exception("No activity data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to create activity: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateActivity(tripId: String, activityId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> {
        return try {
            val request = UpdateActivityRequest(title, description, date, time.takeIf { it.isNotBlank() }, location, cost)
            val response = apiService.updateActivity(tripId, activityId, request)
            if (response.isSuccessful) {
                response.body()?.data?.let { activity ->
                    Result.success(activity)
                } ?: Result.failure(Exception("No activity data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to update activity: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteActivity(tripId: String, activityId: String): Result<Unit> {
        return try {
            val response = apiService.deleteActivity(tripId, activityId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("Failed to delete activity: $errorMessage"))
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
                    val firstMessage = firstField?.let { key -> errors.optJSONArray(key)?.optString(0) }
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
