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
        val method = request.method

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

            // ── Auth ────────────────────────────────────────────────────────
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

            url.contains("/api/user") && method == "GET" -> gson.toJson(mapOf(
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

            url.contains("/api/user") && (method == "PUT" || method == "PATCH") -> gson.toJson(mapOf(
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

            // ── Activities (nested, must come before trips) ─────────────────
            url.contains("/api/trips/") && url.contains("/activities/") && method == "DELETE" ->
                gson.toJson(mapOf("data" to null))

            url.contains("/api/trips/") && url.contains("/activities/") && (method == "PUT" || method == "PATCH") -> {
                gson.toJson(mapOf(
                    "data" to mapOf(
                        "id" to "act-new",
                        "trip_id" to "demo",
                        "title" to "Actividad actualizada",
                        "description" to "",
                        "date" to "2026-04-15",
                        "time" to "10:00",
                        "location" to "",
                        "cost" to 0.0
                    )
                ))
            }

            url.contains("/api/trips/") && url.contains("/activities") && method == "POST" -> {
                // Crear actividad — devolver actividad creada
                val tripId = url.substringAfter("/api/trips/").substringBefore("/activities")
                gson.toJson(mapOf(
                    "data" to mapOf(
                        "id" to "act-${System.currentTimeMillis()}",
                        "trip_id" to tripId,
                        "title" to "Nueva actividad",
                        "description" to "",
                        "date" to "2026-04-15",
                        "time" to "10:00",
                        "location" to "",
                        "cost" to 0.0
                    )
                ))
            }

            url.contains("/api/trips/") && url.contains("/activities") && method == "GET" -> {
                val tripId = url.substringAfter("/api/trips/").substringBefore("/activities")
                val activities: List<Map<String, Any>> = when (tripId) {
                    "trip-1" -> listOf(
                        mapOf("id" to "act-1", "trip_id" to tripId, "title" to "Visita a la Torre Eiffel", "description" to "Tour guiado al monumento más famoso de París, con vistas panorámicas desde la cima.", "date" to "2026-04-15", "time" to "10:00", "location" to "Torre Eiffel, París", "cost" to 28.0),
                        mapOf("id" to "act-2", "trip_id" to tripId, "title" to "Museo del Louvre", "description" to "Recorrido por las colecciones más importantes del mundo.", "date" to "2026-04-16", "time" to "09:30", "location" to "Musée du Louvre", "cost" to 22.0),
                        mapOf("id" to "act-3", "trip_id" to tripId, "title" to "Cena en Montmartre", "description" to "Cena tradicional francesa en el barrio artístico.", "date" to "2026-04-16", "time" to "20:00", "location" to "Montmartre, París", "cost" to 65.0),
                        mapOf("id" to "act-4", "trip_id" to tripId, "title" to "Palacio de Versalles", "description" to "Excursión de día completo al palacio y sus jardines.", "date" to "2026-04-17", "time" to "08:00", "location" to "Versalles", "cost" to 20.0),
                        mapOf("id" to "act-5", "trip_id" to tripId, "title" to "Crucero por el Sena", "description" to "Crucero nocturno con vistas iluminadas.", "date" to "2026-04-18", "time" to "21:00", "location" to "Río Sena", "cost" to 18.0),
                        mapOf("id" to "act-6", "trip_id" to tripId, "title" to "Barrio de Le Marais", "description" to "Paseo por el barrio histórico.", "date" to "2026-04-19", "time" to "11:00", "location" to "Le Marais, París", "cost" to 0.0)
                    )
                    "trip-2" -> listOf(
                        mapOf("id" to "act-7", "trip_id" to tripId, "title" to "Coliseo Romano", "description" to "Visita al anfiteatro más grande del Imperio Romano.", "date" to "2026-06-01", "time" to "09:00", "location" to "Coliseo, Roma", "cost" to 18.0),
                        mapOf("id" to "act-8", "trip_id" to tripId, "title" to "Ciudad del Vaticano", "description" to "Museos Vaticanos y la Capilla Sixtina.", "date" to "2026-06-02", "time" to "08:30", "location" to "Vaticano", "cost" to 25.0),
                        mapOf("id" to "act-9", "trip_id" to tripId, "title" to "Galería Uffizi", "description" to "Arte renacentista en Florencia.", "date" to "2026-06-05", "time" to "10:00", "location" to "Galería Uffizi, Florencia", "cost" to 20.0),
                        mapOf("id" to "act-10", "trip_id" to tripId, "title" to "Paseo en góndola", "description" to "Recorrido por los canales de Venecia.", "date" to "2026-06-09", "time" to "17:00", "location" to "Gran Canal, Venecia", "cost" to 80.0)
                    )
                    "trip-4" -> listOf(
                        mapOf("id" to "act-11", "trip_id" to tripId, "title" to "Estatua de la Libertad", "description" to "Ferry y visita a la isla.", "date" to "2025-11-11", "time" to "09:00", "location" to "Liberty Island", "cost" to 24.0),
                        mapOf("id" to "act-12", "trip_id" to tripId, "title" to "Times Square", "description" to "Exploración del corazón de Manhattan.", "date" to "2025-11-11", "time" to "19:00", "location" to "Times Square", "cost" to 0.0),
                        mapOf("id" to "act-13", "trip_id" to tripId, "title" to "MoMA", "description" to "Museo de Arte Moderno.", "date" to "2025-11-12", "time" to "10:00", "location" to "MoMA, Manhattan", "cost" to 30.0),
                        mapOf("id" to "act-14", "trip_id" to tripId, "title" to "Central Park", "description" to "Paseo en bicicleta.", "date" to "2025-11-13", "time" to "10:00", "location" to "Central Park", "cost" to 15.0)
                    )
                    else -> emptyList()
                }
                gson.toJson(mapOf("data" to activities))
            }

            // ── Expenses ─────────────────────────────────────────────────────
            url.contains("/api/trips/") && url.contains("/expenses/") && method == "DELETE" ->
                gson.toJson(mapOf("data" to null))

            url.contains("/api/trips/") && url.contains("/expenses") && method == "POST" -> {
                val tripId = url.substringAfter("/api/trips/").substringBefore("/expenses")
                gson.toJson(mapOf(
                    "data" to mapOf(
                        "id" to "exp-${System.currentTimeMillis()}",
                        "trip_id" to tripId,
                        "description" to "Nuevo gasto",
                        "amount" to 0.0,
                        "category" to "Otros",
                        "date" to "2026-04-15"
                    )
                ))
            }

            url.contains("/api/trips/") && url.contains("/expenses") && method == "GET" -> {
                val tripId = url.substringAfter("/api/trips/").substringBefore("/expenses")
                val expenses = if (tripId == "trip-1") listOf(
                    mapOf("id" to "exp-1", "trip_id" to tripId, "description" to "Billetes avión", "amount" to 450.0, "category" to "Transporte", "date" to "2026-03-01"),
                    mapOf("id" to "exp-2", "trip_id" to tripId, "description" to "Hotel 7 noches", "amount" to 980.0, "category" to "Alojamiento", "date" to "2026-03-01")
                ) else emptyList<Map<String, Any>>()
                gson.toJson(mapOf("data" to expenses))
            }

            // ── Documents ─────────────────────────────────────────────────────
            url.contains("/api/trips/") && url.contains("/documents") && method == "GET" -> {
                val tripId = url.substringAfter("/api/trips/").substringBefore("/documents")
                val documents = if (tripId == "trip-1") listOf(
                    mapOf("id" to "doc-1", "trip_id" to tripId, "user_id" to "demo-user-1", "file_name" to "pasaporte.pdf", "mime_type" to "application/pdf", "file_size" to 1024000L, "description" to "Pasaporte vigente", "category" to "id"),
                    mapOf("id" to "doc-2", "trip_id" to tripId, "user_id" to "demo-user-1", "file_name" to "vuelo_paris.pdf", "mime_type" to "application/pdf", "file_size" to 512000L, "description" to "Confirmación de vuelo", "category" to "flight")
                ) else emptyList<Map<String, Any>>()
                gson.toJson(mapOf("data" to documents))
            }

            // ── Packing List ──────────────────────────────────────────────────
            url.contains("/api/trips/") && url.contains("/packing-list") -> {
                gson.toJson(mapOf("data" to mapOf(
                    "items" to emptyList<Any>()
                )))
            }

            // ── Suggestions ───────────────────────────────────────────────────
            url.contains("/api/trips/") && url.contains("/suggestions") ->
                gson.toJson(mapOf("data" to emptyList<Any>()))

            // ── Checklist ─────────────────────────────────────────────────────
            url.contains("/api/trips/") && url.contains("/checklist") ->
                gson.toJson(mapOf("data" to mapOf("items" to emptyList<Any>())))

            // ── Individual trip (GET / PUT / DELETE) ──────────────────────────
            url.matches(Regex(".*/api/trips/[^/]+$")) && method == "DELETE" ->
                gson.toJson(mapOf("data" to null))

            url.matches(Regex(".*/api/trips/[^/]+$")) && (method == "PUT" || method == "PATCH") -> {
                val tripId = url.substringAfterLast("/api/trips/").substringBefore("?")
                gson.toJson(mapOf(
                    "data" to mapOf(
                        "id" to tripId,
                        "title" to "Viaje actualizado",
                        "destination" to "Destino",
                        "start_date" to "2026-04-15",
                        "end_date" to "2026-04-22",
                        "budget" to 0.0,
                        "total_expenses" to 0.0,
                        "activities_count" to 0,
                        "expenses_count" to 0
                    )
                ))
            }

            url.matches(Regex(".*/api/trips/[^/]+$")) && method == "GET" -> {
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

            // ── Trips list (GET) ──────────────────────────────────────────────
            url.contains("/api/trips") && method == "GET" -> gson.toJson(mapOf(
                "data" to listOf(
                    mapOf("id" to "trip-1", "title" to "Viaje a París", "destination" to "París, Madrid", "start_date" to "2026-04-15", "end_date" to "2026-04-22", "budget" to 2500.0, "total_expenses" to 1850.0, "activities_count" to 6, "expenses_count" to 4),
                    mapOf("id" to "trip-2", "title" to "Tour por Italia", "destination" to "Roma, Florencia, Venecia", "start_date" to "2026-06-01", "end_date" to "2026-06-12", "budget" to 3200.0, "total_expenses" to 1400.0, "activities_count" to 8, "expenses_count" to 6),
                    mapOf("id" to "trip-3", "title" to "Escapada a Bali", "destination" to "Bali, Indonesia", "start_date" to "2026-08-15", "end_date" to "2026-08-25", "budget" to 4000.0, "total_expenses" to 0.0, "activities_count" to 0, "expenses_count" to 0),
                    mapOf("id" to "trip-4", "title" to "Nueva York Express", "destination" to "Nueva York, EE.UU.", "start_date" to "2025-11-10", "end_date" to "2025-11-17", "budget" to 3500.0, "total_expenses" to 3210.0, "activities_count" to 10, "expenses_count" to 15)
                )
            ))

            // ── Create trip (POST) ────────────────────────────────────────────
            url.contains("/api/trips") && method == "POST" -> {
                val newId = "trip-${System.currentTimeMillis()}"
                gson.toJson(mapOf(
                    "data" to mapOf(
                        "id" to newId,
                        "title" to "Nuevo viaje",
                        "destination" to "Destino",
                        "start_date" to "2026-04-15",
                        "end_date" to "2026-04-22",
                        "budget" to 0.0,
                        "total_expenses" to 0.0,
                        "activities_count" to 0,
                        "expenses_count" to 0
                    )
                ))
            }

            // ── Fallback ──────────────────────────────────────────────────────
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
