package com.vaia.data.repository

import com.vaia.data.api.AddPackingItemRequest
import com.vaia.data.api.VaiaApiService
import com.vaia.domain.model.PackingItem
import com.vaia.domain.model.PackingList
import com.vaia.domain.model.WeatherSuggestion
import com.vaia.domain.repository.PackingRepository
import javax.inject.Inject

class PackingRepositoryImpl @Inject constructor(
    private val apiService: VaiaApiService
) : PackingRepository {

    override suspend fun getPackingList(tripId: String): Result<PackingList> {
        return try {
            val response = apiService.getPackingList(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al obtener lista de equipaje"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generatePackingList(tripId: String): Result<PackingList> {
        return try {
            val response = apiService.generatePackingList(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al generar lista de equipaje"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWeatherSuggestions(tripId: String): Result<List<WeatherSuggestion>> {
        return try {
            val response = apiService.getWeatherSuggestions(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.suggestions)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al obtener sugerencias climáticas"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addPackingItem(tripId: String, name: String, category: String): Result<PackingItem> {
        return try {
            val request = AddPackingItemRequest(name, category)
            val response = apiService.addPackingItem(tripId, request)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.item)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al agregar ítem"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun togglePackingItem(itemId: String): Result<PackingItem> {
        return try {
            val response = apiService.togglePackingItem(itemId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.item)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Error al actualizar ítem"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePackingItem(itemId: String): Result<Unit> {
        return try {
            val response = apiService.deletePackingItem(itemId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error al eliminar ítem"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
