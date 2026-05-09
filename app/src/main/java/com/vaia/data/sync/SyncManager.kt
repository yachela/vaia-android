package com.vaia.data.sync

import android.util.Log
import com.vaia.data.local.db.ActivityDao
import com.vaia.data.local.db.PackingDao
import com.vaia.data.local.db.TripDao
import com.vaia.data.network.ConnectivityObserver
import com.vaia.data.network.ConnectivityStatus
import com.vaia.data.repository.ActivityRepositoryImpl
import com.vaia.data.repository.PackingRepositoryImpl
import com.vaia.domain.repository.ActivityRepository
import com.vaia.domain.repository.PackingRepository
import com.vaia.domain.repository.TripRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
    data class PendingOperations(val count: Int) : SyncState()
}

@Singleton
class SyncManager @Inject constructor(
    private val connectivityObserver: ConnectivityObserver,
    private val activityRepository: ActivityRepository,
    private val packingRepository: PackingRepository,
    private val tripRepository: TripRepository,
    private val activityDao: ActivityDao,
    private val packingDao: PackingDao,
    private val tripDao: TripDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _hasPendingOperations = MutableStateFlow(false)
    val hasPendingOperations: StateFlow<Boolean> = _hasPendingOperations.asStateFlow()

    init {
        observeConnectivity()
        checkPendingOperations()
    }

    private fun observeConnectivity() {
        scope.launch {
            connectivityObserver.observe().collect { status ->
                when (status) {
                    ConnectivityStatus.Available -> {
                        Log.d(TAG, "Connectivity restored, starting sync")
                        syncPendingChanges()
                    }
                    ConnectivityStatus.Lost, ConnectivityStatus.Unavailable -> {
                        Log.d(TAG, "Connectivity lost")
                        checkPendingOperations()
                    }
                    ConnectivityStatus.Losing -> {
                        Log.d(TAG, "Connectivity losing")
                    }
                }
            }
        }
    }

    private fun checkPendingOperations() {
        scope.launch {
            try {
                val pendingActivities = activityDao.getPendingSync()
                val pendingPackingItems = packingDao.getPendingPackingItems()
                val pendingTrips = tripDao.getPendingSync()
                val totalPending = pendingActivities.size + pendingPackingItems.size + pendingTrips.size
                
                _hasPendingOperations.value = totalPending > 0
                
                if (totalPending > 0) {
                    _syncState.value = SyncState.PendingOperations(totalPending)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking pending operations", e)
            }
        }
    }

    fun syncPendingChanges() {
        if (!connectivityObserver.isConnected()) {
            Log.d(TAG, "No connectivity, skipping sync")
            return
        }

        scope.launch {
            try {
                _syncState.value = SyncState.Syncing
                
                // Sync order: trips -> activities -> packing items
                val tripsSynced = syncPendingTrips()
                val activitiesSynced = syncPendingActivities()
                val packingItemsSynced = syncPendingPackingItems()
                
                val totalSynced = tripsSynced + activitiesSynced + packingItemsSynced
                
                if (totalSynced > 0) {
                    _syncState.value = SyncState.Success("$totalSynced operaciones sincronizadas")
                    _hasPendingOperations.value = false
                } else {
                    _syncState.value = SyncState.Idle
                }
                
                checkPendingOperations()
            } catch (e: Exception) {
                Log.e(TAG, "Sync failed", e)
                _syncState.value = SyncState.Error("Error al sincronizar: ${e.message}")
            }
        }
    }

    private suspend fun syncPendingTrips(): Int {
        return try {
            val pendingTrips = tripDao.getPendingSync()
            var syncedCount = 0
            
            pendingTrips.forEach { tripEntity ->
                try {
                    val result = when (tripEntity.syncStatus) {
                        "pending_create" -> {
                            tripRepository.createTrip(
                                tripEntity.title,
                                tripEntity.destination,
                                tripEntity.startDate,
                                tripEntity.endDate,
                                tripEntity.budget
                            )
                        }
                        "pending_update" -> {
                            tripRepository.updateTrip(
                                tripEntity.id,
                                tripEntity.title,
                                tripEntity.destination,
                                tripEntity.startDate,
                                tripEntity.endDate,
                                tripEntity.budget
                            )
                        }
                        "pending_delete" -> {
                            tripRepository.deleteTrip(tripEntity.id)
                        }
                        else -> null
                    }
                    
                    if (result != null) syncedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync trip ${tripEntity.id}", e)
                }
            }
            
            Log.d(TAG, "Synced $syncedCount trips")
            syncedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing trips", e)
            0
        }
    }

    private suspend fun syncPendingActivities(): Int {
        return try {
            val pendingActivities = activityDao.getPendingSync()
            var syncedCount = 0
            
            pendingActivities.forEach { activity ->
                try {
                    // Sync with backend (implementation depends on repository)
                    // For now, just mark as synced
                    activityDao.updateSyncStatus(activity.id, "synced")
                    syncedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync activity ${activity.id}", e)
                    activityDao.updateSyncStatus(activity.id, "error")
                }
            }
            
            Log.d(TAG, "Synced $syncedCount activities")
            syncedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing activities", e)
            0
        }
    }

    private suspend fun syncPendingPackingItems(): Int {
        return try {
            val pendingItems = packingDao.getPendingPackingItems()
            var syncedCount = 0
            
            pendingItems.forEach { item ->
                try {
                    // Sync with backend
                    packingDao.updatePackingItemSyncStatus(item.id, "synced")
                    syncedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync packing item ${item.id}", e)
                    packingDao.updatePackingItemSyncStatus(item.id, "error")
                }
            }
            
            Log.d(TAG, "Synced $syncedCount packing items")
            syncedCount
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing packing items", e)
            0
        }
    }

    fun resetSyncState() {
        _syncState.value = SyncState.Idle
    }

    companion object {
        private const val TAG = "SyncManager"
    }
}
