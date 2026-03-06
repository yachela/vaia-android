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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivitiesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sampleActivity = Activity(
        id = "a1",
        title = "Visita al Coliseo",
        description = "Tour guiado",
        date = "2026-06-01",
        time = "09:00",
        location = "Roma",
        cost = 18.0
    )

    private fun makeViewModel(
        activityRepo: ActivityRepository = FakeActivityRepository(),
        tripRepo: TripRepository = FakeTripRepository()
    ) = ActivitiesViewModel(activityRepo, tripRepo, "trip-1")

    // ── loadActivities ───────────────────────────────────────────────────────

    @Test
    fun `init loads activities on success`() = runTest {
        val repo = FakeActivityRepository(getResult = Result.success(listOf(sampleActivity)))
        val vm = makeViewModel(activityRepo = repo)
        advanceUntilIdle()

        assertEquals(listOf(sampleActivity), vm.activities.value)
        assertNull(vm.error.value)
    }

    @Test
    fun `loadActivities sets error on failure`() = runTest {
        val repo = FakeActivityRepository(getResult = Result.failure(RuntimeException("sin red")))
        val vm = makeViewModel(activityRepo = repo)
        advanceUntilIdle()

        assertEquals("sin red", vm.error.value)
        assertTrue(vm.activities.value.isEmpty())
    }

    // ── createActivity ───────────────────────────────────────────────────────

    @Test
    fun `createActivity transitions to Success and reloads list`() = runTest {
        val repo = FakeActivityRepository(createResult = Result.success(sampleActivity))
        val vm = makeViewModel(activityRepo = repo)
        advanceUntilIdle()

        vm.createActivity("Coliseo", "Tour", "2026-06-01", "09:00", "Roma", 18.0)
        advanceUntilIdle()

        assertTrue(vm.createState.value is ActivitiesViewModel.CreateState.Success)
        assertEquals(2, repo.getCalls) // init + reload after create
    }

    @Test
    fun `createActivity sets Error state on failure`() = runTest {
        val repo = FakeActivityRepository(createResult = Result.failure(RuntimeException("error crear")))
        val vm = makeViewModel(activityRepo = repo)
        advanceUntilIdle()

        vm.createActivity("X", "", "2026-06-01", "10:00", "Y", 0.0)
        advanceUntilIdle()

        val state = vm.createState.value
        assertTrue(state is ActivitiesViewModel.CreateState.Error)
        assertEquals("error crear", (state as ActivitiesViewModel.CreateState.Error).message)
    }

    // ── updateActivity ───────────────────────────────────────────────────────

    @Test
    fun `updateActivity transitions to Success`() = runTest {
        val repo = FakeActivityRepository(updateResult = Result.success(sampleActivity))
        val vm = makeViewModel(activityRepo = repo)
        advanceUntilIdle()

        vm.updateActivity("a1", "Coliseo", "Tour", "2026-06-01", "09:00", "Roma", 18.0)
        advanceUntilIdle()

        assertTrue(vm.updateState.value is ActivitiesViewModel.UpdateState.Success)
    }

    @Test
    fun `updateActivity sets Error state on failure`() = runTest {
        val repo = FakeActivityRepository(updateResult = Result.failure(RuntimeException("no existe")))
        val vm = makeViewModel(activityRepo = repo)
        advanceUntilIdle()

        vm.updateActivity("a1", "X", "", "2026-06-01", "09:00", "Y", 0.0)
        advanceUntilIdle()

        val state = vm.updateState.value
        assertTrue(state is ActivitiesViewModel.UpdateState.Error)
        assertEquals("no existe", (state as ActivitiesViewModel.UpdateState.Error).message)
    }

    // ── deleteActivity ───────────────────────────────────────────────────────

    @Test
    fun `deleteActivity transitions to Success`() = runTest {
        val repo = FakeActivityRepository()
        val vm = makeViewModel(activityRepo = repo)
        advanceUntilIdle()

        vm.deleteActivity("a1")
        advanceUntilIdle()

        assertTrue(vm.deleteState.value is ActivitiesViewModel.DeleteState.Success)
    }

    @Test
    fun `deleteActivity sets Error state on failure`() = runTest {
        val repo = FakeActivityRepository(deleteResult = Result.failure(RuntimeException("no borrar")))
        val vm = makeViewModel(activityRepo = repo)
        advanceUntilIdle()

        vm.deleteActivity("a1")
        advanceUntilIdle()

        val state = vm.deleteState.value
        assertTrue(state is ActivitiesViewModel.DeleteState.Error)
        assertEquals("no borrar", (state as ActivitiesViewModel.DeleteState.Error).message)
    }

    // ── exportItinerary ──────────────────────────────────────────────────────

    @Test
    fun `exportItinerary sets PdfReady with bytes on success`() = runTest {
        val pdfBytes = byteArrayOf(1, 2, 3)
        val tripRepo = FakeTripRepository(pdfResult = Result.success(pdfBytes))
        val vm = makeViewModel(tripRepo = tripRepo)
        advanceUntilIdle()

        vm.exportItinerary()
        advanceUntilIdle()

        val state = vm.exportState.value
        assertTrue(state is ActivitiesViewModel.ExportState.PdfReady)
        assertTrue((state as ActivitiesViewModel.ExportState.PdfReady).bytes.contentEquals(pdfBytes))
    }

    @Test
    fun `exportItinerary sets Error state on failure`() = runTest {
        val tripRepo = FakeTripRepository(pdfResult = Result.failure(RuntimeException("pdf error")))
        val vm = makeViewModel(tripRepo = tripRepo)
        advanceUntilIdle()

        vm.exportItinerary()
        advanceUntilIdle()

        val state = vm.exportState.value
        assertTrue(state is ActivitiesViewModel.ExportState.Error)
        assertEquals("pdf error", (state as ActivitiesViewModel.ExportState.Error).message)
    }

    // ── loadSuggestions ──────────────────────────────────────────────────────

    @Test
    fun `loadSuggestions sets Success with suggestions list`() = runTest {
        val suggestions = listOf(
            ActivitySuggestion("Coliseo", "Tour", "Roma", 18.0, "09:00"),
            ActivitySuggestion("Vaticano", "Arte", "Vaticano", 20.0, "11:00")
        )
        val repo = FakeActivityRepository(suggestionsResult = Result.success(suggestions))
        val vm = makeViewModel(activityRepo = repo)
        advanceUntilIdle()

        vm.loadSuggestions()
        advanceUntilIdle()

        val state = vm.suggestionsState.value
        assertTrue(state is ActivitiesViewModel.SuggestionsState.Success)
        assertEquals(2, (state as ActivitiesViewModel.SuggestionsState.Success).suggestions.size)
    }

    @Test
    fun `loadSuggestions sets Error state on failure`() = runTest {
        val repo = FakeActivityRepository(suggestionsResult = Result.failure(RuntimeException("IA no disponible")))
        val vm = makeViewModel(activityRepo = repo)
        advanceUntilIdle()

        vm.loadSuggestions()
        advanceUntilIdle()

        val state = vm.suggestionsState.value
        assertTrue(state is ActivitiesViewModel.SuggestionsState.Error)
        assertEquals("IA no disponible", (state as ActivitiesViewModel.SuggestionsState.Error).message)
    }

    // ── acceptSuggestion ─────────────────────────────────────────────────────

    @Test
    fun `acceptSuggestion creates activity with suggestion data`() = runTest {
        val suggestion = ActivitySuggestion("Cena", "Buena comida", "Trastevere", 30.0, "20:00")
        val repo = FakeActivityRepository(createResult = Result.success(sampleActivity))
        val vm = makeViewModel(activityRepo = repo)
        advanceUntilIdle()

        vm.acceptSuggestion(suggestion, "2026-06-02")
        advanceUntilIdle()

        assertTrue(vm.createState.value is ActivitiesViewModel.CreateState.Success)
        val call = repo.lastCreate!!
        assertEquals("Cena", call.title)
        assertEquals("Trastevere", call.location)
        assertEquals("2026-06-02", call.date)
        assertEquals(30.0, call.cost, 0.001)
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

    private data class CreateCall(
        val title: String,
        val location: String,
        val date: String,
        val cost: Double
    )

    private class FakeActivityRepository(
        private val getResult: Result<List<Activity>> = Result.success(emptyList()),
        private val createResult: Result<Activity> = Result.success(
            Activity("a1", "X", "", "2026-06-01", "09:00", "Y", 0.0)
        ),
        private val updateResult: Result<Activity> = Result.success(
            Activity("a1", "X", "", "2026-06-01", "09:00", "Y", 0.0)
        ),
        private val deleteResult: Result<Unit> = Result.success(Unit),
        private val suggestionsResult: Result<List<ActivitySuggestion>> = Result.success(emptyList())
    ) : ActivityRepository {

        var getCalls = 0
        var lastCreate: CreateCall? = null

        override suspend fun getActivities(tripId: String): Result<List<Activity>> {
            getCalls++
            return getResult
        }

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
            lastCreate = CreateCall(title, location, date, cost)
            return createResult
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
        ): Result<Activity> = updateResult

        override suspend fun deleteActivity(tripId: String, activityId: String): Result<Unit> = deleteResult

        override suspend fun getSuggestions(tripId: String): Result<List<ActivitySuggestion>> = suggestionsResult
    }

    private class FakeTripRepository(
        private val pdfResult: Result<ByteArray> = Result.success(ByteArray(0))
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
        override suspend fun exportExpensesCsv(tripId: String): Result<ByteArray> = Result.success(ByteArray(0))
    }
}
