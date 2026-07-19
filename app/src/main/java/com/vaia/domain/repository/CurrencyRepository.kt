package com.vaia.domain.repository

import com.vaia.domain.model.ExchangeRates

interface CurrencyRepository {
    suspend fun getExchangeRates(baseCurrency: String): Result<ExchangeRates>
    suspend fun getAvailableCurrencies(): Result<Map<String, String>>
}
