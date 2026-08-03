package com.vaia.data.integration

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient

/**
 * Un único [PlacesClient] para todo el proceso.
 *
 * Cada `Places.createClient()` levanta un canal gRPC propio, con sus hilos, y el SDK
 * no expone forma de apagarlo. Creando uno por campo de autocompletado —y el
 * formulario de viaje tiene uno por destino, rehechos en cada apertura— los canales
 * se acumulaban hasta que el recolector los juntaba, y gRPC lo denunciaba en el log:
 *
 *     Previous channel was garbage collected without being shut down!
 *
 * El SDK está pensado para un cliente por proceso, así que se crea una sola vez y se
 * comparte. Se usa el `applicationContext` a propósito: el cliente vive lo que vive
 * el proceso y guardar una Activity acá la dejaría sin recolectar.
 */
object PlacesClientProvider {

    @Volatile
    private var instance: PlacesClient? = null

    /** `null` si Places no está inicializado (build sin `MAPS_API_KEY`). */
    fun get(context: Context): PlacesClient? {
        if (!Places.isInitialized()) return null
        return instance ?: synchronized(this) {
            instance ?: Places.createClient(context.applicationContext).also { instance = it }
        }
    }
}
