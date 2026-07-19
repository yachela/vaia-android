package com.vaia.domain.model

/**
 * Tasas de cambio con su procedencia: si vienen del cache local, la UI avisa
 * desde cuándo son para que el usuario sepa que pueden estar desactualizadas.
 */
data class ExchangeRates(
    val rates: Map<String, Double>,
    val updatedAt: Long,
    val isFromCache: Boolean
)
