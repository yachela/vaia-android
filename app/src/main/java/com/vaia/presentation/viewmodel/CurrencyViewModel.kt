package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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

    // Diccionario de las 50 divisas más relevantes para un viajero argentino
    private val countryMapping = mapOf(
        "ARS" to "Argentina",
        "USD" to "Estados Unidos",
        "EUR" to "Unión Europea",
        "BRL" to "Brasil",
        "UYU" to "Uruguay",
        "CLP" to "Chile",
        "PYG" to "Paraguay",
        "COP" to "Colombia",
        "PEN" to "Perú",
        "BOB" to "Bolivia",
        "MXN" to "México",
        "GBP" to "Reino Unido",
        "CAD" to "Canadá",
        "AUD" to "Australia",
        "NZD" to "Nueva Zelanda",
        "JPY" to "Japón",
        "CNY" to "China",
        "CHF" to "Suiza",
        "TRY" to "Turquía",
        "ILS" to "Israel",
        "AED" to "Emiratos Árabes",
        "THB" to "Tailandia",
        "SGD" to "Singapur",
        "KRW" to "Corea del Sur",
        "ZAR" to "Sudáfrica",
        "NOK" to "Noruega",
        "SEK" to "Suecia",
        "DKK" to "Dinamarca",
        "CZK" to "República Checa",
        "HUF" to "Hungría",
        "PLN" to "Polonia",
        "INR" to "India",
        "IDR" to "Indonesia",
        "MYR" to "Malasia",
        "PHP" to "Filipinas",
        "VND" to "Vietnam",
        "EGP" to "Egipto",
        "MAD" to "Marruecos",
        "QAR" to "Qatar",
        "SAR" to "Arabia Saudita",
        "HKD" to "Hong Kong",
        "CRC" to "Costa Rica",
        "DOP" to "Rep. Dominicana",
        "PAB" to "Panamá",
        "GTQ" to "Guatemala",
        "HNL" to "Honduras",
        "NIO" to "Nicaragua",
        "ISK" to "Islandia",
        "RUB" to "Rusia",
        "TWD" to "Taiwán"
    )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _availableCurrencies.value = countryMapping.map { (code, country) ->
            CurrencyInfo(code, country)
        }.sortedBy { it.countryName }
        loadRates("USD")
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
