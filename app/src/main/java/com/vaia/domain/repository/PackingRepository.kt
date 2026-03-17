package com.vaia.domain.repository

import com.vaia.domain.model.PackingItem
import com.vaia.domain.model.PackingList
import com.vaia.domain.model.WeatherSuggestion

interface PackingRepository {
    suspend fun getPackingList(tripId: String): Result<PackingList>
    suspend fun generatePackingList(tripId: String): Result<PackingList>
    suspend fun getWeatherSuggestions(tripId: String): Result<List<WeatherSuggestion>>
    suspend fun addPackingItem(tripId: String, name: String, category: String): Result<PackingItem>
    suspend fun togglePackingItem(itemId: String): Result<PackingItem>
    suspend fun deletePackingItem(itemId: String): Result<Unit>
}
