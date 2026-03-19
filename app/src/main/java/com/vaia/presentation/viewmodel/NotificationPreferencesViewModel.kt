package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.data.api.NotificationPreferencesRequest
import com.vaia.data.api.VaiaApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val apiService: VaiaApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState

    fun loadPreferences() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // Por defecto, ambas preferencias están habilitadas
                // En una implementación real, cargaríamos esto del backend
                _uiState.value = UiState.Success(
                    activityReminders = true,
                    tripReminders = true,
                    isSaving = false
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error al cargar preferencias: ${e.message}")
            }
        }
    }

    fun updateActivityReminders(enabled: Boolean) {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(
                activityReminders = enabled,
                isSaving = true
            )
            updatePreferences(activityReminders = enabled)
        }
    }

    fun updateTripReminders(enabled: Boolean) {
        val currentState = _uiState.value
        if (currentState is UiState.Success) {
            _uiState.value = currentState.copy(
                tripReminders = enabled,
                isSaving = true
            )
            updatePreferences(tripReminders = enabled)
        }
    }

    private fun updatePreferences(
        activityReminders: Boolean? = null,
        tripReminders: Boolean? = null
    ) {
        viewModelScope.launch {
            try {
                val request = NotificationPreferencesRequest(
                    activityReminders = activityReminders,
                    tripReminders = tripReminders
                )
                val response = apiService.updateNotificationPreferences(request)
                
                if (response.isSuccessful && response.body()?.data != null) {
                    val data = response.body()!!.data!!
                    _uiState.value = UiState.Success(
                        activityReminders = data.activityReminders,
                        tripReminders = data.tripReminders,
                        isSaving = false
                    )
                } else {
                    val currentState = _uiState.value
                    if (currentState is UiState.Success) {
                        _uiState.value = currentState.copy(isSaving = false)
                    }
                }
            } catch (e: Exception) {
                val currentState = _uiState.value
                if (currentState is UiState.Success) {
                    _uiState.value = currentState.copy(isSaving = false)
                }
            }
        }
    }

    sealed class UiState {
        data object Loading : UiState()
        data class Success(
            val activityReminders: Boolean,
            val tripReminders: Boolean,
            val isSaving: Boolean = false
        ) : UiState()
        data class Error(val message: String) : UiState()
    }
}
