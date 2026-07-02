package com.vaia.data.network

import com.vaia.data.auth.TokenProvider
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Añade las cabeceras comunes y el token de sesión a cada petición.
 *
 * Lee el token del caché en memoria de [TokenProvider], por lo que no
 * realiza ninguna operación bloqueante por request.
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
            .header("Accept", "application/json")

        val token = tokenProvider.token
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
