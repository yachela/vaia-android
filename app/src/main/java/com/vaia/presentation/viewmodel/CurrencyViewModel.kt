package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.Currencies
import com.vaia.domain.repository.CurrencyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CurrencyInfo(
    val code: String,
    val countryName: String,
    val rate: Double = 0.0
)

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    private val currencyRepository: CurrencyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CurrencyUiState>(CurrencyUiState.Loading)
    val uiState: StateFlow<CurrencyUiState> = _uiState.asStateFlow()

    private val _rates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val rates: StateFlow<Map<String, Double>> = _rates.asStateFlow()

    private val _availableCurrencies = MutableStateFlow<List<CurrencyInfo>>(emptyList())
    val availableCurrencies: StateFlow<List<CurrencyInfo>> = _availableCurrencies.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        // El catálogo es el mismo que usa la moneda del viaje: una sola lista.
        _availableCurrencies.value = Currencies.all.map { CurrencyInfo(it.code, it.countryName) }
        loadRates(Currencies.DEFAULT_CODE)
    }

    fun loadRates(baseCurrency: String) {
        viewModelScope.launch {
            _uiState.value = CurrencyUiState.Loading
            currencyRepository.getExchangeRates(baseCurrency.lowercase()).onSuccess { exchangeRates ->
                // Normalizar claves a Mayúsculas para consistencia con el mapping
                _rates.value = exchangeRates.rates.mapKeys { it.key.uppercase() }
                _uiState.value = if (exchangeRates.isFromCache) {
                    CurrencyUiState.SuccessFromCache(exchangeRates.updatedAt)
                } else {
                    CurrencyUiState.Success
                }
            }.onFailure { error ->
                _uiState.value = CurrencyUiState.Error(error.message ?: "Error de conexión")
            }
        }
    }
}

sealed class CurrencyUiState {
    object Loading : CurrencyUiState()
    object Success : CurrencyUiState()

    /** Conversión posible sin conexión, con tasas de [updatedAt] (epoch millis). */
    data class SuccessFromCache(val updatedAt: Long) : CurrencyUiState()
    data class Error(val message: String) : CurrencyUiState()
}
