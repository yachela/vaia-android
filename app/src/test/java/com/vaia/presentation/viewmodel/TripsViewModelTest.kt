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

    // ── loadMoreTrips ────────────────────────────────────────────────────────

    @Test
    fun `loadMoreTrips appends trips and increments page`() = runTest {
        val page1Trip = Trip("t1", "Paris", "Paris", "2026-06-01", "2026-06-05", 1000.0)
        val page2Trip = Trip("t2", "Roma", "Roma", "2026-07-01", "2026-07-05", 800.0)
        val paginatedRepo = PaginatedTripRepository(
            page1 = listOf(page1Trip) to true,
            page2 = listOf(page2Trip) to false
        )
        val vm = TripsViewModel(paginatedRepo, FakeAuthRepository(), FakeActivityRepository())

        vm.loadTrips()
        advanceUntilIdle()
        assertEquals(listOf(page1Trip), vm.trips.value)
        assertTrue(vm.hasMorePages.value)

        vm.loadMoreTrips()
        advanceUntilIdle()
        assertEquals(listOf(page1Trip, page2Trip), vm.trips.value)
        assertTrue(!vm.hasMorePages.value)
    }

    @Test
    fun `loadMoreTrips does nothing when hasMorePages is false`() = runTest {
        val trip = Trip("t1", "Paris", "Paris", "2026-06-01", "2026-06-05", 1000.0)
        val repo = FakeTripRepository(trip)
        val vm = TripsViewModel(repo, FakeAuthRepository(), FakeActivityRepository())

        vm.loadTrips()
        advanceUntilIdle()
        // hasMorePages is false by default in FakeTripRepository

        vm.loadMoreTrips()
        advanceUntilIdle()

        assertEquals(listOf(trip), vm.trips.value) // no duplicates
    }

    // ── exportItinerary / exportExpenses ─────────────────────────────────────

    @Test
    fun `exportItinerary sets PdfReady on success`() = runTest {
        val pdfBytes = byteArrayOf(10, 20, 30)
        val repo = ExportTripRepository(pdfResult = Result.success(pdfBytes))
        val vm = TripsViewModel(repo, FakeAuthRepository(), FakeActivityRepository())

        vm.exportItinerary("trip-1")
        advanceUntilIdle()

        val state = vm.exportState.value
        assertTrue(state is TripsViewModel.ExportState.PdfReady)
        assertTrue((state as TripsViewModel.ExportState.PdfReady).bytes.contentEquals(pdfBytes))
        assertEquals("trip-1", state.tripId)
    }

    @Test
    fun `exportItinerary sets Error on failure`() = runTest {
        val repo = ExportTripRepository(pdfResult = Result.failure(RuntimeException("pdf falla")))
        val vm = TripsViewModel(repo, FakeAuthRepository(), FakeActivityRepository())

        vm.exportItinerary("trip-1")
        advanceUntilIdle()

        val state = vm.exportState.value
        assertTrue(state is TripsViewModel.ExportState.Error)
        assertEquals("pdf falla", (state as TripsViewModel.ExportState.Error).message)
    }

    @Test
    fun `exportExpenses sets CsvReady on success`() = runTest {
        val csvBytes = byteArrayOf(5, 6, 7)
        val repo = ExportTripRepository(csvResult = Result.success(csvBytes))
        val vm = TripsViewModel(repo, FakeAuthRepository(), FakeActivityRepository())

        vm.exportExpenses("trip-1")
        advanceUntilIdle()

        val state = vm.exportState.value
        assertTrue(state is TripsViewModel.ExportState.CsvReady)
        assertTrue((state as TripsViewModel.ExportState.CsvReady).bytes.contentEquals(csvBytes))
    }

    @Test
    fun `exportExpenses sets Error on failure`() = runTest {
        val repo = ExportTripRepository(csvResult = Result.failure(RuntimeException("csv falla")))
        val vm = TripsViewModel(repo, FakeAuthRepository(), FakeActivityRepository())

        vm.exportExpenses("trip-1")
        advanceUntilIdle()

        val state = vm.exportState.value
        assertTrue(state is TripsViewModel.ExportState.Error)
        assertEquals("csv falla", (state as TripsViewModel.ExportState.Error).message)
    }

    // ── createTrip ────────────────────────────────────────────────────────────

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
        override suspend fun getSuggestions(tripId: String): Result<List<com.vaia.domain.model.ActivitySuggestion>> = Result.success(emptyList())
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

    private class FakeActivityRepository : ActivityRepository {
        override suspend fun getActivities(tripId: String): Result<List<Activity>> = Result.success(emptyList())
        override suspend fun getActivity(tripId: String, activityId: String): Result<Activity> =
            Result.failure(NotImplementedError())
        override suspend fun createActivity(tripId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> =
            Result.failure(NotImplementedError())
        override suspend fun updateActivity(tripId: String, activityId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> =
            Result.failure(NotImplementedError())
        override suspend fun deleteActivity(tripId: String, activityId: String): Result<Unit> = Result.success(Unit)
        override suspend fun getSuggestions(tripId: String): Result<List<com.vaia.domain.model.ActivitySuggestion>> = Result.success(emptyList())
    }

    private class PaginatedTripRepository(
        private val page1: Pair<List<Trip>, Boolean>,
        private val page2: Pair<List<Trip>, Boolean>
    ) : TripRepository {
        override suspend fun getTrips(): Result<List<Trip>> = Result.success(emptyList())
        override suspend fun getTripsPage(page: Int): Result<Pair<List<Trip>, Boolean>> =
            if (page == 1) Result.success(page1) else Result.success(page2)
        override suspend fun getTrip(tripId: String): Result<Trip> = Result.failure(NotImplementedError())
        override suspend fun createTrip(title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> =
            Result.failure(NotImplementedError())
        override suspend fun updateTrip(tripId: String, title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> =
            Result.failure(NotImplementedError())
        override suspend fun deleteTrip(tripId: String): Result<Unit> = Result.success(Unit)
        override suspend fun exportItineraryPdf(tripId: String): Result<ByteArray> = Result.success(ByteArray(0))
        override suspend fun exportExpensesCsv(tripId: String): Result<ByteArray> = Result.success(ByteArray(0))
    }

    private class ExportTripRepository(
        private val pdfResult: Result<ByteArray> = Result.success(ByteArray(0)),
        private val csvResult: Result<ByteArray> = Result.success(ByteArray(0))
    ) : TripRepository {
        override suspend fun getTrips(): Result<List<Trip>> = Result.success(emptyList())
        override suspend fun getTripsPage(page: Int): Result<Pair<List<Trip>, Boolean>> =
            Result.success(Pair(emptyList(), false))
        override suspend fun getTrip(tripId: String): Result<Trip> = Result.failure(NotImplementedError())
        override suspend fun createTrip(title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> =
            Result.failure(NotImplementedError())
        override suspend fun updateTrip(tripId: String, title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> =
            Result.failure(NotImplementedError())
        override suspend fun deleteTrip(tripId: String): Result<Unit> = Result.success(Unit)
        override suspend fun exportItineraryPdf(tripId: String): Result<ByteArray> = pdfResult
        override suspend fun exportExpensesCsv(tripId: String): Result<ByteArray> = csvResult
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
