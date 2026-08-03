package com.vaia.domain.model

import java.text.Normalizer

/** Una moneda del catálogo: el código ISO, el país que la usa y cómo se escribe el monto. */
data class CurrencyOption(
    val code: String,
    val countryName: String,
    val symbol: String
)

/**
 * Catálogo único de monedas. Antes cada pantalla escribía "$" a mano y el consejo
 * de presupuesto asumía USD: si el presupuesto se cargaba en una moneda y los
 * gastos en otra, el total se sumaba igual y el viaje aparecía siempre excedido.
 * La moneda ahora es un dato del viaje y todos los montos se muestran con la suya.
 */
object Currencies {

    const val DEFAULT_CODE = "USD"

    private val catalog = listOf(
        CurrencyOption("ARS", "Argentina", "$"),
        CurrencyOption("USD", "Estados Unidos", "US$"),
        CurrencyOption("EUR", "Unión Europea", "€"),
        CurrencyOption("BRL", "Brasil", "R$"),
        CurrencyOption("UYU", "Uruguay", "\$U"),
        CurrencyOption("CLP", "Chile", "CLP$"),
        CurrencyOption("PYG", "Paraguay", "₲"),
        CurrencyOption("COP", "Colombia", "COL$"),
        CurrencyOption("PEN", "Perú", "S/"),
        CurrencyOption("BOB", "Bolivia", "Bs"),
        CurrencyOption("MXN", "México", "MX$"),
        CurrencyOption("GBP", "Reino Unido", "£"),
        CurrencyOption("CAD", "Canadá", "CA$"),
        CurrencyOption("AUD", "Australia", "A$"),
        CurrencyOption("NZD", "Nueva Zelanda", "NZ$"),
        CurrencyOption("JPY", "Japón", "¥"),
        CurrencyOption("CNY", "China", "CN¥"),
        CurrencyOption("CHF", "Suiza", "CHF"),
        CurrencyOption("TRY", "Turquía", "₺"),
        CurrencyOption("ILS", "Israel", "₪"),
        CurrencyOption("AED", "Emiratos Árabes", "AED"),
        CurrencyOption("THB", "Tailandia", "฿"),
        CurrencyOption("SGD", "Singapur", "S$"),
        CurrencyOption("KRW", "Corea del Sur", "₩"),
        CurrencyOption("ZAR", "Sudáfrica", "R"),
        CurrencyOption("NOK", "Noruega", "kr"),
        CurrencyOption("SEK", "Suecia", "kr"),
        CurrencyOption("DKK", "Dinamarca", "kr"),
        CurrencyOption("CZK", "República Checa", "Kč"),
        CurrencyOption("HUF", "Hungría", "Ft"),
        CurrencyOption("PLN", "Polonia", "zł"),
        CurrencyOption("INR", "India", "₹"),
        CurrencyOption("IDR", "Indonesia", "Rp"),
        CurrencyOption("MYR", "Malasia", "RM"),
        CurrencyOption("PHP", "Filipinas", "₱"),
        CurrencyOption("VND", "Vietnam", "₫"),
        CurrencyOption("EGP", "Egipto", "E£"),
        CurrencyOption("MAD", "Marruecos", "MAD"),
        CurrencyOption("QAR", "Qatar", "QAR"),
        CurrencyOption("SAR", "Arabia Saudita", "SAR"),
        CurrencyOption("HKD", "Hong Kong", "HK$"),
        CurrencyOption("CRC", "Costa Rica", "₡"),
        CurrencyOption("DOP", "Rep. Dominicana", "RD$"),
        CurrencyOption("PAB", "Panamá", "B/."),
        CurrencyOption("GTQ", "Guatemala", "Q"),
        CurrencyOption("HNL", "Honduras", "L"),
        CurrencyOption("NIO", "Nicaragua", "C$"),
        CurrencyOption("ISK", "Islandia", "kr"),
        CurrencyOption("RUB", "Rusia", "₽"),
        CurrencyOption("TWD", "Taiwán", "NT$")
    )

    private val byCode = catalog.associateBy { it.code }

    /** Ordenadas por país, que es como las busca quien arma el viaje. */
    val all: List<CurrencyOption> = catalog.sortedBy { it.countryName }

    fun optionOf(code: String?): CurrencyOption? = code?.uppercase()?.let { byCode[it] }

    /** Códigos guardados en minúscula o desconocidos no rompen la pantalla: caen al default. */
    fun normalize(code: String?): String = optionOf(code)?.code ?: DEFAULT_CODE

    /** Símbolo para mostrar el monto; sigue el mismo fallback que [normalize]. */
    fun symbolOf(code: String?): String = byCode.getValue(normalize(code)).symbol

    /**
     * El puñado de monedas que casi siempre alcanza: la del viaje, la que el usuario
     * eligió en su perfil y las de la región. Va primero en el selector para que el
     * catálogo completo quede detrás del buscador y no haya que scrollear 50 países.
     */
    fun suggestedFor(profileCode: String?, currentCode: String?): List<CurrencyOption> =
        listOfNotNull(optionOf(currentCode), optionOf(profileCode))
            .plus(REGIONAL_CODES.mapNotNull { byCode[it] })
            .distinct()
            .take(MAX_SUGGESTED)

    /**
     * Busca por código o país ignorando mayúsculas **y acentos**: en un teclado móvil
     * "peru" o "japon" se escriben sin tilde, y comparar con `ignoreCase` sola dejaba
     * sin resultados a las 10 monedas cuyo país lleva acento.
     */
    fun search(query: String): List<CurrencyOption> {
        val normalized = foldAccents(query.trim())
        if (normalized.isEmpty()) return all
        return all.filter {
            foldAccents(it.code).contains(normalized) ||
                foldAccents(it.countryName).contains(normalized)
        }
    }

    private fun foldAccents(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .lowercase()

    private val DIACRITICS = Regex("\\p{Mn}+")

    private const val MAX_SUGGESTED = 6

    private val REGIONAL_CODES = listOf("ARS", "USD", "EUR", "BRL")
}
