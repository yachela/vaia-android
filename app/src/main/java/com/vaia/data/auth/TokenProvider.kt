package com.vaia.data.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fuente única de verdad en memoria para el token de sesión.
 *
 * Se actualiza desde AuthRepository (login/registro/logout) y se lee desde el
 * interceptor de red sin bloquear el hilo de la petición.
 */
@Singleton
class TokenProvider @Inject constructor() {

    @Volatile
    var token: String? = null

    fun isLoggedIn(): Boolean = !token.isNullOrBlank()
}
