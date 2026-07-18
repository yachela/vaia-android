package com.vaia.presentation.viewmodel

import com.vaia.domain.model.PackingItem
import com.vaia.domain.model.PackingList
import com.vaia.domain.model.PackingProgress
import com.vaia.domain.model.WeatherSuggestion
import com.vaia.domain.repository.PackingRepository
import com.vaia.presentation.ui.packing.PackingListUiState
import com.vaia.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PackingListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val samplePackingList = PackingList(
        id = "pl-1",
        tripId = "trip-1",
        itemsByCategory = emptyList(),
        progress = PackingProgress(0, 0, 0),
        createdAt = "2026-06-01T00:00:00Z",
        updatedAt = "2026-06-01T00:00:00Z"
    )

    @Test
    fun `loadPackingList emits Success without calling weather suggestions`() = runTest {
        val repo = FakePackingRepository(getListResult = Result.success(samplePackingList))
        val vm = PackingListViewModel(repo)

        vm.loadPackingList("trip-1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PackingListUiState.Success)
        assertEquals(samplePackingList, (state as PackingListUiState.Success).packingList)
        assertEquals(0, repo.weatherSuggestionsCalls)
    }

    @Test
    fun `loadPackingList emits Error on failure`() = runTest {
        val repo = FakePackingRepository(getListResult = Result.failure(RuntimeException("sin red")))
        val vm = PackingListViewModel(repo)

        vm.loadPackingList("trip-1")
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is PackingListUiState.Error)
        assertEquals("sin red", (state as PackingListUiState.Error).message)
    }

    private class FakePackingRepository(
        private val getListResult: Result<PackingList>
    ) : PackingRepository {

        var weatherSuggestionsCalls = 0

        override suspend fun getPackingList(tripId: String): Result<PackingList> = getListResult

        override suspend fun generatePackingList(tripId: String): Result<PackingList> = getListResult

        override suspend fun getWeatherSuggestions(tripId: String): Result<List<WeatherSuggestion>> {
            weatherSuggestionsCalls++
            return Result.success(emptyList())
        }

        override suspend fun addPackingItem(tripId: String, name: String, category: String): Result<PackingItem> =
            Result.failure(NotImplementedError())

        override suspend fun togglePackingItem(itemId: String): Result<PackingItem> =
            Result.failure(NotImplementedError())

        override suspend fun deletePackingItem(itemId: String): Result<Unit> = Result.success(Unit)
    }
}
