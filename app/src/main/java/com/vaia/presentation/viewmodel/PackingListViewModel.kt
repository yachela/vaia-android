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
                _uiState.value = PackingListUiState.Syncing(currentState.packingList)
                
                packingRepository.togglePackingItem(itemId)
                    .onSuccess {
                        // Reload the packing list to get updated data
                        loadPackingList(currentState.packingList.tripId)
                    }
                    .onFailure { error ->
                        _uiState.value = PackingListUiState.Error(
                            error.message ?: "Error al actualizar ítem"
                        )
                    }
            }
        }
    }

    fun addItem(tripId: String, name: String, category: String) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is PackingListUiState.Success) {
                _uiState.value = PackingListUiState.Syncing(currentState.packingList)
                
                packingRepository.addPackingItem(tripId, name, category)
                    .onSuccess {
                        // Reload the packing list to get updated data
                        loadPackingList(tripId)
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
                _uiState.value = PackingListUiState.Syncing(currentState.packingList)
                
                packingRepository.deletePackingItem(itemId)
                    .onSuccess {
                        // Reload the packing list to get updated data
                        loadPackingList(tripId)
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
