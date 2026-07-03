package com.vaia.data.repository

import com.vaia.data.api.VaiaApiService
import com.vaia.data.api.dto.CreateActivityRequest
import com.vaia.data.api.dto.UpdateActivityRequest
import com.vaia.data.api.dto.toDomain
import com.vaia.data.local.db.ActivityDao
import com.vaia.data.network.ErrorMapper
import com.vaia.data.local.db.toActivity
import com.vaia.data.local.db.toEntity
import com.vaia.domain.model.Activity
import com.vaia.domain.model.ActivitySuggestion
import com.vaia.domain.repository.ActivityRepository

class ActivityRepositoryImpl(
    private val apiService: VaiaApiService,
    private val activityDao: ActivityDao
) : ActivityRepository {

    override suspend fun getActivities(tripId: String): Result<List<Activity>> {
        return try {
            val response = apiService.getActivities(tripId)
            if (response.isSuccessful) {
                val activities = response.body()?.data?.map { it.toDomain() } ?: emptyList()
                activityDao.deleteByTripId(tripId)
                activityDao.insertAll(activities.map { it.toEntity(tripId) })
                Result.success(activities)
            } else {
                val cached = activityDao.getByTripId(tripId)
                if (cached.isNotEmpty()) return Result.success(cached.map { it.toActivity() })
                Result.failure(ErrorMapper.fromResponse(response, "No se pudieron obtener las actividades"))
            }
        } catch (e: Exception) {
            val cached = activityDao.getByTripId(tripId)
            if (cached.isNotEmpty()) return Result.success(cached.map { it.toActivity() })
            Result.failure(e)
        }
    }

    override suspend fun getActivity(tripId: String, activityId: String): Result<Activity> {
        return try {
            val response = apiService.getActivity(tripId, activityId)
            if (response.isSuccessful) {
                response.body()?.data?.let { activityDto ->
                    val activity = activityDto.toDomain()
                    activityDao.insert(activity.toEntity(tripId))
                    Result.success(activity)
                } ?: Result.failure(Exception("No activity data received"))
            } else {
                val cached = activityDao.getById(activityId)
                if (cached != null) {
                    Result.success(cached.toActivity())
                } else {
                    Result.failure(ErrorMapper.fromResponse(response, "No se pudo obtener la actividad"))
                }
            }
        } catch (e: Exception) {
            val cached = activityDao.getById(activityId)
            if (cached != null) {
                Result.success(cached.toActivity())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun createActivity(tripId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> {
        return try {
            val request = CreateActivityRequest(title, description, date, time.takeIf { it.isNotBlank() }, location, cost)
            val response = apiService.createActivity(tripId, request)
            if (response.isSuccessful) {
                response.body()?.data?.let { activityDto ->
                    val activity = activityDto.toDomain()
                    activityDao.insert(activity.toEntity(tripId))
                    Result.success(activity)
                } ?: Result.failure(Exception("No activity data received"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo crear la actividad"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun updateActivity(tripId: String, activityId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> {
        return try {
            val request = UpdateActivityRequest(title, description, date, time.takeIf { it.isNotBlank() }, location, cost)
            val response = apiService.updateActivity(tripId, activityId, request)
            if (response.isSuccessful) {
                response.body()?.data?.let { activityDto ->
                    val activity = activityDto.toDomain()
                    activityDao.insert(activity.toEntity(tripId))
                    Result.success(activity)
                } ?: Result.failure(Exception("No activity data received"))
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo actualizar la actividad"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun deleteActivity(tripId: String, activityId: String): Result<Unit> {
        return try {
            val response = apiService.deleteActivity(tripId, activityId)
            if (response.isSuccessful) {
                activityDao.deleteById(activityId)
                Result.success(Unit)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo eliminar la actividad"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun getSuggestions(tripId: String): Result<List<ActivitySuggestion>> {
        return try {
            val response = apiService.getActivitySuggestions(tripId)
            if (response.isSuccessful) {
                Result.success(response.body()?.data?.map { it.toDomain() } ?: emptyList())
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudieron obtener las sugerencias"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

}
