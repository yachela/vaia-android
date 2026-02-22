package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.Trip
import com.vaia.domain.repository.AuthRepository
import com.vaia.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TripsViewModel(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _createTripState = MutableStateFlow<CreateTripState>(CreateTripState.Idle)
    val createTripState: StateFlow<CreateTripState> = _createTripState

    private val _updateTripState = MutableStateFlow<UpdateTripState>(UpdateTripState.Idle)
    val updateTripState: StateFlow<UpdateTripState> = _updateTripState

    private val _deleteTripState = MutableStateFlow<DeleteTripState>(DeleteTripState.Idle)
    val deleteTripState: StateFlow<DeleteTripState> = _deleteTripState

    fun loadTrips() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            tripRepository.getTrips().fold(
                onSuccess = { trips ->
                    _trips.value = trips
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Error al cargar viajes"
                    _isLoading.value = false
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _trips.value = emptyList()
            _error.value = null
        }
    }

    fun createTrip(
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        budget: Double
    ) {
        viewModelScope.launch {
            _createTripState.value = CreateTripState.Loading
            tripRepository.createTrip(title, destination, startDate, endDate, budget).fold(
                onSuccess = {
                    _createTripState.value = CreateTripState.Success
                    loadTrips()
                },
                onFailure = { exception ->
                    _createTripState.value = CreateTripState.Error(
                        exception.message ?: "Error al crear viaje"
                    )
                }
            )
        }
    }

    fun resetCreateTripState() {
        _createTripState.value = CreateTripState.Idle
    }

    fun updateTrip(
        tripId: String,
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        budget: Double
    ) {
        viewModelScope.launch {
            _updateTripState.value = UpdateTripState.Loading
            tripRepository.updateTrip(tripId, title, destination, startDate, endDate, budget).fold(
                onSuccess = {
                    _updateTripState.value = UpdateTripState.Success
                    loadTrips()
                },
                onFailure = { exception ->
                    _updateTripState.value = UpdateTripState.Error(
                        exception.message ?: "Error al actualizar viaje"
                    )
                }
            )
        }
    }

    fun resetUpdateTripState() {
        _updateTripState.value = UpdateTripState.Idle
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            _deleteTripState.value = DeleteTripState.Loading
            tripRepository.deleteTrip(tripId).fold(
                onSuccess = {
                    _deleteTripState.value = DeleteTripState.Success
                    loadTrips()
                },
                onFailure = { exception ->
                    _deleteTripState.value = DeleteTripState.Error(
                        exception.message ?: "Error al eliminar viaje"
                    )
                }
            )
        }
    }

    fun resetDeleteTripState() {
        _deleteTripState.value = DeleteTripState.Idle
    }

    sealed class CreateTripState {
        object Idle : CreateTripState()
        object Loading : CreateTripState()
        object Success : CreateTripState()
        data class Error(val message: String) : CreateTripState()
    }

    sealed class UpdateTripState {
        object Idle : UpdateTripState()
        object Loading : UpdateTripState()
        object Success : UpdateTripState()
        data class Error(val message: String) : UpdateTripState()
    }

    sealed class DeleteTripState {
        object Idle : DeleteTripState()
        object Loading : DeleteTripState()
        object Success : DeleteTripState()
        data class Error(val message: String) : DeleteTripState()
    }
}
