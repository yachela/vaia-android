package com.vaia.data

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import com.google.gson.Gson

class MockInterceptor : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

        // Auto-enable demo mode when demo credentials are used
        if ((url.contains("/api/login") || url.contains("/api/register")) && !DemoMode.isEnabled) {
            val bodyStr = try {
                val buffer = Buffer()
                request.body?.writeTo(buffer)
                buffer.readUtf8()
            } catch (_: Exception) { "" }
            if (bodyStr.contains("demo@vaia.app")) DemoMode.isEnabled = true
        }

        // Only mock if demo mode is enabled
        if (!DemoMode.isEnabled) {
            return chain.proceed(request)
        }

        // Simulate network delay
        Thread.sleep(200)

        val responseBody = when {
            url.contains("/api/login") || url.contains("/api/register") -> gson.toJson(mapOf(
                "data" to mapOf(
                    "user" to mapOf(
                        "id" to "demo-user-1",
                        "name" to "Viajero Demo",
                        "email" to "demo@vaia.app",
                        "bio" to "Amante de los viajes",
                        "country" to "España",
                        "language" to "es",
                        "currency" to "EUR"
                    ),
                    "access_token" to "demo-token-12345",
                    "token_type" to "Bearer"
                )
            ))
            
            url.contains("/api/user") && request.method == "GET" -> gson.toJson(mapOf(
                "data" to mapOf(
                    "id" to "demo-user-1",
                    "name" to "Viajero Demo",
                    "email" to "demo@vaia.app",
                    "bio" to "Amante de los viajes",
                    "country" to "España",
                    "language" to "es",
                    "currency" to "EUR"
                )
            ))

            url.contains("/api/trips") && !url.contains("/trips/") -> gson.toJson(mapOf(
                "data" to listOf(
                    mapOf(
                        "id" to "trip-1",
                        "title" to "Viaje a París",
                        "destination" to "París, Madrid",
                        "start_date" to "2026-04-15",
                        "end_date" to "2026-04-22",
                        "budget" to 2500.0,
                        "total_expenses" to 1850.0,
                        "activities_count" to 6,
                        "expenses_count" to 4
                    ),
                    mapOf(
                        "id" to "trip-2",
                        "title" to "Tour por Italia",
                        "destination" to "Roma, Florencia, Venecia",
                        "start_date" to "2026-06-01",
                        "end_date" to "2026-06-12",
                        "budget" to 3200.0,
                        "total_expenses" to 1400.0,
                        "activities_count" to 8,
                        "expenses_count" to 6
                    ),
                    mapOf(
                        "id" to "trip-3",
                        "title" to "Escapada a Bali",
                        "destination" to "Bali, Indonesia",
                        "start_date" to "2026-08-15",
                        "end_date" to "2026-08-25",
                        "budget" to 4000.0,
                        "total_expenses" to 0.0,
                        "activities_count" to 0,
                        "expenses_count" to 0
                    ),
                    mapOf(
                        "id" to "trip-4",
                        "title" to "Nueva York Express",
                        "destination" to "Nueva York, EE.UU.",
                        "start_date" to "2025-11-10",
                        "end_date" to "2025-11-17",
                        "budget" to 3500.0,
                        "total_expenses" to 3210.0,
                        "activities_count" to 10,
                        "expenses_count" to 15
                    )
                )
            ))

            url.matches(Regex(".*/api/trips/[^/]+$")) -> {
                val tripId = url.substringAfter("/api/trips/").substringBefore("?")
                gson.toJson(mapOf(
                    "data" to mapOf(
                        "id" to tripId,
                        "title" to "Viaje a París",
                        "destination" to "París, Francia",
                        "start_date" to "2026-04-15",
                        "end_date" to "2026-04-22",
                        "budget" to 2500.0,
                        "total_expenses" to 1850.0,
                        "activities_count" to 8,
                        "expenses_count" to 12
                    )
                ))
            }

            url.contains("/api/trips/") && url.contains("/activities") -> {
                val tripId = url.substringAfter("/api/trips/").substringBefore("/activities")
                val activities: List<Map<String, Any>> = when (tripId) {
                    "trip-1" -> listOf(
                        mapOf("id" to "act-1", "trip_id" to tripId, "title" to "Visita a la Torre Eiffel", "description" to "Tour guiado al monumento más famoso de París, con vistas panorámicas desde la cima.", "date" to "2026-04-15", "time" to "10:00", "location" to "Torre Eiffel, París", "cost" to 28.0),
                        mapOf("id" to "act-2", "trip_id" to tripId, "title" to "Museo del Louvre", "description" to "Recorrido por las colecciones más importantes del mundo, incluyendo la Mona Lisa.", "date" to "2026-04-16", "time" to "09:30", "location" to "Musée du Louvre", "cost" to 22.0),
                        mapOf("id" to "act-3", "trip_id" to tripId, "title" to "Cena en Montmartre", "description" to "Cena tradicional francesa en el barrio artístico de Montmartre.", "date" to "2026-04-16", "time" to "20:00", "location" to "Montmartre, París", "cost" to 65.0),
                        mapOf("id" to "act-4", "trip_id" to tripId, "title" to "Palacio de Versalles", "description" to "Excursión de día completo al palacio y sus jardines.", "date" to "2026-04-17", "time" to "08:00", "location" to "Versalles", "cost" to 20.0),
                        mapOf("id" to "act-5", "trip_id" to tripId, "title" to "Crucero por el Sena", "description" to "Crucero nocturno con vistas iluminadas de los monumentos.", "date" to "2026-04-18", "time" to "21:00", "location" to "Río Sena", "cost" to 18.0),
                        mapOf("id" to "act-6", "trip_id" to tripId, "title" to "Barrio de Le Marais", "description" to "Paseo por el barrio histórico con sus galerías de arte y plazas.", "date" to "2026-04-19", "time" to "11:00", "location" to "Le Marais, París", "cost" to 0.0)
                    )
                    "trip-2" -> listOf(
                        mapOf("id" to "act-7", "trip_id" to tripId, "title" to "Coliseo Romano", "description" to "Visita al anfiteatro más grande del Imperio Romano.", "date" to "2026-06-01", "time" to "09:00", "location" to "Coliseo, Roma", "cost" to 18.0),
                        mapOf("id" to "act-8", "trip_id" to tripId, "title" to "Ciudad del Vaticano", "description" to "Museos Vaticanos y la Capilla Sixtina.", "date" to "2026-06-02", "time" to "08:30", "location" to "Vaticano", "cost" to 25.0),
                        mapOf("id" to "act-9", "trip_id" to tripId, "title" to "Galería Uffizi", "description" to "Colección de arte renacentista en Florencia, con obras de Botticelli y Miguel Ángel.", "date" to "2026-06-05", "time" to "10:00", "location" to "Galería Uffizi, Florencia", "cost" to 20.0),
                        mapOf("id" to "act-10", "trip_id" to tripId, "title" to "Paseo en góndola", "description" to "Recorrido clásico en góndola por los canales de Venecia.", "date" to "2026-06-09", "time" to "17:00", "location" to "Gran Canal, Venecia", "cost" to 80.0)
                    )
                    "trip-4" -> listOf(
                        mapOf("id" to "act-11", "trip_id" to tripId, "title" to "Estatua de la Libertad", "description" to "Ferry y visita a la isla.", "date" to "2025-11-11", "time" to "09:00", "location" to "Liberty Island", "cost" to 24.0),
                        mapOf("id" to "act-12", "trip_id" to tripId, "title" to "Times Square", "description" to "Exploración del corazón de Manhattan.", "date" to "2025-11-11", "time" to "19:00", "location" to "Times Square", "cost" to 0.0),
                        mapOf("id" to "act-13", "trip_id" to tripId, "title" to "MoMA", "description" to "Museo de Arte Moderno con obras de Picasso, Warhol y más.", "date" to "2025-11-12", "time" to "10:00", "location" to "MoMA, Manhattan", "cost" to 30.0),
                        mapOf("id" to "act-14", "trip_id" to tripId, "title" to "Central Park", "description" to "Paseo en bicicleta por el icónico parque.", "date" to "2025-11-13", "time" to "10:00", "location" to "Central Park", "cost" to 15.0)
                    )
                    else -> emptyList()
                }
                gson.toJson(mapOf("data" to activities))
            }

            url.contains("/api/trips/") && url.contains("/expenses") -> {
                val tripId = url.substringAfter("/api/trips/").substringBefore("/expenses")
                val expenses = if (tripId == "trip-1") listOf(
                    mapOf("id" to "exp-1", "trip_id" to tripId, "description" to "Billetes avión", "amount" to 450.0, "category" to "Transporte", "date" to "2026-03-01"),
                    mapOf("id" to "exp-2", "trip_id" to tripId, "description" to "Hotel 7 noches", "amount" to 980.0, "category" to "Alojamiento", "date" to "2026-03-01")
                ) else emptyList<Map<String, Any>>()
                gson.toJson(mapOf("data" to expenses))
            }

            url.contains("/api/trips/") && url.contains("/documents") -> {
                val tripId = url.substringAfter("/api/trips/").substringBefore("/documents")
                val documents = if (tripId == "trip-1") listOf(
                    mapOf("id" to "doc-1", "trip_id" to tripId, "user_id" to "demo-user-1", "file_name" to "pasaporte.pdf", "mime_type" to "application/pdf", "file_size" to 1024000L, "description" to "Pasaporte vigente", "category" to "id"),
                    mapOf("id" to "doc-2", "trip_id" to tripId, "user_id" to "demo-user-1", "file_name" to "vuelo_paris.pdf", "mime_type" to "application/pdf", "file_size" to 512000L, "description" to "Confirmación de vuelo", "category" to "flight")
                ) else emptyList<Map<String, Any>>()
                gson.toJson(mapOf("data" to documents))
            }

            else -> gson.toJson(mapOf("data" to null))
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

object DemoMode {
    @Volatile var isEnabled = false
}
