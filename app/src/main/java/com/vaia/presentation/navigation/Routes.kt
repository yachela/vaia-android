package com.vaia.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable data object Login
@Serializable data object Register
@Serializable data object Home
@Serializable data object Explore
@Serializable data object Trips
@Serializable data object Calendar
@Serializable data object Organizer
@Serializable data object Profile
@Serializable data class Activities(val tripId: String)
@Serializable data class Roadmap(val tripId: String)
@Serializable data class Expenses(val tripId: String)
@Serializable data class TripDocuments(val tripId: String)
@Serializable data class TripChecklist(val tripId: String, val tripTitle: String)
@Serializable data class PackingList(val tripId: String, val tripName: String, val daysUntilDeparture: Int)
@Serializable data class DocumentPreview(val documentUri: String, val documentName: String)
@Serializable data object Onboarding


