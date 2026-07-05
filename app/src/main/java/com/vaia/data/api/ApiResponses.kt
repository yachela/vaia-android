package com.vaia.data.api

import com.google.gson.annotations.SerializedName

// Envoltorios genéricos de las respuestas de la API

data class ApiResponse<T>(
    val data: T? = null,
    val message: String? = null,
    val errors: Map<String, List<String>>? = null
)

data class PaginationMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
    @SerializedName("per_page") val perPage: Int = 15
)

data class PaginatedResponse<T>(
    val data: List<T>,
    val links: Map<String, String?>? = null,
    val meta: PaginationMeta? = null
)
