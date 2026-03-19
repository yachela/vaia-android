package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.Trip
import com.vaia.domain.repository.TripRepository
import com.vaia.presentation.ui.home.HomeUiState
import com.vaia.presentation.ui.home.TripStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadTrips() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val result = tripRepository.getTrips()
                result.fold(
                    onSuccess = { trips ->
                        // Ordenar viajes por fecha de inicio (ascendente)
                        val sortedTrips = trips.sortedBy { it.startDate }
                        _uiState.value = HomeUiState.Success(sortedTrips)
                    },
                    onFailure = { error ->
                        _uiState.value = HomeUiState.Error(
                            error.message ?: "Error al cargar viajes"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(
                    e.message ?: "Error al cargar viajes"
                )
            }
        }
    }

    fun calculateTripStatus(trip: Trip): TripStatus {
        return try {
            val formatter = DateTimeFormatter.ISO_DATE
            val today = LocalDate.now()
            val startDate = LocalDate.parse(trip.startDate, formatter)
            val endDate = LocalDate.parse(trip.endDate, formatter)

            when {
                today.isBefore(startDate) -> TripStatus.UPCOMING
                today.isAfter(endDate) -> TripStatus.COMPLETED
                else -> TripStatus.IN_PROGRESS
            }
        } catch (e: Exception) {
            TripStatus.UPCOMING
        }
    }
}
