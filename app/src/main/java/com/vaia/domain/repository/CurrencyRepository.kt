package com.vaia.domain.repository

interface CurrencyRepository {
    suspend fun getExchangeRates(baseCurrency: String): Result<Map<String, Double>>
    suspend fun getAvailableCurrencies(): Result<Map<String, String>>
}
