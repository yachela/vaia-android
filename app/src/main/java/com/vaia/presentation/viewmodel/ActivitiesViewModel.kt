package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.Activity
import com.vaia.domain.model.ActivitySuggestion
import com.vaia.domain.repository.ActivityRepository
import com.vaia.domain.repository.TripRepository
import com.vaia.worker.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ActivitiesViewModel(
    private val activityRepository: ActivityRepository,
    private val tripRepository: TripRepository,
    private val tripId: String,
    private val reminderScheduler: ReminderScheduler? = null
) : ViewModel() {

    private var tripTitle: String = ""

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities

    private val _timelineData = MutableStateFlow<TimelineData?>(null)
    val timelineData: StateFlow<TimelineData?> = _timelineData

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
        viewModelScope.launch {
            tripRepository.getTrip(tripId).getOrNull()?.title?.let { tripTitle = it }
        }
        loadActivities()
    }

    fun loadActivities() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            activityRepository.getActivities(tripId).fold(
                onSuccess = { activities ->
                    _activities.value = activities
                    updateTimelineData(activities)
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
                onSuccess = { activity ->
                    _createState.value = CreateState.Success
                    reminderScheduler?.schedule(activity, tripTitle)
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
                    reminderScheduler?.cancel(activityId)
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
    
    private val _visibleSuggestions = MutableStateFlow<List<ActivitySuggestion>>(emptyList())
    val visibleSuggestions: StateFlow<List<ActivitySuggestion>> = _visibleSuggestions
    
    private val dismissedSuggestionIds = mutableSetOf<String>()

    fun loadSuggestions() {
        viewModelScope.launch {
            _suggestionsState.value = SuggestionsState.Loading
            dismissedSuggestionIds.clear()
            
            activityRepository.getSuggestions(tripId).fold(
                onSuccess = { suggestions -> 
                    _suggestionsState.value = SuggestionsState.Success(suggestions)
                    _visibleSuggestions.value = suggestions
                },
                onFailure = { _suggestionsState.value = SuggestionsState.Error(it.message ?: "No se pudieron cargar sugerencias") }
            )
        }
    }
    
    fun acceptSuggestion(suggestion: ActivitySuggestion, date: String) {
        dismissedSuggestionIds.add(suggestion.hashCode().toString())
        _visibleSuggestions.value = _visibleSuggestions.value.filter { it.hashCode().toString() !in dismissedSuggestionIds }
        
        createActivity(
            title = suggestion.title,
            description = suggestion.description,
            date = date,
            time = suggestion.time,
            location = suggestion.location,
            cost = suggestion.cost
        )
    }
    
    fun acceptSuggestionDirectly(suggestion: ActivitySuggestion) {
        dismissedSuggestionIds.add(suggestion.hashCode().toString())
        _visibleSuggestions.value = _visibleSuggestions.value.filter { it.hashCode().toString() !in dismissedSuggestionIds }
    }
    
    fun dismissSuggestion(suggestion: ActivitySuggestion) {
        dismissedSuggestionIds.add(suggestion.hashCode().toString())
        _visibleSuggestions.value = _visibleSuggestions.value.filter { it.hashCode().toString() !in dismissedSuggestionIds }
    }

    fun resetSuggestionsState() {
        _suggestionsState.value = SuggestionsState.Idle
        _visibleSuggestions.value = emptyList()
    }

    sealed class SuggestionsState {
        object Idle : SuggestionsState()
        object Loading : SuggestionsState()
        data class Success(val suggestions: List<ActivitySuggestion>) : SuggestionsState()
        data class Error(val message: String) : SuggestionsState()
    }

    // Timeline support
    private fun updateTimelineData(activities: List<Activity>) {
        if (activities.isEmpty()) {
            _timelineData.value = null
            return
        }

        val sortedActivities = activities.sortedWith(
            compareBy(
                { normalizeDate(it.date) },
                { it.time }
            )
        )

        val activitiesWithStatus = sortedActivities.map { activity ->
            ActivityWithStatus(
                activity = activity,
                status = calculateActivityStatus(activity)
            )
        }

        val groupedByDate = activitiesWithStatus.groupBy { normalizeDate(it.activity.date) }
        val totalDays = groupedByDate.size
        val currentDate = getCurrentDate()

        val dayData = groupedByDate.entries.mapIndexed { index, (date, dayActivities) ->
            val completedCount = dayActivities.count { it.status == ActivityStatus.COMPLETED }
            val totalCount = dayActivities.size
            val progressPercentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0

            DayData(
                date = date,
                dayNumber = index + 1,
                totalDays = totalDays,
                activities = dayActivities,
                completedCount = completedCount,
                totalCount = totalCount,
                progressPercentage = progressPercentage
            )
        }

        _timelineData.value = TimelineData(
            days = dayData,
            destination = tripTitle
        )
    }

    private fun calculateActivityStatus(activity: Activity): ActivityStatus {
        // TODO: Implementar lógica de marcado manual cuando se agregue al modelo
        // Por ahora solo calculamos basado en tiempo
        
        val now = Calendar.getInstance()
        val activityDateTime = parseActivityDateTime(activity.date, activity.time) ?: return ActivityStatus.PENDING

        return when {
            activityDateTime.before(now) -> ActivityStatus.COMPLETED
            isActivityInProgress(activityDateTime, now) -> ActivityStatus.IN_PROGRESS
            else -> ActivityStatus.PENDING
        }
    }

    private fun parseActivityDateTime(date: String, time: String): Calendar? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val dateTimeStr = "$date ${time.ifBlank { "00:00" }}"
            val parsedDate = dateFormat.parse(dateTimeStr) ?: return null
            Calendar.getInstance().apply { this.time = parsedDate }
        } catch (e: Exception) {
            null
        }
    }

    private fun isActivityInProgress(activityTime: Calendar, now: Calendar): Boolean {
        val diffMinutes = (now.timeInMillis - activityTime.timeInMillis) / (1000 * 60)
        return diffMinutes in 0..120 // En curso si está dentro de las próximas 2 horas
    }

    private fun normalizeDate(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            inputFormat.format(parsedDate!!)
        } catch (e: Exception) {
            date
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Calendar.getInstance().time)
    }

    // Data classes para timeline
    data class TimelineData(
        val days: List<DayData>,
        val destination: String
    )

    data class DayData(
        val date: String,
        val dayNumber: Int,
        val totalDays: Int,
        val activities: List<ActivityWithStatus>,
        val completedCount: Int,
        val totalCount: Int,
        val progressPercentage: Int
    )

    data class ActivityWithStatus(
        val activity: Activity,
        val status: ActivityStatus
    )

    enum class ActivityStatus {
        COMPLETED,
        IN_PROGRESS,
        PENDING
    }
}
