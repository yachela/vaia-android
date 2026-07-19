package com.vaia.data.repository

import com.vaia.data.api.CurrencyApiService
import com.vaia.data.local.CurrencyCache
import com.vaia.data.local.ErrorLogger
import com.vaia.domain.model.ExchangeRates
import com.vaia.domain.repository.CurrencyRepository
import javax.inject.Inject

class CurrencyRepositoryImpl @Inject constructor(
    private val apiService: CurrencyApiService,
    private val currencyCache: CurrencyCache
) : CurrencyRepository {

    override suspend fun getExchangeRates(baseCurrency: String): Result<ExchangeRates> {
        return try {
            val response = apiService.getLatestRates(baseCurrency.lowercase())
            if (response.isSuccessful) {
                val jsonObject = response.body()
                val ratesJson = jsonObject?.getAsJsonObject(baseCurrency.lowercase())
                val rates = mutableMapOf<String, Double>()
                ratesJson?.entrySet()?.forEach { (key, value) ->
                    rates[key.uppercase()] = value.asDouble
                }
                if (rates.isEmpty()) {
                    return cachedRates(baseCurrency)
                        ?: Result.failure(Exception("No se recibieron tasas de cambio"))
                }
                currencyCache.saveRates(baseCurrency, rates)
                Result.success(
                    ExchangeRates(rates = rates, updatedAt = System.currentTimeMillis(), isFromCache = false)
                )
            } else {
                cachedRates(baseCurrency)
                    ?: Result.failure(Exception("Error al obtener tasas: ${response.message()}"))
            }
        } catch (e: Exception) {
            cachedRates(baseCurrency)
                ?: Result.failure(ErrorLogger.logAndWrap("Currency", "getExchangeRates", e, "Error al obtener tasas de cambio"))
        }
    }

    /** Última cotización guardada del par, para poder convertir sin conexión. */
    private suspend fun cachedRates(baseCurrency: String): Result<ExchangeRates>? =
        currencyCache.getRates(baseCurrency)?.let { (rates, updatedAt) ->
            Result.success(ExchangeRates(rates = rates, updatedAt = updatedAt, isFromCache = true))
        }

    override suspend fun getAvailableCurrencies(): Result<Map<String, String>> {
        return try {
            val response = apiService.getCurrencies()
            if (response.isSuccessful) {
                val currencies = (response.body() ?: emptyMap()).mapKeys { it.key.uppercase() }
                if (currencies.isEmpty()) {
                    return currencyCache.getCurrencies()?.let { Result.success(it) }
                        ?: Result.failure(Exception("No se recibió el listado de divisas"))
                }
                currencyCache.saveCurrencies(currencies)
                Result.success(currencies)
            } else {
                currencyCache.getCurrencies()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Error al obtener divisas: ${response.message()}"))
            }
        } catch (e: Exception) {
            currencyCache.getCurrencies()?.let { Result.success(it) }
                ?: Result.failure(ErrorLogger.logAndWrap("Currency", "getAvailableCurrencies", e, "Error al obtener divisas disponibles"))
        }
    }
}
