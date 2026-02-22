package com.vaia.domain.repository

import com.vaia.domain.model.*

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthTokens>
    suspend fun register(name: String, email: String, password: String, passwordConfirmation: String): Result<AuthTokens>
    suspend fun logout(): Result<Unit>
    suspend fun getCurrentUser(): Result<User>
    suspend fun updateProfile(
        name: String,
        bio: String?,
        country: String?,
        language: String?,
        currency: String?,
        avatarUrl: String?
    ): Result<User>
    fun isLoggedIn(): Boolean
    fun getAccessToken(): String?
}

interface TripRepository {
    suspend fun getTrips(): Result<List<Trip>>
    suspend fun getTrip(tripId: String): Result<Trip>
    suspend fun createTrip(title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip>
    suspend fun updateTrip(tripId: String, title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip>
    suspend fun deleteTrip(tripId: String): Result<Unit>
}

interface ActivityRepository {
    suspend fun getActivities(tripId: String): Result<List<Activity>>
    suspend fun getActivity(tripId: String, activityId: String): Result<Activity>
    suspend fun createActivity(tripId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity>
    suspend fun updateActivity(tripId: String, activityId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity>
    suspend fun deleteActivity(tripId: String, activityId: String): Result<Unit>
}

interface ExpenseRepository {
    suspend fun getExpenses(tripId: String): Result<List<Expense>>
    suspend fun getExpense(tripId: String, expenseId: String): Result<Expense>
    suspend fun createExpense(tripId: String, amount: Double, description: String, date: String, category: String, receiptImage: ByteArray?): Result<Expense>
    suspend fun updateExpense(tripId: String, expenseId: String, amount: Double, description: String, date: String, category: String, receiptImage: ByteArray?): Result<Expense>
    suspend fun deleteExpense(tripId: String, expenseId: String): Result<Unit>
}
