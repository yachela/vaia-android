package com.vaia.data.repository

import com.vaia.data.api.CreateActivityRequest
import com.vaia.data.api.UpdateActivityRequest
import com.vaia.data.api.VaiaApiService
import com.vaia.data.local.db.ActivityDao
import com.vaia.data.local.db.toActivity
import com.vaia.data.local.db.toEntity
import com.vaia.domain.model.Activity
import com.vaia.domain.model.ActivitySuggestion
import com.vaia.domain.repository.ActivityRepository
import com.vaia.data.local.ErrorLogger
import org.json.JSONObject

class ActivityRepositoryImpl(
    private val apiService: VaiaApiService,
    private val activityDao: ActivityDao
) : ActivityRepository {

    override suspend fun getActivities(tripId: String): Result<List<Activity>> {
        return try {
            val response = apiService.getActivities(tripId)
            if (response.isSuccessful) {
                val rawActivities = response.body()?.data ?: emptyList()
                val activities = rawActivities.map { it.copy(
                    title = (it.title as Any?)?.toString() ?: "Actividad sin título",
                    description = (it.description as Any?)?.toString() ?: "",
                    date = (it.date as Any?)?.toString() ?: "",
                    location = (it.location as Any?)?.toString() ?: "",
                    time = (it.time as Any?)?.toString() ?: ""
                )}
                activityDao.deleteByTripId(tripId)
                activityDao.insertAll(activities.map { it.toEntity(tripId) })
                Result.success(activities)
            } else {
                val cached = activityDao.getByTripId(tripId)
                if (cached.isNotEmpty()) return Result.success(cached.map { it.toActivity() })
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("No se pudieron obtener las actividades: $errorMessage"))
            }
        } catch (e: Exception) {
            val cached = activityDao.getByTripId(tripId)
            if (cached.isNotEmpty()) return Result.success(cached.map { it.toActivity() })
            Result.failure(ErrorLogger.logAndWrap("Activity", "getActivities", e, "No se pudieron obtener las actividades"))
        }
    }

    override suspend fun getActivity(tripId: String, activityId: String): Result<Activity> {
        return try {
            val response = apiService.getActivity(tripId, activityId)
            if (response.isSuccessful) {
                response.body()?.data?.let { rawActivity ->
                    val activity = rawActivity.copy(
                        title = (rawActivity.title as Any?)?.toString() ?: "Actividad sin título",
                        description = (rawActivity.description as Any?)?.toString() ?: "",
                        date = (rawActivity.date as Any?)?.toString() ?: "",
                        location = (rawActivity.location as Any?)?.toString() ?: "",
                        time = (rawActivity.time as Any?)?.toString() ?: ""
                    )
                    activityDao.insert(activity.toEntity(tripId))
                    Result.success(activity)
                } ?: Result.failure(Exception("No activity data received"))
            } else {
                val cached = activityDao.getById(activityId)
                if (cached != null) {
                    Result.success(cached.toActivity())
                } else {
                    val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                    Result.failure(Exception("No se pudo obtener la actividad: $errorMessage"))
                }
            }
        } catch (e: Exception) {
            val cached = activityDao.getById(activityId)
            if (cached != null) {
                Result.success(cached.toActivity())
            } else {
                Result.failure(ErrorLogger.logAndWrap("Activity", "getActivity", e, "No se pudo obtener la actividad"))
            }
        }
    }

    override suspend fun createActivity(tripId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> {
        return try {
            val request = CreateActivityRequest(title, description, date, time.takeIf { it.isNotBlank() }, location, cost)
            val response = apiService.createActivity(tripId, request)
            if (response.isSuccessful) {
                response.body()?.data?.let { activity ->
                    activityDao.insert(activity.toEntity(tripId))
                    Result.success(activity)
                } ?: Result.failure(Exception("No activity data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("No se pudo crear la actividad: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorLogger.logAndWrap("Activity", "createActivity", e, "No se pudo crear la actividad"))
        }
    }

    override suspend fun updateActivity(tripId: String, activityId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> {
        return try {
            val request = UpdateActivityRequest(title, description, date, time.takeIf { it.isNotBlank() }, location, cost)
            val response = apiService.updateActivity(tripId, activityId, request)
            if (response.isSuccessful) {
                response.body()?.data?.let { activity ->
                    activityDao.insert(activity.toEntity(tripId))
                    Result.success(activity)
                } ?: Result.failure(Exception("No activity data received"))
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("No se pudo actualizar la actividad: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorLogger.logAndWrap("Activity", "updateActivity", e, "No se pudo actualizar la actividad"))
        }
    }

    override suspend fun deleteActivity(tripId: String, activityId: String): Result<Unit> {
        return try {
            val response = apiService.deleteActivity(tripId, activityId)
            if (response.isSuccessful) {
                activityDao.deleteById(activityId)
                Result.success(Unit)
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(Exception("No se pudo eliminar la actividad: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorLogger.logAndWrap("Activity", "deleteActivity", e, "No se pudo eliminar la actividad"))
        }
    }

    override suspend fun getSuggestions(tripId: String): Result<List<ActivitySuggestion>> {
        return try {
            val response = apiService.getActivitySuggestions(tripId)
            if (response.isSuccessful) {
                Result.success(response.body()?.data ?: emptyList())
            } else {
                val errorMessage = parseApiError(response.errorBody()?.string(), response.message())
                Result.failure(ErrorLogger.logAndWrap("Activity", "getSuggestions", Exception(errorMessage), "No se pudieron obtener las sugerencias"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorLogger.logAndWrap("Activity", "getSuggestions", e, "No se pudieron obtener las sugerencias"))
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
