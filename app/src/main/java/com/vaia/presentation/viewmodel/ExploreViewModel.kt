package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.presentation.ui.explore.EditorChoice
import com.vaia.presentation.ui.explore.ExploreUiState
import com.vaia.presentation.ui.explore.NearbyActivity
import com.vaia.presentation.ui.explore.TrendingDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    // TODO: Inject repositories when backend endpoints are available
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExploreUiState>(ExploreUiState.Loading)
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    fun loadExploreData() {
        viewModelScope.launch {
            _uiState.value = ExploreUiState.Loading
            try {
                // TODO: Replace with actual API calls when backend is ready
                // For now, using mock data
                val trendingDestinations = getMockTrendingDestinations()
                val nearbyActivities = getMockNearbyActivities()
                val editorChoice = getMockEditorChoice()

                _uiState.value = ExploreUiState.Success(
                    trendingDestinations = trendingDestinations,
                    nearbyActivities = nearbyActivities,
                    editorChoice = editorChoice
                )
            } catch (e: Exception) {
                _uiState.value = ExploreUiState.Error(
                    e.message ?: "Error al cargar datos de exploración"
                )
            }
        }
    }

    // Mock data - replace with actual repository calls
    private fun getMockTrendingDestinations(): List<TrendingDestination> {
        return listOf(
            TrendingDestination("1", "París", null, "Francia"),
            TrendingDestination("2", "Tokyo", null, "Japón"),
            TrendingDestination("3", "Nueva York", null, "Estados Unidos"),
            TrendingDestination("4", "Barcelona", null, "España"),
            TrendingDestination("5", "Roma", null, "Italia"),
            TrendingDestination("6", "Londres", null, "Reino Unido")
        )
    }

    private fun getMockNearbyActivities(): List<NearbyActivity> {
        return listOf(
            NearbyActivity(
                "1",
                "Tour por el Centro Histórico",
                "Centro, Ciudad",
                "Cultura",
                "2.5 km",
                null
            ),
            NearbyActivity(
                "2",
                "Museo de Arte Moderno",
                "Zona Norte",
                "Arte",
                "3.8 km",
                null
            ),
            NearbyActivity(
                "3",
                "Parque Nacional",
                "Zona Sur",
                "Naturaleza",
                "5.2 km",
                null
            ),
            NearbyActivity(
                "4",
                "Restaurante Gourmet",
                "Zona Gastronómica",
                "Gastronomía",
                "1.8 km",
                null
            )
        )
    }

    private fun getMockEditorChoice(): EditorChoice {
        return EditorChoice(
            "1",
            "Descubre los Tesoros Ocultos de la Ciudad",
            "Una experiencia única que combina historia, cultura y gastronomía local",
            null,
            "Elección del Editor"
        )
    }
}
