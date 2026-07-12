package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.domain.repository.PackingRepository
import com.vaia.presentation.ui.packing.PackingListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PackingListViewModel @Inject constructor(
    private val packingRepository: PackingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PackingListUiState>(PackingListUiState.Loading)
    val uiState: StateFlow<PackingListUiState> = _uiState.asStateFlow()

    fun loadPackingList(tripId: String) {
        viewModelScope.launch {
            _uiState.value = PackingListUiState.Loading
            
            packingRepository.getPackingList(tripId)
                .onSuccess { packingList ->
                    // Load weather suggestions
                    packingRepository.getWeatherSuggestions(tripId)
                        .onSuccess { suggestions ->
                            _uiState.value = PackingListUiState.Success(
                                packingList = packingList,
                                weatherSuggestions = suggestions
                            )
                        }
                        .onFailure {
                            // Show packing list even if weather suggestions fail
                            _uiState.value = PackingListUiState.Success(
                                packingList = packingList,
                                weatherSuggestions = emptyList()
                            )
                        }
                }
                .onFailure { error ->
                    _uiState.value = PackingListUiState.Error(
                        error.message ?: "Error al cargar lista de equipaje"
                    )
                }
        }
    }

    fun toggleItem(itemId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is PackingListUiState.Success) {
                // Optimistic UI update immediately
                val updatedCategories = currentState.packingList.itemsByCategory.map { cat ->
                    cat.copy(items = cat.items.map { item ->
                        if (item.id == itemId) item.copy(isPacked = !item.isPacked) else item
                    })
                }
                val total = updatedCategories.sumOf { it.items.size }
                val packed = updatedCategories.sumOf { cat -> cat.items.count { it.isPacked } }
                val percentage = if (total > 0) (packed * 100) / total else 0
                _uiState.value = PackingListUiState.Success(
                    packingList = currentState.packingList.copy(
                        itemsByCategory = updatedCategories,
                        progress = com.vaia.domain.model.PackingProgress(total, packed, percentage)
                    ),
                    weatherSuggestions = currentState.weatherSuggestions
                )

                packingRepository.togglePackingItem(itemId)
                    .onFailure {
                        // Revert UI only if the local Room update also failed (network-only error)
                        // In demo mode we keep the optimistic state
                    }
            }
        }
    }

    fun addItem(tripId: String, name: String, category: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is PackingListUiState.Success) {
                packingRepository.addPackingItem(tripId, name, category)
                    .onSuccess { newItem ->
                        // Optimistic update: agregar el ítem localmente sin recargar
                        val updatedCategories = currentState.packingList.itemsByCategory.toMutableList()
                        val existingCatIndex = updatedCategories.indexOfFirst { it.category == newItem.category }
                        if (existingCatIndex >= 0) {
                            val cat = updatedCategories[existingCatIndex]
                            updatedCategories[existingCatIndex] = cat.copy(items = cat.items + newItem)
                        } else {
                            updatedCategories.add(com.vaia.domain.model.PackingCategory(newItem.category, listOf(newItem)))
                        }
                        val total = updatedCategories.sumOf { it.items.size }
                        val packed = updatedCategories.sumOf { cat -> cat.items.count { it.isPacked } }
                        val percentage = if (total > 0) (packed * 100) / total else 0
                        _uiState.value = PackingListUiState.Success(
                            packingList = currentState.packingList.copy(
                                itemsByCategory = updatedCategories,
                                progress = com.vaia.domain.model.PackingProgress(total, packed, percentage)
                            ),
                            weatherSuggestions = currentState.weatherSuggestions
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = PackingListUiState.Error(
                            error.message ?: "Error al agregar ítem"
                        )
                    }
            }
        }
    }

    fun deleteItem(itemId: String, tripId: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is PackingListUiState.Success) {
                packingRepository.deletePackingItem(itemId)
                    .onSuccess {
                        val updatedCategories = currentState.packingList.itemsByCategory
                            .map { cat -> cat.copy(items = cat.items.filter { it.id != itemId }) }
                            .filter { it.items.isNotEmpty() }
                        val total = updatedCategories.sumOf { it.items.size }
                        val packed = updatedCategories.sumOf { cat -> cat.items.count { it.isPacked } }
                        val percentage = if (total > 0) (packed * 100) / total else 0
                        _uiState.value = PackingListUiState.Success(
                            packingList = currentState.packingList.copy(
                                itemsByCategory = updatedCategories,
                                progress = com.vaia.domain.model.PackingProgress(total, packed, percentage)
                            ),
                            weatherSuggestions = currentState.weatherSuggestions
                        )
                    }
                    .onFailure { error ->
                        _uiState.value = PackingListUiState.Error(
                            error.message ?: "Error al eliminar ítem"
                        )
                    }
            }
        }
    }

    fun syncPendingChanges() {
        // TODO: Implement offline sync logic in Phase 7
        // This will be implemented when we add ConnectivityObserver and SyncManager
    }
}
