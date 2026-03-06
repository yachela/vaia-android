package com.vaia.presentation.viewmodel

import com.vaia.domain.model.Activity
import com.vaia.domain.model.AuthTokens
import com.vaia.domain.model.Trip
import com.vaia.domain.model.User
import com.vaia.domain.repository.ActivityRepository
import com.vaia.domain.repository.AuthRepository
import com.vaia.domain.repository.TripRepository
import com.vaia.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `createTrip with aventura seeds three template activities`() = runTest {
        val createdTrip = Trip(
            id = "trip-1",
            title = "Patagonia",
            destination = "Bariloche",
            startDate = "2026-04-10",
            endDate = "2026-04-14",
            budget = 1200.0
        )
        val tripRepo = FakeTripRepository(createdTrip)
        val activityRepo = RecordingActivityRepository()
        val authRepo = FakeAuthRepository()
        val viewModel = TripsViewModel(tripRepo, authRepo, activityRepo)

        viewModel.createTrip(
            title = createdTrip.title,
            destination = createdTrip.destination,
            startDate = createdTrip.startDate,
            endDate = createdTrip.endDate,
            budget = createdTrip.budget,
            templateType = "aventura"
        )
        advanceUntilIdle()

        assertTrue(viewModel.createTripState.value is TripsViewModel.CreateTripState.Success)
        assertEquals(3, activityRepo.created.size)
        assertEquals("trip-1", activityRepo.created.first().tripId)
        assertEquals("2026-04-10", activityRepo.created.first().date)
    }

    private data class ActivityCall(
        val tripId: String,
        val title: String,
        val description: String,
        val date: String,
        val time: String,
        val location: String,
        val cost: Double
    )

    private class RecordingActivityRepository : ActivityRepository {
        val created = mutableListOf<ActivityCall>()

        override suspend fun getActivities(tripId: String): Result<List<Activity>> = Result.success(emptyList())
        override suspend fun getActivity(tripId: String, activityId: String): Result<Activity> =
            Result.failure(NotImplementedError())

        override suspend fun createActivity(
            tripId: String,
            title: String,
            description: String,
            date: String,
            time: String,
            location: String,
            cost: Double
        ): Result<Activity> {
            created += ActivityCall(tripId, title, description, date, time, location, cost)
            return Result.failure(NotImplementedError())
        }

        override suspend fun updateActivity(
            tripId: String,
            activityId: String,
            title: String,
            description: String,
            date: String,
            time: String,
            location: String,
            cost: Double
        ): Result<Activity> = Result.failure(NotImplementedError())

        override suspend fun deleteActivity(tripId: String, activityId: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeTripRepository(private val createdTrip: Trip) : TripRepository {
        override suspend fun getTrips(): Result<List<Trip>> = Result.success(listOf(createdTrip))
        override suspend fun getTripsPage(page: Int): Result<Pair<List<Trip>, Boolean>> =
            Result.success(Pair(listOf(createdTrip), false))
        override suspend fun getTrip(tripId: String): Result<Trip> = Result.success(createdTrip)
        override suspend fun createTrip(
            title: String,
            destination: String,
            startDate: String,
            endDate: String,
            budget: Double
        ): Result<Trip> = Result.success(createdTrip)

        override suspend fun updateTrip(
            tripId: String,
            title: String,
            destination: String,
            startDate: String,
            endDate: String,
            budget: Double
        ): Result<Trip> = Result.success(createdTrip)

        override suspend fun deleteTrip(tripId: String): Result<Unit> = Result.success(Unit)
        override suspend fun exportItineraryPdf(tripId: String): Result<ByteArray> =
            Result.success(ByteArray(0))
        override suspend fun exportExpensesCsv(tripId: String): Result<ByteArray> =
            Result.success(ByteArray(0))
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun login(email: String, password: String): Result<AuthTokens> =
            Result.failure(NotImplementedError())

        override suspend fun register(
            name: String,
            email: String,
            password: String,
            passwordConfirmation: String
        ): Result<AuthTokens> = Result.failure(NotImplementedError())

        override suspend fun logout(): Result<Unit> = Result.success(Unit)
        override suspend fun getCurrentUser(): Result<User> = Result.failure(NotImplementedError())
        override suspend fun updateProfile(
            name: String,
            bio: String?,
            country: String?,
            language: String?,
            currency: String?
        ): Result<User> = Result.failure(NotImplementedError())

        override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Result<User> =
            Result.failure(NotImplementedError())

        override fun isLoggedIn(): Boolean = true
        override fun getAccessToken(): String? = null
    }
}
