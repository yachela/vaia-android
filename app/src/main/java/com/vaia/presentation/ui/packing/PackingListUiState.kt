package com.vaia.presentation.ui.packing

import com.vaia.domain.model.PackingList
import com.vaia.domain.model.WeatherSuggestion

sealed class PackingListUiState {
    data object Loading : PackingListUiState()
    data class Success(
        val packingList: PackingList,
        val weatherSuggestions: List<WeatherSuggestion> = emptyList(),
        val weatherError: String? = null
    ) : PackingListUiState()
    data class Error(val message: String) : PackingListUiState()
    data class Syncing(val packingList: PackingList) : PackingListUiState()
}
