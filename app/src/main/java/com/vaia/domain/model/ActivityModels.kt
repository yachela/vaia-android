package com.vaia.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Activity(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val time: String = "",
    val location: String,
    val cost: Double
) : Parcelable

data class ActivitySuggestion(
    val title: String,
    val description: String,
    val location: String,
    val cost: Double,
    val time: String = ""
)
