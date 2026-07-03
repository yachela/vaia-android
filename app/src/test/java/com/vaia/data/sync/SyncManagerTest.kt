package com.vaia.data.sync

import com.vaia.data.local.db.ActivityDao
import com.vaia.data.local.db.ActivityEntity
import com.vaia.data.local.db.PackingDao
import com.vaia.data.local.db.PackingItemEntity
import com.vaia.data.local.db.PackingListEntity
import com.vaia.data.network.ConnectivityObserver
import com.vaia.data.network.ConnectivityStatus
import com.vaia.domain.model.Activity
import com.vaia.domain.model.ActivitySuggestion
import com.vaia.domain.model.PackingItem
import com.vaia.domain.model.PackingList
import com.vaia.domain.model.WeatherSuggestion
import com.vaia.domain.repository.ActivityRepository
import com.vaia.domain.repository.PackingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SyncManagerTest {

    // ── Fakes ─────────────────────────────────────────────────────────────────

    private class FakeConnectivityObserver(
        var connected: Boolean = true
    ) : ConnectivityObserver {
        val statusFlow = MutableSharedFlow<ConnectivityStatus>()
        override fun observe(): Flow<ConnectivityStatus> = statusFlow
        override fun isConnected(): Boolean = connected
    }

    private class FakeActivityDao : ActivityDao {
        val activities = linkedMapOf<String, ActivityEntity>()
        override suspend fun getByTripId(tripId: String) =
            activities.values.filter { it.tripId == tripId }
        override suspend fun getById(id: String) = activities[id]
        override suspend fun insertAll(activities: List<ActivityEntity>) {
            activities.forEach { this.activities[it.id] = it }
        }
        override suspend fun insert(activity: ActivityEntity) {
            activities[activity.id] = activity
        }
        override suspend fun deleteById(id: String) { activities.remove(id) }
        override suspend fun deleteByTripId(tripId: String) {
            activities.values.removeAll { it.tripId == tripId }
        }
        override suspend fun getPendingSync() =
            activities.values.filter { it.syncStatus == "pending" }
        override suspend fun updateSyncStatus(id: String, status: String) {
            activities[id]?.let { activities[id] = it.copy(syncStatus = status) }
        }
    }

    private class FakePackingDao : PackingDao {
        val items = linkedMapOf<String, PackingItemEntity>()
        val lists = linkedMapOf<String, PackingListEntity>()
        override fun getPackingListByTripId(tripId: String) =
            flowOf(lists.values.firstOrNull { it.tripId == tripId })
        override suspend fun getPackingListByTripIdSync(tripId: String) =
            lists.values.firstOrNull { it.tripId == tripId }
        override suspend fun insertPackingList(packingList: PackingListEntity) {
            lists[packingList.id] = packingList
        }
        override suspend fun updatePackingList(packingList: PackingListEntity) {
            lists[packingList.id] = packingList
        }
        override suspend fun deletePackingListByTripId(tripId: String) {
            lists.values.removeAll { it.tripId == tripId }
        }
        override fun getPackingItemsByListId(packingListId: String) =
            flowOf(items.values.filter { it.packingListId == packingListId })
        override suspend fun getPackingItemsByListIdSync(packingListId: String) =
            items.values.filter { it.packingListId == packingListId }
        override suspend fun getPackingItemById(itemId: String) = items[itemId]
        override suspend fun insertPackingItem(item: PackingItemEntity) {
            items[item.id] = item
        }
        override suspend fun insertPackingItems(items: List<PackingItemEntity>) {
            items.forEach { this.items[it.id] = it }
        }
        override suspend fun updatePackingItem(item: PackingItemEntity) {
            items[item.id] = item
        }
        override suspend fun deletePackingItem(itemId: String) { items.remove(itemId) }
        override suspend fun deletePackingItemsByListId(packingListId: String) {
            items.values.removeAll { it.packingListId == packingListId }
        }
        override suspend fun getPendingPackingItems() =
            items.values.filter { it.syncStatus == "pending" }
        override suspend fun updatePackingItemSyncStatus(itemId: String, status: String) {
            items[itemId]?.let { items[itemId] = it.copy(syncStatus = status) }
        }
        override suspend fun getPendingPackingLists() =
            lists.values.filter { it.syncStatus == "pending" }
        override suspend fun updatePackingListSyncStatus(listId: String, status: String) {
            lists[listId]?.let { lists[listId] = it.copy(syncStatus = status) }
        }
    }

    private object StubActivityRepository : ActivityRepository {
        override suspend fun getActivities(tripId: String): Result<List<Activity>> =
            Result.failure(NotImplementedError())
        override suspend fun getActivity(tripId: String, activityId: String): Result<Activity> =
            Result.failure(NotImplementedError())
        override suspend fun createActivity(tripId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> =
            Result.failure(NotImplementedError())
        override suspend fun updateActivity(tripId: String, activityId: String, title: String, description: String, date: String, time: String, location: String, cost: Double): Result<Activity> =
            Result.failure(NotImplementedError())
        override suspend fun deleteActivity(tripId: String, activityId: String): Result<Unit> =
            Result.failure(NotImplementedError())
        override suspend fun getSuggestions(tripId: String): Result<List<ActivitySuggestion>> =
            Result.failure(NotImplementedError())
    }

    private object StubPackingRepository : PackingRepository {
        override suspend fun getPackingList(tripId: String): Result<PackingList> =
            Result.failure(NotImplementedError())
        override suspend fun generatePackingList(tripId: String): Result<PackingList> =
            Result.failure(NotImplementedError())
        override suspend fun getWeatherSuggestions(tripId: String): Result<List<WeatherSuggestion>> =
            Result.failure(NotImplementedError())
        override suspend fun addPackingItem(tripId: String, name: String, category: String): Result<PackingItem> =
            Result.failure(NotImplementedError())
        override suspend fun togglePackingItem(itemId: String): Result<PackingItem> =
            Result.failure(NotImplementedError())
        override suspend fun deletePackingItem(itemId: String): Result<Unit> =
            Result.failure(NotImplementedError())
    }

    private fun pendingActivity(id: String) = ActivityEntity(
        id = id,
        tripId = "t1",
        title = "Actividad $id",
        syncStatus = "pending"
    )

    private fun pendingPackingItem(id: String) = PackingItemEntity(
        id = id,
        packingListId = "pl1",
        name = "Item $id",
        category = "ROPA",
        isPacked = false,
        isSuggested = false,
        suggestionReason = null,
        syncStatus = "pending",
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `detecta operaciones pendientes al inicializar`() = runTest {
        val activityDao = FakeActivityDao().apply {
            activities["a1"] = pendingActivity("a1")
        }
        val packingDao = FakePackingDao().apply {
            items["p1"] = pendingPackingItem("p1")
        }

        val syncManager = SyncManager(
            FakeConnectivityObserver(),
            StubActivityRepository,
            StubPackingRepository,
            activityDao,
            packingDao,
            StandardTestDispatcher(testScheduler)
        )
        advanceUntilIdle()

        assertTrue(syncManager.hasPendingOperations.value)
        assertEquals(SyncState.PendingOperations(2), syncManager.syncState.value)
    }

    @Test
    fun `sin operaciones pendientes queda en Idle`() = runTest {
        val syncManager = SyncManager(
            FakeConnectivityObserver(),
            StubActivityRepository,
            StubPackingRepository,
            FakeActivityDao(),
            FakePackingDao(),
            StandardTestDispatcher(testScheduler)
        )
        advanceUntilIdle()

        assertFalse(syncManager.hasPendingOperations.value)
        assertEquals(SyncState.Idle, syncManager.syncState.value)
    }

    @Test
    fun `syncPendingChanges marca como sincronizadas las operaciones pendientes`() = runTest {
        val activityDao = FakeActivityDao().apply {
            activities["a1"] = pendingActivity("a1")
            activities["a2"] = pendingActivity("a2")
        }
        val packingDao = FakePackingDao().apply {
            items["p1"] = pendingPackingItem("p1")
        }
        val syncManager = SyncManager(
            FakeConnectivityObserver(connected = true),
            StubActivityRepository,
            StubPackingRepository,
            activityDao,
            packingDao,
            StandardTestDispatcher(testScheduler)
        )
        advanceUntilIdle()

        syncManager.syncPendingChanges()
        advanceUntilIdle()

        assertEquals("synced", activityDao.activities["a1"]?.syncStatus)
        assertEquals("synced", activityDao.activities["a2"]?.syncStatus)
        assertEquals("synced", packingDao.items["p1"]?.syncStatus)
        assertEquals(SyncState.Success("3 operaciones sincronizadas"), syncManager.syncState.value)
        assertFalse(syncManager.hasPendingOperations.value)
    }

    @Test
    fun `syncPendingChanges no hace nada sin conectividad`() = runTest {
        val activityDao = FakeActivityDao().apply {
            activities["a1"] = pendingActivity("a1")
        }
        val syncManager = SyncManager(
            FakeConnectivityObserver(connected = false),
            StubActivityRepository,
            StubPackingRepository,
            activityDao,
            FakePackingDao(),
            StandardTestDispatcher(testScheduler)
        )
        advanceUntilIdle()

        syncManager.syncPendingChanges()
        advanceUntilIdle()

        assertEquals("pending", activityDao.activities["a1"]?.syncStatus)
        assertTrue(syncManager.hasPendingOperations.value)
    }

    @Test
    fun `al recuperar conectividad se dispara la sincronización`() = runTest {
        val connectivity = FakeConnectivityObserver(connected = true)
        val activityDao = FakeActivityDao().apply {
            activities["a1"] = pendingActivity("a1")
        }
        val syncManager = SyncManager(
            connectivity,
            StubActivityRepository,
            StubPackingRepository,
            activityDao,
            FakePackingDao(),
            StandardTestDispatcher(testScheduler)
        )
        advanceUntilIdle()

        connectivity.statusFlow.emit(ConnectivityStatus.Available)
        advanceUntilIdle()

        assertEquals("synced", activityDao.activities["a1"]?.syncStatus)
        assertEquals(SyncState.Success("1 operaciones sincronizadas"), syncManager.syncState.value)
    }

    @Test
    fun `resetSyncState vuelve a Idle`() = runTest {
        val syncManager = SyncManager(
            FakeConnectivityObserver(),
            StubActivityRepository,
            StubPackingRepository,
            FakeActivityDao().apply { activities["a1"] = pendingActivity("a1") },
            FakePackingDao(),
            StandardTestDispatcher(testScheduler)
        )
        advanceUntilIdle()

        syncManager.resetSyncState()

        assertEquals(SyncState.Idle, syncManager.syncState.value)
    }
}
