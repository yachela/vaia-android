package com.vaia.presentation.ui.explore

data class TrendingDestination(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val country: String
)

data class NearbyActivity(
    val id: String,
    val name: String,
    val location: String,
    val category: String,
    val distance: String,
    val imageUrl: String? = null
)

data class EditorChoice(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String? = null,
    val label: String = "Elección del Editor"
)

sealed class ExploreUiState {
    data object Loading : ExploreUiState()
    data class Success(
        val trendingDestinations: List<TrendingDestination>,
        val nearbyActivities: List<NearbyActivity>,
        val editorChoice: EditorChoice?
    ) : ExploreUiState()
    data class Error(val message: String) : ExploreUiState()
}
