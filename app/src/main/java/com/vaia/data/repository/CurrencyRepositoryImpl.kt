package com.vaia.data.repository

import com.vaia.data.api.CurrencyApiService
import com.vaia.domain.repository.CurrencyRepository
import javax.inject.Inject

class CurrencyRepositoryImpl @Inject constructor(
    private val apiService: CurrencyApiService
) : CurrencyRepository {

    override suspend fun getExchangeRates(baseCurrency: String): Result<Map<String, Double>> {
        return try {
            val response = apiService.getLatestRates(baseCurrency.lowercase())
            if (response.isSuccessful) {
                val jsonObject = response.body()
                val ratesJson = jsonObject?.getAsJsonObject(baseCurrency.lowercase())
                val rates = mutableMapOf<String, Double>()
                ratesJson?.entrySet()?.forEach { (key, value) ->
                    rates[key.uppercase()] = value.asDouble
                }
                Result.success(rates)
            } else {
                Result.failure(Exception("Error al obtener tasas: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAvailableCurrencies(): Result<Map<String, String>> {
        return try {
            val response = apiService.getCurrencies()
            if (response.isSuccessful) {
                val currencies = response.body() ?: emptyMap()
                Result.success(currencies.mapKeys { it.key.uppercase() })
            } else {
                Result.failure(Exception("Error al obtener divisas: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
