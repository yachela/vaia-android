package com.vaia.presentation.ui.common

import com.vaia.domain.model.Currencies
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val SPANISH = Locale("es", "AR")

/**
 * Monto con el símbolo de la moneda del viaje. Los decimales solo aparecen cuando
 * existen: "US$1.250" se lee mejor que "US$1.250,00" en las tarjetas.
 */
fun formatMoney(amount: Double, currency: String?): String =
    "${Currencies.symbolOf(currency)}${formatAmount(amount)}"

/** Solo el número, para cuando la moneda ya está dicha en la etiqueta. */
fun formatAmount(amount: Double): String {
    val rounded = amount.roundToLong()
    return if (abs(amount - rounded) < 0.01) {
        String.format(SPANISH, "%,d", rounded)
    } else {
        String.format(SPANISH, "%,.2f", amount)
    }
}
