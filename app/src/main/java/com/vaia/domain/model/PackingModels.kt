package com.vaia.domain.model

data class PackingList(
    val id: String,
    val tripId: String,
    val itemsByCategory: List<PackingCategory>,
    val progress: PackingProgress,
    val createdAt: String,
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
    val isPacked: Boolean,
    val isSuggested: Boolean,
    val suggestionReason: String?,
    val createdAt: String,
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
