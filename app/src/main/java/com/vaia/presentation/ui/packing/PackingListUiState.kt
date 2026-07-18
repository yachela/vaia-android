package com.vaia.presentation.ui.packing

import com.vaia.domain.model.PackingList

sealed class PackingListUiState {
    data object Loading : PackingListUiState()
    data class Success(
        val packingList: PackingList
    ) : PackingListUiState()
    data class Error(val message: String) : PackingListUiState()
    data class Syncing(val packingList: PackingList) : PackingListUiState()
}
