package com.vaia.data.repository

import com.vaia.data.api.VaiaApiService
import com.vaia.data.api.dto.AddPackingItemRequest
import com.vaia.data.api.dto.toDomain
import com.vaia.data.network.ErrorMapper
import com.vaia.data.local.db.*
import com.vaia.domain.model.PackingItem
import com.vaia.domain.model.PackingList
import com.vaia.domain.model.WeatherSuggestion
import com.vaia.domain.repository.PackingRepository
import javax.inject.Inject

class PackingRepositoryImpl @Inject constructor(
    private val apiService: VaiaApiService,
    private val packingDao: PackingDao
) : PackingRepository {

    override suspend fun getPackingList(tripId: String): Result<PackingList> {
        return try {
            val response = apiService.getPackingList(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                val packingList = response.body()!!.data!!.toDomain()
                packingDao.insertPackingList(packingList.toEntity())
                val itemEntities = packingList.itemsByCategory.flatMap { category ->
                    category.items.map { it.toEntity(packingList.id) }
                }
                packingDao.insertPackingItems(itemEntities)
                Result.success(packingList)
            } else {
                val cachedList = packingDao.getPackingListByTripIdSync(tripId)
                if (cachedList != null) {
                    val cachedItems = packingDao.getPackingItemsByListIdSync(cachedList.id)
                    Result.success(cachedList.toPackingList(cachedItems))
                } else {
                    Result.failure(ErrorMapper.fromResponse(response, "No se pudo obtener la lista de equipaje"))
                }
            }
        } catch (e: Exception) {
            val cachedList = packingDao.getPackingListByTripIdSync(tripId)
            if (cachedList != null) {
                val cachedItems = packingDao.getPackingItemsByListIdSync(cachedList.id)
                Result.success(cachedList.toPackingList(cachedItems))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun generatePackingList(tripId: String): Result<PackingList> {
        return try {
            val response = apiService.generatePackingList(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                val packingList = response.body()!!.data!!.toDomain()
                packingDao.insertPackingList(packingList.toEntity())
                val itemEntities = packingList.itemsByCategory.flatMap { category ->
                    category.items.map { it.toEntity(packingList.id) }
                }
                packingDao.insertPackingItems(itemEntities)
                Result.success(packingList)
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo generar la lista de equipaje"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun getWeatherSuggestions(tripId: String): Result<List<WeatherSuggestion>> {
        return try {
            val response = apiService.getWeatherSuggestions(tripId)
            if (response.isSuccessful && response.body()?.data != null) {
                Result.success(response.body()!!.data!!.suggestions.map { it.toDomain() })
            } else {
                Result.failure(ErrorMapper.fromResponse(response, "No se pudieron obtener las sugerencias climáticas"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun addPackingItem(tripId: String, name: String, category: String): Result<PackingItem> {
        return try {
            // Crear el ítem localmente de inmediato
            val now = java.time.Instant.now().toString()
            val localItem = PackingItem(
                id = "local-item-${System.currentTimeMillis()}",
                name = name,
                category = category,
                isPacked = false,
                isSuggested = false,
                suggestionReason = null,
                createdAt = now,
                updatedAt = now
            )
            val cachedList = packingDao.getPackingListByTripIdSync(tripId)
            val listId = cachedList?.id ?: ""
            packingDao.insertPackingItem(localItem.toEntity(listId, "pending"))

            // Intentar sincronizar con la API en background (fallo silencioso)
            try {
                val request = AddPackingItemRequest(name, category)
                val response = apiService.addPackingItem(tripId, request)
                if (response.isSuccessful && response.body()?.data != null) {
                    val serverItem = response.body()!!.data!!.item.toDomain()
                    packingDao.deletePackingItem(localItem.id)
                    packingDao.insertPackingItem(serverItem.toEntity(listId, "synced"))
                    return Result.success(serverItem)
                }
            } catch (_: Exception) { /* fallo silencioso, usamos el ítem local */ }

            Result.success(localItem)
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun togglePackingItem(itemId: String): Result<PackingItem> {
        return try {
            // Optimistic local update first
            val existing = packingDao.getPackingItemById(itemId)
            if (existing != null) {
                packingDao.updatePackingItem(existing.copy(isPacked = !existing.isPacked, syncStatus = "pending"))
            }

            val response = apiService.togglePackingItem(itemId)
            if (response.isSuccessful && response.body()?.data != null) {
                val item = response.body()!!.data!!.item.toDomain()
                val listId = existing?.packingListId ?: ""
                packingDao.insertPackingItem(item.toEntity(listId, "synced"))
                Result.success(item)
            } else {
                // Revert optimistic update on failure
                if (existing != null) packingDao.updatePackingItem(existing)
                Result.failure(ErrorMapper.fromResponse(response, "No se pudo actualizar el ítem"))
            }
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }

    override suspend fun deletePackingItem(itemId: String): Result<Unit> {
        return try {
            // Eliminar localmente de inmediato
            packingDao.deletePackingItem(itemId)
            // Intentar sincronizar con la API (fallo silencioso)
            try { apiService.deletePackingItem(itemId) } catch (_: Exception) {}
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(ErrorMapper.fromThrowable(e))
        }
    }
}
