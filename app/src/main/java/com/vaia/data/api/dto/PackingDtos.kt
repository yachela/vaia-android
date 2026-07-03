package com.vaia.data.api.dto

import com.google.gson.annotations.SerializedName
import com.vaia.domain.model.PackingCategory
import com.vaia.domain.model.PackingItem
import com.vaia.domain.model.PackingList
import com.vaia.domain.model.PackingProgress
import com.vaia.domain.model.WeatherSuggestion

// DTOs de red para la lista de equipaje. La capa domain no conoce Gson.
// Se aceptan claves snake_case y camelCase porque el backend usa ambas variantes.

data class PackingListDto(
    val id: String?,
    @SerializedName(value = "trip_id", alternate = ["tripId"])
    val tripId: String? = null,
    @SerializedName(value = "items_by_category", alternate = ["itemsByCategory"])
    val itemsByCategory: List<PackingCategoryDto>? = null,
    val progress: PackingProgressDto? = null,
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: String? = null,
    @SerializedName(value = "updated_at", alternate = ["updatedAt"])
    val updatedAt: String? = null
)

data class PackingCategoryDto(
    val category: String?,
    val items: List<PackingItemDto>? = null
)

data class PackingItemDto(
    val id: String?,
    val name: String?,
    val category: String? = null,
    @SerializedName(value = "is_packed", alternate = ["isPacked"])
    val isPacked: Boolean? = null,
    @SerializedName(value = "is_suggested", alternate = ["isSuggested"])
    val isSuggested: Boolean? = null,
    @SerializedName(value = "suggestion_reason", alternate = ["suggestionReason"])
    val suggestionReason: String? = null,
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: String? = null,
    @SerializedName(value = "updated_at", alternate = ["updatedAt"])
    val updatedAt: String? = null
)

data class PackingProgressDto(
    val total: Int? = null,
    val packed: Int? = null,
    val percentage: Int? = null
)

data class WeatherSuggestionDto(
    val name: String?,
    val category: String? = null,
    @SerializedName(value = "suggestion_reason", alternate = ["suggestionReason"])
    val suggestionReason: String? = null
)

fun PackingListDto.toDomain(): PackingList = PackingList(
    id = id.orEmpty(),
    tripId = tripId.orEmpty(),
    itemsByCategory = itemsByCategory?.map { it.toDomain() } ?: emptyList(),
    progress = progress?.toDomain() ?: PackingProgress(0, 0, 0),
    createdAt = createdAt.orEmpty(),
    updatedAt = updatedAt.orEmpty()
)

fun PackingCategoryDto.toDomain(): PackingCategory = PackingCategory(
    category = category.orEmpty(),
    items = items?.map { it.toDomain() } ?: emptyList()
)

fun PackingItemDto.toDomain(): PackingItem = PackingItem(
    id = id.orEmpty(),
    name = name.orEmpty(),
    category = category.orEmpty(),
    isPacked = isPacked ?: false,
    isSuggested = isSuggested ?: false,
    suggestionReason = suggestionReason,
    createdAt = createdAt.orEmpty(),
    updatedAt = updatedAt.orEmpty()
)

fun PackingProgressDto.toDomain(): PackingProgress = PackingProgress(
    total = total ?: 0,
    packed = packed ?: 0,
    percentage = percentage ?: 0
)

fun WeatherSuggestionDto.toDomain(): WeatherSuggestion = WeatherSuggestion(
    name = name.orEmpty(),
    category = category.orEmpty(),
    suggestionReason = suggestionReason.orEmpty()
)

data class AddPackingItemRequest(
    val name: String,
    val category: String
)

data class PackingItemResponse(
    val item: PackingItemDto
)

data class WeatherSuggestionsResponse(
    val suggestions: List<WeatherSuggestionDto>
)
