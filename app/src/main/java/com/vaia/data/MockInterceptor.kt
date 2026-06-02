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

    private val checklistItems = mutableListOf(
        mutableMapOf<String, Any?>(
            "id" to "check-1",
            "name" to "Pasaporte",
            "isDefault" to true,
            "is_default" to true,
            "isCompleted" to true,
            "is_completed" to true,
            "position" to 1,
            "createdAt" to "2026-05-29T08:00:00Z",
            "created_at" to "2026-05-29T08:00:00Z",
            "updatedAt" to "2026-05-29T08:00:00Z",
            "updated_at" to "2026-05-29T08:00:00Z"
        ),
        mutableMapOf<String, Any?>(
            "id" to "check-2",
            "name" to "Billete de Avión",
            "isDefault" to true,
            "is_default" to true,
            "isCompleted" to false,
            "is_completed" to false,
            "position" to 2,
            "createdAt" to "2026-05-29T08:00:00Z",
            "created_at" to "2026-05-29T08:00:00Z",
            "updatedAt" to "2026-05-29T08:00:00Z",
            "updated_at" to "2026-05-29T08:00:00Z"
        ),
        mutableMapOf<String, Any?>(
            "id" to "check-3",
            "name" to "Reserva de Hotel",
            "isDefault" to true,
            "is_default" to true,
            "isCompleted" to false,
            "is_completed" to false,
            "position" to 3,
            "createdAt" to "2026-05-29T08:00:00Z",
            "created_at" to "2026-05-29T08:00:00Z",
            "updatedAt" to "2026-05-29T08:00:00Z",
            "updated_at" to "2026-05-29T08:00:00Z"
        )
    )

    private val packingItems = mutableListOf(
        mutableMapOf<String, Any?>(
            "id" to "item-1",
            "name" to "Camisetas",
            "category" to "ROPA",
            "isPacked" to false,
            "isSuggested" to false,
            "suggestionReason" to null,
            "createdAt" to "2026-05-29T08:00:00Z",
            "updatedAt" to "2026-05-29T08:00:00Z"
        ),
        mutableMapOf<String, Any?>(
            "id" to "item-2",
            "name" to "Pantalones",
            "category" to "ROPA",
            "isPacked" to true,
            "isSuggested" to false,
            "suggestionReason" to null,
            "createdAt" to "2026-05-29T08:00:00Z",
            "updatedAt" to "2026-05-29T08:00:00Z"
        ),
        mutableMapOf<String, Any?>(
            "id" to "item-3",
            "name" to "Cargador de móvil",
            "category" to "TECNOLOGIA",
            "isPacked" to false,
            "isSuggested" to true,
            "suggestionReason" to "Recomendado para tu viaje",
            "createdAt" to "2026-05-29T08:00:00Z",
            "updatedAt" to "2026-05-29T08:00:00Z"
        )
    )

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

        val bodyStr = try {
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            buffer.readUtf8()
        } catch (_: Exception) { "" }

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
            url.contains("/api/trips/") && url.contains("/packing-list") && method == "GET" -> {
                val itemsByCategory = packingItems.groupBy { it["category"] as String }
                    .map { (category, items) ->
                        mapOf(
                            "category" to category,
                            "items" to items
                        )
                    }
                val totalCount = packingItems.size
                val packedCount = packingItems.count { it["isPacked"] == true }
                val percentage = if (totalCount > 0) (packedCount * 100) / totalCount else 0
                gson.toJson(mapOf("data" to mapOf(
                    "id" to "demo-packing-list",
                    "tripId" to "demo",
                    "itemsByCategory" to itemsByCategory,
                    "progress" to mapOf(
                        "total" to totalCount,
                        "packed" to packedCount,
                        "percentage" to percentage
                    ),
                    "createdAt" to "2026-05-29T08:00:00Z",
                    "updatedAt" to "2026-05-29T08:00:00Z"
                )))
            }

            url.contains("/api/trips/") && url.contains("/packing-list/items") && method == "POST" -> {
                val requestMap = try {
                    gson.fromJson(bodyStr, mapOf<String, Any>().javaClass)
                } catch (_: Exception) {
                    emptyMap()
                }
                val name = requestMap["name"] as? String ?: "Nuevo ítem"
                val category = requestMap["category"] as? String ?: "ROPA"
                val newItem = mutableMapOf<String, Any?>(
                    "id" to "item-${System.currentTimeMillis()}",
                    "name" to name,
                    "category" to category,
                    "isPacked" to false,
                    "isSuggested" to false,
                    "suggestionReason" to null,
                    "createdAt" to "2026-05-29T08:00:00Z",
                    "updatedAt" to "2026-05-29T08:00:00Z"
                )
                packingItems.add(newItem)
                gson.toJson(mapOf("data" to mapOf("item" to newItem)))
            }

            url.contains("/api/packing-list/items/") && url.contains("/toggle") && method == "PATCH" -> {
                val itemId = url.substringAfter("/api/packing-list/items/").substringBefore("/toggle")
                var updatedItem: Map<String, Any?>? = null
                for (i in packingItems.indices) {
                    if (packingItems[i]["id"] == itemId) {
                        val item = packingItems[i].toMutableMap()
                        val newPackedState = !(item["isPacked"] as Boolean)
                        item["isPacked"] = newPackedState
                        item["updatedAt"] = "2026-05-29T08:00:00Z"
                        packingItems[i] = item
                        updatedItem = item
                        break
                    }
                }
                if (updatedItem == null) {
                    updatedItem = mapOf(
                        "id" to itemId,
                        "name" to "Ítem",
                        "category" to "ROPA",
                        "isPacked" to true,
                        "isSuggested" to false,
                        "suggestionReason" to null,
                        "createdAt" to "2026-05-29T08:00:00Z",
                        "updatedAt" to "2026-05-29T08:00:00Z"
                    )
                }
                gson.toJson(mapOf("data" to mapOf("item" to updatedItem)))
            }

            url.contains("/api/packing-list/items/") && method == "DELETE" -> {
                val itemId = url.substringAfter("/api/packing-list/items/").substringBefore("?")
                packingItems.removeAll { it["id"] == itemId }
                gson.toJson(mapOf("data" to null))
            }

            // ── Weather Suggestions ───────────────────────────────────────────
            url.contains("/api/trips/") && url.contains("/weather-suggestions") -> {
                gson.toJson(mapOf("data" to mapOf(
                    "suggestions" to listOf(
                        mapOf(
                            "name" to "Paraguas",
                            "category" to "ROPA",
                            "suggestionReason" to "Se pronostican lluvias en tu destino"
                        ),
                        mapOf(
                            "name" to "Gafas de sol",
                            "category" to "ROPA",
                            "suggestionReason" to "Días soleados previstos"
                        )
                    )
                )))
            }

            // ── Suggestions ───────────────────────────────────────────────────
            url.contains("/api/trips/") && url.contains("/suggestions") ->
                gson.toJson(mapOf("data" to emptyList<Any>()))

            // ── Checklist ─────────────────────────────────────────────────────
            url.contains("/api/trips/") && url.contains("/checklist") && method == "GET" -> {
                val completedCount = checklistItems.count { it["isCompleted"] == true }
                val totalCount = checklistItems.size
                val percentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0
                gson.toJson(mapOf("data" to mapOf(
                    "id" to "demo-checklist",
                    "trip_id" to "demo",
                    "items" to checklistItems,
                    "progress" to mapOf(
                        "completed" to completedCount,
                        "total" to totalCount,
                        "percentage" to percentage
                    )
                )))
            }

            url.contains("/api/trips/") && url.contains("/checklist/items") && method == "POST" -> {
                val name = try {
                    gson.fromJson(bodyStr, mapOf<String, Any>().javaClass)["name"] as? String ?: "Nuevo documento"
                } catch (_: Exception) {
                    "Nuevo documento"
                }
                val newItem = mutableMapOf<String, Any?>(
                    "id" to "check-${System.currentTimeMillis()}",
                    "name" to name,
                    "isDefault" to false,
                    "is_default" to false,
                    "isCompleted" to false,
                    "is_completed" to false,
                    "position" to checklistItems.size + 1,
                    "createdAt" to "2026-05-29T08:00:00Z",
                    "created_at" to "2026-05-29T08:00:00Z",
                    "updatedAt" to "2026-05-29T08:00:00Z",
                    "updated_at" to "2026-05-29T08:00:00Z"
                )
                checklistItems.add(newItem)
                gson.toJson(mapOf("data" to newItem))
            }

            url.contains("/api/checklist/items/") && url.contains("/complete") && method == "PATCH" -> {
                val itemId = url.substringAfter("/api/checklist/items/").substringBefore("/complete")
                val isCompleted = bodyStr.contains("true")
                var updatedItem: Map<String, Any?>? = null
                for (i in checklistItems.indices) {
                    if (checklistItems[i]["id"] == itemId) {
                        val item = checklistItems[i].toMutableMap()
                        item["isCompleted"] = isCompleted
                        item["is_completed"] = isCompleted
                        item["updatedAt"] = "2026-05-29T08:00:00Z"
                        item["updated_at"] = "2026-05-29T08:00:00Z"
                        checklistItems[i] = item
                        updatedItem = item
                        break
                    }
                }
                if (updatedItem == null) {
                    updatedItem = mapOf(
                        "id" to itemId,
                        "name" to "Documento",
                        "isDefault" to false,
                        "is_default" to false,
                        "isCompleted" to isCompleted,
                        "is_completed" to isCompleted,
                        "position" to 1,
                        "createdAt" to "2026-05-29T08:00:00Z",
                        "created_at" to "2026-05-29T08:00:00Z",
                        "updatedAt" to "2026-05-29T08:00:00Z",
                        "updated_at" to "2026-05-29T08:00:00Z"
                    )
                }
                gson.toJson(mapOf("data" to updatedItem))
            }

            url.contains("/api/checklist/items/") && method == "DELETE" -> {
                val itemId = url.substringAfter("/api/checklist/items/").substringBefore("?")
                checklistItems.removeAll { it["id"] == itemId }
                gson.toJson(mapOf("data" to null))
            }

            url.contains("/api/checklist/items/") && url.contains("/documents") && method == "POST" -> {
                val itemId = url.substringAfter("/api/checklist/items/").substringBefore("/documents")
                val documentId = "doc-${System.currentTimeMillis()}"
                val documentName = if (url.contains("/from-drive")) "Documento de Drive.pdf" else "Documento Subido.pdf"
                val source = if (url.contains("/from-drive")) "google_drive" else "local"
                val document = mapOf(
                    "id" to documentId,
                    "checklist_item_id" to itemId,
                    "file_name" to documentName,
                    "file_path" to "/mock/path/$documentName",
                    "mime_type" to "application/pdf",
                    "file_size" to 102400L,
                    "source" to source,
                    "google_drive_file_id" to if (source == "google_drive") "drive-123" else null,
                    "uploaded_by" to "Viajero Demo",
                    "created_at" to "2026-05-29T08:00:00Z",
                    "updated_at" to "2026-05-29T08:00:00Z"
                )
                // Update item in memory
                for (i in checklistItems.indices) {
                    if (checklistItems[i]["id"] == itemId) {
                        val item = checklistItems[i].toMutableMap()
                        item["document"] = document
                        item["isCompleted"] = true
                        item["is_completed"] = true
                        checklistItems[i] = item
                        break
                    }
                }
                gson.toJson(mapOf("data" to document))
            }

            url.contains("/api/checklist/documents/") && url.contains("/preview") && method == "GET" -> {
                gson.toJson(mapOf("data" to mapOf(
                    "url" to "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf",
                    "expires_at" to "2026-05-29T09:00:00Z"
                )))
            }

            url.contains("/api/checklist/documents/") && method == "DELETE" -> {
                val documentId = url.substringAfter("/api/checklist/documents/").substringBefore("?")
                // Remove document from the checklist item in memory
                for (i in checklistItems.indices) {
                    val doc = checklistItems[i]["document"] as? Map<*, *>
                    if (doc != null && doc["id"] == documentId) {
                        val item = checklistItems[i].toMutableMap()
                        item["document"] = null
                        item["isCompleted"] = false
                        item["is_completed"] = false
                        checklistItems[i] = item
                        break
                    }
                }
                gson.toJson(mapOf("data" to null))
            }

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
    @Volatile var isEnabled = true
}
