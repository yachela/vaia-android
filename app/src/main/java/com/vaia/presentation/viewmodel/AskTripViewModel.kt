package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.TripInsight
import com.vaia.domain.model.TripQuestion
import com.vaia.domain.usecase.GetTripInsightsUseCase
import com.vaia.domain.usecase.TripInsightsCalculator
import com.vaia.domain.usecase.TripSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Un turno de la conversación: la pregunta que se tocó y lo que la app respondió. */
data class QaTurn(
    val question: TripQuestion,
    val insight: TripInsight
)

data class AskTripUiState(
    val isLoading: Boolean = true,
    val available: List<TripQuestion> = emptyList(),
    val turns: List<QaTurn> = emptyList(),
    /** El viaje no está en el cache local: no hay nada que responder. */
    val hasNoData: Boolean = false
)

@HiltViewModel
class AskTripViewModel @Inject constructor(
    private val getTripInsights: GetTripInsightsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AskTripUiState())
    val uiState: StateFlow<AskTripUiState> = _uiState.asStateFlow()

    private var snapshot: TripSnapshot? = null

    fun load(tripId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val loaded = getTripInsights(tripId)
            snapshot = loaded

            _uiState.value = if (loaded == null) {
                AskTripUiState(isLoading = false, hasNoData = true)
            } else {
                AskTripUiState(
                    isLoading = false,
                    available = TripInsightsCalculator.availableQuestions(loaded, today()),
                    turns = _uiState.value.turns
                )
            }
        }
    }

    fun ask(question: TripQuestion) {
        val current = snapshot ?: return
        val insight = TripInsightsCalculator.answer(question, current, today())
        _uiState.value = _uiState.value.copy(
            turns = _uiState.value.turns + QaTurn(question, insight)
        )
    }

    fun clear() {
        _uiState.value = _uiState.value.copy(turns = emptyList())
    }

    // Aislado para poder fijar la fecha en tests.
    internal fun today(): LocalDate = LocalDate.now()
}
