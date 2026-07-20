package com.vaia.data

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class CurrencyMockInterceptor : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!DemoMode.isEnabled) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val url = request.url.toString()

        Thread.sleep(100)

        val responseBody = when {
            url.contains("/currencies/") && url.endsWith(".json") -> {
                val code = url.substringAfterLast("/currencies/").substringBefore(".")
                val rates = mapOf(
                    "usd" to 1.0,
                    "eur" to 0.92,
                    "ars" to 350.0,
                    "brl" to 4.97,
                    "gbp" to 0.79,
                    "jpy" to 149.5,
                    "cny" to 7.24,
                    "mxn" to 17.15,
                    "clp" to 880.0,
                    "cop" to 3950.0,
                    "pen" to 3.72,
                    "uyu" to 38.5,
                    "pyg" to 7200.0,
                    "bob" to 6.9
                )
                gson.toJson(mapOf(code.lowercase() to rates))
            }
            url.endsWith("/currencies.json") -> {
                val currencies = mapOf(
                    "usd" to "United States Dollar",
                    "eur" to "Euro",
                    "ars" to "Argentine Peso",
                    "brl" to "Brazilian Real",
                    "gbp" to "British Pound",
                    "jpy" to "Japanese Yen",
                    "cny" to "Chinese Yuan",
                    "mxn" to "Mexican Peso",
                    "clp" to "Chilean Peso",
                    "cop" to "Colombian Peso",
                    "pen" to "Peruvian Sol",
                    "uyu" to "Uruguayan Peso",
                    "pyg" to "Paraguayan Guarani",
                    "bob" to "Bolivian Boliviano"
                )
                gson.toJson(currencies)
            }
            else -> gson.toJson(emptyMap<String, Any>())
        }

        return Response.Builder()
            .code(200)
            .message("OK")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(responseBody.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
