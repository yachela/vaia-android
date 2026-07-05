package com.vaia.data.network

import okhttp3.Interceptor

/**
 * Punto de extensión del modo demo, implementado por cada flavor.
 *
 * El flavor `demo` aporta el MockInterceptor y el estado real de demo;
 * el flavor `prod` no compila ese código y devuelve una implementación nula.
 */
interface DemoModeController {

    /** Interceptor de respuestas simuladas, o null si el flavor no soporta modo demo. */
    val mockInterceptor: Interceptor?

    /** Activa o desactiva el modo demo. En prod es un no-op y siempre es false. */
    var isDemoEnabled: Boolean
}
