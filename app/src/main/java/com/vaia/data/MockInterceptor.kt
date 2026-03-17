package com.vaia.data

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import com.google.gson.Gson

class MockInterceptor : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()

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
                        "destination" to "París, Francia",
                        "start_date" to "2026-04-15",
                        "end_date" to "2026-04-22",
                        "budget" to 2500.0,
                        "total_expenses" to 1850.0,
                        "activities_count" to 8,
                        "expenses_count" to 12
                    ),
                    mapOf(
                        "id" to "trip-2",
                        "title" to "Tour por Italia",
                        "destination" to "Roma, Italia",
                        "start_date" to "2026-06-01",
                        "end_date" to "2026-06-10",
                        "budget" to 3000.0,
                        "total_expenses" to 1200.0,
                        "activities_count" to 5,
                        "expenses_count" to 7
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
                val activities = if (tripId == "trip-1") listOf(
                    mapOf("id" to "act-1", "trip_id" to tripId, "title" to "Visita a la Torre Eiffel", "description" to "Tour guiadas", "date" to "2026-04-15", "time" to "10:00", "location" to "Torre Eiffel", "cost" to 25.0),
                    mapOf("id" to "act-2", "trip_id" to tripId, "title" to "Cena en Montmartre", "description" to "Cena tradicional", "date" to "2026-04-16", "time" to "20:00", "location" to "Montmartre", "cost" to 60.0)
                ) else emptyList<Map<String, Any>>()
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
