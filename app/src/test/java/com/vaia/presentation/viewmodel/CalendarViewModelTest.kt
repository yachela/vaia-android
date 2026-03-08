package com.vaia.presentation.viewmodel

import com.vaia.domain.model.Activity
import com.vaia.domain.model.ActivitySuggestion
import com.vaia.domain.model.Trip
import com.vaia.domain.repository.ActivityRepository
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
class CalendarViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val trip1 = Trip(
        id = "t1", title = "Roma", destination = "Roma",
        startDate = "2026-06-01", endDate = "2026-06-10", budget = 1000.0
    )
    private val trip2 = Trip(
        id = "t2", title = "París", destination = "París",
        startDate = "2026-08-01", endDate = "2026-08-10", budget = 2000.0
    )
    private val activity1 = Activity("a1", "Coliseo", "Tour", "2026-06-02", "09:00", "Roma", 18.0)
    private val activity2 = Activity("a2", "Louvre", "Museo", "2026-08-03", "10:00", "París", 20.0)

    private fun makeViewModel(
        tripRepo: TripRepository = FakeTripRepository(),
        activityRepo: ActivityRepository = FakeActivityRepository()
    ) = CalendarViewModel(tripRepo, activityRepo)

    // ── init / load ───────────────────────────────────────────────────────────

    @Test
    fun `init transitions from Loading to Ready`() = runTest {
        val vm = makeViewModel()
        // Before idle, state is Loading
        assertTrue(vm.state.value is CalendarViewModel.State.Loading)

        advanceUntilIdle()
        assertTrue(vm.state.value is CalendarViewModel.State.Ready)
    }

    @Test
    fun `load with no trips produces empty Ready`() = runTest {
        val vm = makeViewModel(tripRepo = FakeTripRepository(trips = emptyList()))
        advanceUntilIdle()

        val state = vm.state.value as CalendarViewModel.State.Ready
        assertTrue(state.activities.isEmpty())
    }

    @Test
    fun `load collects activities from all trips`() = runTest {
        val activityRepo = FakeActivityRepository(
            perTrip = mapOf("t1" to listOf(activity1), "t2" to listOf(activity2))
        )
        val vm = makeViewModel(
            tripRepo = FakeTripRepository(trips = listOf(trip1, trip2)),
            activityRepo = activityRepo
        )
        advanceUntilIdle()

        val state = vm.state.value as CalendarViewModel.State.Ready
        assertEquals(2, state.activities.size)
        assertTrue(state.activities.any { it.activity.id == "a1" && it.trip.id == "t1" })
        assertTrue(state.activities.any { it.activity.id == "a2" && it.trip.id == "t2" })
    }

    @Test
    fun `load ignores activity fetch failures and still returns Ready`() = runTest {
        val activityRepo = FakeActivityRepository(
            perTrip = mapOf("t1" to listOf(activity1)),
            failFor = setOf("t2")
        )
        val vm = makeViewModel(
            tripRepo = FakeTripRepository(trips = listOf(trip1, trip2)),
            activityRepo = activityRepo
        )
        advanceUntilIdle()

        // CalendarViewModel uses getOrDefault(emptyList()) — failures are silently skipped
        val state = vm.state.value as CalendarViewModel.State.Ready
        assertEquals(1, state.activities.size)
        assertEquals("a1", state.activities.first().activity.id)
    }

    @Test
    fun `load sets Error state when trips fetch fails`() = runTest {
        val vm = makeViewModel(
            tripRepo = FakeTripRepository(tripsError = RuntimeException("sin red"))
        )
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is CalendarViewModel.State.Error)
        assertEquals("sin red", (state as CalendarViewModel.State.Error).message)
    }

    @Test
    fun `load can be called again to refresh`() = runTest {
        val activityRepo = FakeActivityRepository(
            perTrip = mapOf("t1" to listOf(activity1))
        )
        val vm = makeViewModel(
            tripRepo = FakeTripRepository(trips = listOf(trip1)),
            activityRepo = activityRepo
        )
        advanceUntilIdle()
        assertEquals(1, (vm.state.value as CalendarViewModel.State.Ready).activities.size)

        vm.load()
        advanceUntilIdle()
        assertEquals(1, (vm.state.value as CalendarViewModel.State.Ready).activities.size)
    }

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeTripRepository(
        private val trips: List<Trip> = emptyList(),
        private val tripsError: Throwable? = null
    ) : TripRepository {
        override suspend fun getTrips(): Result<List<Trip>> =
            if (tripsError != null) Result.failure(tripsError)
            else Result.success(trips)

        override suspend fun getTripsPage(page: Int): Result<Pair<List<Trip>, Boolean>> =
            Result.success(Pair(emptyList(), false))
        override suspend fun getTrip(tripId: String): Result<Trip> = Result.failure(NotImplementedError())
        override suspend fun createTrip(title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> = Result.failure(NotImplementedError())
        override suspend fun updateTrip(tripId: String, title: String, destination: String, startDate: String, endDate: String, budget: Double): Result<Trip> = Result.failure(NotImplementedError())
        override suspend fun deleteTrip(tripId: String): Result<Unit> = Result.success(Unit)
        override suspend fun exportItineraryPdf(tripId: String): Result<ByteArray> = Result.success(ByteArray(0))
        override suspend fun exportExpensesCsv(tripId: String): Result<ByteArray> = Result.success(ByteArray(0))
    }

    private class FakeActivityRepository(
        private val perTrip: Map<String, List<Activity>> = emptyMap(),
        private val failFor: Set<String> = emptySet()
    ) : ActivityRepository {
        override suspend fun getActivities(tripId: String): Result<List<Activity>> =
            if (tripId in failFor) Result.failure(RuntimeException("fallo para $tripId"))
            else Result.success(perTrip[tripId] ?: emptyList())

        override suspend fun getActivity(tripId: String, activityId: String): Result<Activity> = Result.failure(NotImplementedError())
        override suspend fun createActivity(tripId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> = Result.failure(NotImplementedError())
        override suspend fun updateActivity(tripId: String, activityId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> = Result.failure(NotImplementedError())
        override suspend fun deleteActivity(tripId: String, activityId: String): Result<Unit> = Result.failure(NotImplementedError())
        override suspend fun getSuggestions(tripId: String): Result<List<ActivitySuggestion>> = Result.success(emptyList())
    }
}
