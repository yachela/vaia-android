package com.vaia.data.repository

import com.vaia.data.api.CreateActivityRequest
import com.vaia.data.api.UpdateActivityRequest
import com.vaia.data.api.VaiaApiService
import com.vaia.data.local.db.TripDao
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
    private val activityDao: ActivityDao,
    private val tripDao: TripDao
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
                val code = response.code()
                val errorBody = try { response.errorBody()?.string() } catch (_: Exception) { null }
                android.util.Log.e("[API_DIAGNOSTIC]", "getActivitySuggestions falló en el servidor con código $code. Cuerpo de respuesta: $errorBody. Encabezados: ${response.headers()}")
                if (code == 503 || code >= 500) {
                    getLocalSuggestionsFallback(tripId)
                } else {
                    val errorMessage = parseApiError(errorBody, response.message())
                    Result.failure(ErrorLogger.logAndWrap("Activity", "getSuggestions", Exception(errorMessage), "No se pudieron obtener las sugerencias"))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("[API_DIAGNOSTIC]", "getActivitySuggestions lanzó una excepción: ${e.message}", e)
            getLocalSuggestionsFallback(tripId)
        }
    }

    private suspend fun getLocalSuggestionsFallback(tripId: String): Result<List<ActivitySuggestion>> {
        val entity = tripDao.getById(tripId)
        val destination = entity?.destination ?: ""
        val destNormalized = destination.trim().lowercase()
        val suggestions = when {
            destNormalized.contains("parís") || destNormalized.contains("paris") -> {
                listOf(
                    ActivitySuggestion("Paseo por Montmartre", "Recorrido a pie por el icónico barrio bohemio de los artistas y visita a la Basílica del Sagrado Corazón.", "Montmartre, París", 0.0, "10:00"),
                    ActivitySuggestion("Crucero por el Sena", "Paseo en barco de una hora con vistas espectaculares de la Torre Eiffel y la Catedral de Notre-Dame al atardecer.", "Bateaux Parisiens, París", 15.0, "19:00"),
                    ActivitySuggestion("Visita al Museo de Orsay", "Explora la mayor colección de obras impresionistas del mundo en una antigua y majestuosa estación ferroviaria.", "Museo de Orsay, París", 16.0, "14:00")
                )
            }
            destNormalized.contains("roma") -> {
                listOf(
                    ActivitySuggestion("Coliseo y Foro Romano", "Sumérgete en la historia antigua con una visita al anfiteatro más grande del Imperio Romano.", "Piazza del Colosseo, Roma", 18.0, "09:00"),
                    ActivitySuggestion("Fontana di Trevi y Panteón", "Camina por las plazas históricas del centro y cumple la tradición de lanzar una moneda en la Fontana.", "Piazza di Trevi, Roma", 0.0, "18:30"),
                    ActivitySuggestion("Cena en Trastevere", "Disfruta de pastas tradicionales y ambiente bohemio en las tradicionales osterias romanas.", "Trastevere, Roma", 25.0, "20:00")
                )
            }
            destNormalized.contains("nueva york") || destNormalized.contains("new york") || destNormalized.contains("ny") -> {
                listOf(
                    ActivitySuggestion("Explorar Central Park", "Relájate o recorre en bicicleta los caminos del parque urbano más emblemático de Manhattan.", "Central Park, Nueva York", 0.0, "10:00"),
                    ActivitySuggestion("Mirador Top of the Rock", "Disfruta de las mejores vistas panorámicas de 360 grados de Manhattan y el Empire State.", "Rockefeller Center, Nueva York", 40.0, "17:00"),
                    ActivitySuggestion("Cruce a pie del Puente de Brooklyn", "Camina por el puente colgante histórico y quédate a cenar en la zona de DUMBO.", "Puente de Brooklyn, Nueva York", 0.0, "18:00")
                )
            }
            else -> {
                listOf(
                    ActivitySuggestion("Free Walking Tour céntrico", "Una excelente forma de conocer la historia, cultura y principales puntos de la ciudad guiado por un experto local.", "Punto de encuentro céntrico", 0.0, "10:00"),
                    ActivitySuggestion("Mercado gastronómico local", "Degusta platos tradicionales, ingredientes frescos y especialidades regionales a precios de residente local.", "Mercado Central", 12.0, "13:30"),
                    ActivitySuggestion("Mirador principal de la ciudad", "Sube al punto elevado de referencia para capturar vistas panorámicas espectaculares durante la puesta de sol.", "Mirador local", 5.0, "18:00")
                )
            }
        }
        return Result.success(suggestions)
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
