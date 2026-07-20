package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.TripInsight
import com.vaia.domain.model.TripQuestion
import com.vaia.domain.repository.AskTripRepository
import com.vaia.domain.usecase.TripInsightsCalculator
import com.vaia.domain.usecase.TripInsightsProvider
import com.vaia.domain.usecase.TripSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Un turno de la conversación. [insight] en null significa que la respuesta
 * todavía se está mostrando: el cálculo es instantáneo, pero una burbuja que
 * aparece en 0 ms no se lee bien, así que se muestra el indicador un instante.
 */
data class QaTurn(
    val question: TripQuestion,
    val insight: TripInsight?
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
    private val insights: TripInsightsProvider,
    private val askRepository: AskTripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AskTripUiState())
    val uiState: StateFlow<AskTripUiState> = _uiState.asStateFlow()

    private var snapshot: TripSnapshot? = null
    private var tripId: String? = null

    fun load(tripId: String) {
        this.tripId = tripId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val loaded = insights.snapshotOf(tripId)
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
        val trip = tripId ?: return

        viewModelScope.launch {
            val pending = QaTurn(question, insight = null)
            _uiState.value = _uiState.value.copy(turns = _uiState.value.turns + pending)

            val insight = if (question.needsAi) {
                // Acá la espera es real: el modelo puede tardar decenas de segundos.
                askRepository.ask(trip, question)
            } else {
                // La respuesta ya está calculada; la pausa es solo para que la burbuja
                // se pueda seguir con la vista, no para simular un trabajo que no existe.
                delay(ANSWER_REVEAL_DELAY_MS)
                TripInsightsCalculator.answer(question, current, today())
            }

            _uiState.value = _uiState.value.copy(
                turns = _uiState.value.turns.map { turn ->
                    // Por identidad: dos turnos de la misma pregunta son iguales por valor.
                    if (turn === pending) turn.copy(insight = insight) else turn
                }
            )
        }
    }

    fun clear() {
        _uiState.value = _uiState.value.copy(turns = emptyList())
    }

    // Aislado para poder fijar la fecha en tests.
    internal fun today(): LocalDate = LocalDate.now()

    companion object {
        /** Suficiente para que la burbuja se lea entrar; no tanto como para estorbar. */
        const val ANSWER_REVEAL_DELAY_MS = 450L
    }
}
