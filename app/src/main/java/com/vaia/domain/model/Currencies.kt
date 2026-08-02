package com.vaia.domain.model

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
}
