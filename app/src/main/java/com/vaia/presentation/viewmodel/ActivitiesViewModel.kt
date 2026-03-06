package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.Activity
import com.vaia.domain.model.ActivitySuggestion
import com.vaia.domain.repository.ActivityRepository
import com.vaia.domain.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ActivitiesViewModel(
    private val activityRepository: ActivityRepository,
    private val tripRepository: TripRepository,
    private val tripId: String
) : ViewModel() {

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _createState = MutableStateFlow<CreateState>(CreateState.Idle)
    val createState: StateFlow<CreateState> = _createState

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState

    init {
        loadActivities()
    }

    fun loadActivities() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            activityRepository.getActivities(tripId).fold(
                onSuccess = { activities ->
                    _activities.value = activities
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Error al cargar actividades"
                    _isLoading.value = false
                }
            )
        }
    }

    fun createActivity(title: String, description: String, date: String, time: String, location: String, cost: Double) {
        viewModelScope.launch {
            _createState.value = CreateState.Loading

            activityRepository.createActivity(tripId, title, description, date, time, location, cost).fold(
                onSuccess = {
                    _createState.value = CreateState.Success
                    loadActivities()
                },
                onFailure = { exception ->
                    _createState.value = CreateState.Error(exception.message ?: "Error al crear actividad")
                }
            )
        }
    }

    fun resetCreateState() {
        _createState.value = CreateState.Idle
    }

    fun updateActivity(activityId: String, title: String, description: String, date: String, time: String, location: String, cost: Double) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Loading

            activityRepository.updateActivity(tripId, activityId, title, description, date, time, location, cost).fold(
                onSuccess = {
                    _updateState.value = UpdateState.Success
                    loadActivities()
                },
                onFailure = { exception ->
                    _updateState.value = UpdateState.Error(exception.message ?: "Error al actualizar actividad")
                }
            )
        }
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Loading

            activityRepository.deleteActivity(tripId, activityId).fold(
                onSuccess = {
                    _deleteState.value = DeleteState.Success
                    loadActivities()
                },
                onFailure = { exception ->
                    _deleteState.value = DeleteState.Error(exception.message ?: "Error al eliminar actividad")
                }
            )
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateState.Idle
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    sealed class CreateState {
        object Idle : CreateState()
        object Loading : CreateState()
        object Success : CreateState()
        data class Error(val message: String) : CreateState()
    }

    sealed class UpdateState {
        object Idle : UpdateState()
        object Loading : UpdateState()
        object Success : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    sealed class DeleteState {
        object Idle : DeleteState()
        object Loading : DeleteState()
        object Success : DeleteState()
        data class Error(val message: String) : DeleteState()
    }

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState

    fun exportItinerary() {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            tripRepository.exportItineraryPdf(tripId).fold(
                onSuccess = { bytes -> _exportState.value = ExportState.PdfReady(bytes) },
                onFailure = { _exportState.value = ExportState.Error(it.message ?: "Error al exportar") }
            )
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    sealed class ExportState {
        object Idle : ExportState()
        object Loading : ExportState()
        data class PdfReady(val bytes: ByteArray) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    private val _suggestionsState = MutableStateFlow<SuggestionsState>(SuggestionsState.Idle)
    val suggestionsState: StateFlow<SuggestionsState> = _suggestionsState

    fun loadSuggestions() {
        viewModelScope.launch {
            _suggestionsState.value = SuggestionsState.Loading
            activityRepository.getSuggestions(tripId).fold(
                onSuccess = { suggestions -> _suggestionsState.value = SuggestionsState.Success(suggestions) },
                onFailure = { _suggestionsState.value = SuggestionsState.Error(it.message ?: "No se pudieron cargar sugerencias") }
            )
        }
    }

    fun acceptSuggestion(suggestion: ActivitySuggestion, date: String) {
        createActivity(
            title = suggestion.title,
            description = suggestion.description,
            date = date,
            time = suggestion.time,
            location = suggestion.location,
            cost = suggestion.cost
        )
    }

    fun resetSuggestionsState() {
        _suggestionsState.value = SuggestionsState.Idle
    }

    sealed class SuggestionsState {
        object Idle : SuggestionsState()
        object Loading : SuggestionsState()
        data class Success(val suggestions: List<ActivitySuggestion>) : SuggestionsState()
        data class Error(val message: String) : SuggestionsState()
    }
}
