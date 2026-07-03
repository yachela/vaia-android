package com.vaia.domain.repository

import com.vaia.domain.model.BudgetAdvice
import com.vaia.domain.model.Trip

interface TripRepository {
    suspend fun getTrips(): Result<List<Trip>>
    suspend fun getTripsPage(page: Int): Result<Pair<List<Trip>, Boolean>>
    suspend fun getTrip(tripId: String): Result<Trip>
    suspend fun createTrip(title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip>
    suspend fun updateTrip(tripId: String, title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip>
    suspend fun deleteTrip(tripId: String): Result<Unit>
    suspend fun exportItineraryPdf(tripId: String): Result<ByteArray>
    suspend fun exportExpensesCsv(tripId: String): Result<ByteArray>
    suspend fun getBudgetAdvice(tripId: String): Result<BudgetAdvice>
}
