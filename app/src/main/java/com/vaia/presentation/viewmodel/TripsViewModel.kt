package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.Trip
import com.vaia.domain.repository.ActivityRepository
import com.vaia.domain.repository.AuthRepository
import com.vaia.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class TripsViewModel(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository,
    private val activityRepository: ActivityRepository
) : ViewModel() {

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _hasMorePages = MutableStateFlow(false)
    val hasMorePages: StateFlow<Boolean> = _hasMorePages

    private var currentPage = 1

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _createTripState = MutableStateFlow<CreateTripState>(CreateTripState.Idle)
    val createTripState: StateFlow<CreateTripState> = _createTripState

    private val _updateTripState = MutableStateFlow<UpdateTripState>(UpdateTripState.Idle)
    val updateTripState: StateFlow<UpdateTripState> = _updateTripState

    private val _deleteTripState = MutableStateFlow<DeleteTripState>(DeleteTripState.Idle)
    val deleteTripState: StateFlow<DeleteTripState> = _deleteTripState

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    fun loadTrips() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            currentPage = 1

            tripRepository.getTripsPage(1).fold(
                onSuccess = { (trips, hasNext) ->
                    _trips.value = trips
                    _hasMorePages.value = hasNext
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Error al cargar viajes"
                    _isLoading.value = false
                }
            )
        }
    }

    fun loadMoreTrips() {
        if (_isLoadingMore.value || !_hasMorePages.value) return
        viewModelScope.launch {
            _isLoadingMore.value = true
            val nextPage = currentPage + 1

            tripRepository.getTripsPage(nextPage).fold(
                onSuccess = { (newTrips, hasNext) ->
                    _trips.value = _trips.value + newTrips
                    _hasMorePages.value = hasNext
                    currentPage = nextPage
                    _isLoadingMore.value = false
                },
                onFailure = {
                    _isLoadingMore.value = false
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
        budget: Double,
        templateType: String? = null
    ) {
        viewModelScope.launch {
            _createTripState.value = CreateTripState.Loading
            tripRepository.createTrip(title, destination, startDate, endDate, budget).fold(
                onSuccess = { trip ->
                    seedTemplateActivities(trip, templateType)
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

    private suspend fun seedTemplateActivities(trip: Trip, templateType: String?) {
        val template = TripTemplate.fromKey(templateType) ?: return
        val baseDate = runCatching { LocalDate.parse(trip.startDate.take(10)) }.getOrNull() ?: return
        val plans = template.defaultPlans(trip.destination)

        plans.forEachIndexed { index, plan ->
            val date = baseDate.plusDays(index.toLong()).toString()
            activityRepository.createActivity(
                tripId = trip.id,
                title = plan.title,
                description = plan.description,
                date = date,
                time = plan.time,
                location = plan.location.ifBlank { trip.destination },
                cost = plan.cost
            )
        }
    }

    private enum class TripTemplate(val key: String) {
        AVENTURA("aventura"),
        FAMILIAR("familiar"),
        SOLITARIO("solitario"),
        AMIGOS("amigos");

        fun defaultPlans(destination: String): List<TemplatePlan> = when (this) {
            AVENTURA -> listOf(
                TemplatePlan("Senderismo panorámico", "Ruta de trekking al aire libre.", "08:00", destination, 0.0),
                TemplatePlan("Actividad extrema", "Reserva una experiencia de adrenalina.", "11:30", destination, 40.0),
                TemplatePlan("Atardecer en mirador", "Cierre del día en punto escénico.", "18:30", destination, 0.0)
            )
            FAMILIAR -> listOf(
                TemplatePlan("Desayuno familiar", "Comenzar el día con calma.", "09:00", destination, 25.0),
                TemplatePlan("Paseo cultural", "Museo o actividad apta para niños.", "12:00", destination, 30.0),
                TemplatePlan("Cena tranquila", "Restaurante cómodo para grupo familiar.", "19:30", destination, 45.0)
            )
            SOLITARIO -> listOf(
                TemplatePlan("Café y planificación", "Organizar el día en una cafetería local.", "09:00", destination, 10.0),
                TemplatePlan("Recorrido libre", "Caminar y descubrir zonas emblemáticas.", "13:00", destination, 0.0),
                TemplatePlan("Reflexión nocturna", "Cierre del día en un lugar tranquilo.", "20:00", destination, 15.0)
            )
            AMIGOS -> listOf(
                TemplatePlan("Brunch grupal", "Encuentro inicial para coordinar el día.", "10:30", destination, 20.0),
                TemplatePlan("Actividad en grupo", "Tour o experiencia para todos.", "14:00", destination, 35.0),
                TemplatePlan("Salida nocturna", "Plan social para la noche.", "21:00", destination, 30.0)
            )
        }

        companion object {
            fun fromKey(key: String?): TripTemplate? = entries.firstOrNull { it.key == key }
        }
    }

    private data class TemplatePlan(
        val title: String,
        val description: String,
        val time: String,
        val location: String,
        val cost: Double
    )

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

    fun exportItinerary(tripId: String) {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            tripRepository.exportItineraryPdf(tripId).fold(
                onSuccess = { bytes -> _exportState.value = ExportState.PdfReady(bytes, tripId) },
                onFailure = { _exportState.value = ExportState.Error(it.message ?: "Error al exportar") }
            )
        }
    }

    fun exportExpenses(tripId: String) {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            tripRepository.exportExpensesCsv(tripId).fold(
                onSuccess = { bytes -> _exportState.value = ExportState.CsvReady(bytes, tripId) },
                onFailure = { _exportState.value = ExportState.Error(it.message ?: "Error al exportar") }
            )
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
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

    sealed class ExportState {
        object Idle : ExportState()
        object Loading : ExportState()
        data class PdfReady(val bytes: ByteArray, val tripId: String) : ExportState()
        data class CsvReady(val bytes: ByteArray, val tripId: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }
}
