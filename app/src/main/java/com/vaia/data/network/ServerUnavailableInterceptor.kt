package com.vaia.data.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** El gateway respondió 502/503/504: el backend está caído o reiniciando, no es culpa del pedido. */
class ServerUnavailableException(val code: Int) :
    IOException("El servidor no está disponible ($code)")

/**
 * Los 502/503/504 son intermitentes: casi siempre alcanza con reintentar en unos segundos.
 *
 * Se reintenta solo GET y HEAD. Un 502 no dice si el backend llegó a procesar el pedido, así que
 * repetir un POST o un DELETE podría duplicar un documento o un gasto.
 *
 * Cuando se agotan los reintentos lanza [ServerUnavailableException] en lugar de devolver la
 * respuesta. Así el cuerpo del error (que suele ser el HTML de nginx, no JSON) nunca llega a la
 * pantalla, y los repositorios que cachean en Room caen a los datos guardados como con cualquier
 * otro fallo de red.
 */
class ServerUnavailableInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val allowedRetries = if (request.method in IDEMPOTENT_METHODS) MAX_RETRIES else 0

        var attempt = 0
        while (true) {
            val response = chain.proceed(request)
            if (response.code !in GATEWAY_CODES) return response

            response.close()
            android.util.Log.w(
                "VAIA_NETWORK",
                "Gateway ${response.code} en ${request.method} ${request.url.encodedPath} " +
                    "(intento ${attempt + 1} de ${allowedRetries + 1})"
            )

            if (attempt == allowedRetries) throw ServerUnavailableException(response.code)

            Thread.sleep(BACKOFF_MS * (attempt + 1))
            attempt++
        }
    }

    companion object {
        private val GATEWAY_CODES = setOf(502, 503, 504)
        private val IDEMPOTENT_METHODS = setOf("GET", "HEAD")
        private const val MAX_RETRIES = 2
        private const val BACKOFF_MS = 600L
    }
}
