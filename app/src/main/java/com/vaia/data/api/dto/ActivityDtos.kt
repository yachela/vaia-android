package com.vaia.data.api.dto

import com.vaia.domain.model.Activity
import com.vaia.domain.model.ActivitySuggestion

// DTOs de red para actividades. La capa domain no conoce Gson.

data class ActivityDto(
    val id: String?,
    val title: String?,
    val description: String? = null,
    val date: String? = null,
    val time: String? = null,
    val location: String? = null,
    val cost: Double? = null
)

data class ActivitySuggestionDto(
    val title: String?,
    val description: String? = null,
    val location: String? = null,
    val cost: Double? = null,
    val time: String? = null
)

fun ActivityDto.toDomain(): Activity = Activity(
    id = id.orEmpty(),
    title = title ?: "Actividad sin título",
    description = description.orEmpty(),
    date = date.orEmpty(),
    time = time.orEmpty(),
    location = location.orEmpty(),
    cost = cost ?: 0.0
)

fun ActivitySuggestionDto.toDomain(): ActivitySuggestion = ActivitySuggestion(
    title = title.orEmpty(),
    description = description.orEmpty(),
    location = location.orEmpty(),
    cost = cost ?: 0.0,
    time = time.orEmpty()
)

data class CreateActivityRequest(
    val title: String,
    val description: String,
    val date: String,
    val time: String?,
    val location: String,
    val cost: Double
)

data class UpdateActivityRequest(
    val title: String,
    val description: String,
    val date: String,
    val time: String?,
    val location: String,
    val cost: Double
)

data class SuggestionsResponse(
    val data: List<ActivitySuggestionDto>
)
