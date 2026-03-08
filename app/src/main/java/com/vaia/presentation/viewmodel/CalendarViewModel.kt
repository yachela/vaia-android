package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.Activity
import com.vaia.domain.model.Trip
import com.vaia.domain.repository.ActivityRepository
import com.vaia.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CalendarViewModel(
    private val tripRepository: TripRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    data class TripActivity(val activity: Activity, val trip: Trip)

    sealed class State {
        object Loading : State()
        data class Ready(val activities: List<TripActivity>) : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = State.Loading
            val tripsResult = tripRepository.getTrips()
            if (tripsResult.isFailure) {
                _state.value = State.Error(
                    tripsResult.exceptionOrNull()?.message ?: "Error al cargar viajes"
                )
                return@launch
            }
            val trips = tripsResult.getOrDefault(emptyList())
            val all = mutableListOf<TripActivity>()
            for (trip in trips) {
                activityRepository.getActivities(trip.id)
                    .getOrDefault(emptyList())
                    .forEach { all.add(TripActivity(it, trip)) }
            }
            _state.value = State.Ready(all)
        }
    }
}

class CalendarViewModelFactory(
    private val tripRepository: TripRepository,
    private val activityRepository: ActivityRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CalendarViewModel(tripRepository, activityRepository) as T
}
