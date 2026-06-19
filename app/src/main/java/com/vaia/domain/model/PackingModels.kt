package com.vaia.domain.model

import com.google.gson.annotations.SerializedName

data class PackingList(
    val id: String,
    @SerializedName(value = "trip_id", alternate = ["tripId"])
    val tripId: String,
    @SerializedName(value = "items_by_category", alternate = ["itemsByCategory"])
    val itemsByCategory: List<PackingCategory>,
    val progress: PackingProgress,
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: String,
    @SerializedName(value = "updated_at", alternate = ["updatedAt"])
    val updatedAt: String
)

data class PackingCategory(
    val category: String,
    val items: List<PackingItem>
)

data class PackingItem(
    val id: String,
    val name: String,
    val category: String,
    @SerializedName(value = "is_packed", alternate = ["isPacked"])
    val isPacked: Boolean,
    @SerializedName(value = "is_suggested", alternate = ["isSuggested"])
    val isSuggested: Boolean,
    @SerializedName(value = "suggestion_reason", alternate = ["suggestionReason"])
    val suggestionReason: String?,
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: String,
    @SerializedName(value = "updated_at", alternate = ["updatedAt"])
    val updatedAt: String
)

data class PackingProgress(
    val total: Int,
    val packed: Int,
    val percentage: Int
)

data class WeatherSuggestion(
    val name: String,
    val category: String,
    val suggestionReason: String
)

enum class PackingCategoryType {
    HIGIENE,
    ROPA,
    TECNOLOGIA,
    DOCUMENTACION
}
