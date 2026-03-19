# Offline Sync Strategy

## Overview

VAIA implements an automatic synchronization strategy to handle offline operations and ensure data consistency when connectivity is restored.

## Architecture

### Components

1. **ConnectivityObserver**: Monitors network connectivity status
2. **SyncManager**: Coordinates synchronization of pending operations
3. **Room Database**: Local storage with sync status tracking
4. **Repositories**: Handle data operations with offline support

### Sync Status States

Each entity in the local database has a `sync_status` field with the following possible values:

- `synced`: Entity is synchronized with backend
- `pending`: Entity has local changes waiting to be synchronized
- `error`: Synchronization failed, requires retry

## Synchronization Order

When connectivity is restored, operations are synchronized in the following order:

1. **Activities**: Offline activity changes (create, update, delete)
2. **Packing Items**: Packing list item changes (toggle, add, delete)
3. **Documents**: Pending document uploads (future implementation)

This order ensures that trip-related data is synchronized before dependent entities.

## Connectivity Detection

The `ConnectivityObserver` uses Android's `ConnectivityManager` to monitor network state:

```kotlin
sealed class ConnectivityStatus {
    data object Available : ConnectivityStatus()
    data object Unavailable : ConnectivityStatus()
    data object Losing : ConnectivityStatus()
    data object Lost : ConnectivityStatus()
}
```

### Automatic Sync Trigger

- Sync is automatically triggered when `ConnectivityStatus.Available` is detected
- Manual sync can be triggered via `SyncManager.syncPendingChanges()`

## Conflict Resolution

### Strategy: Last Write Wins

Currently, the system uses a "last write wins" strategy:

- Local changes overwrite server data during sync
- No conflict detection or merging is performed
- Future versions may implement more sophisticated conflict resolution

### Handling Sync Failures

When synchronization fails for an entity:

1. Entity `sync_status` is set to `error`
2. Error is logged for debugging
3. Entity remains in local database
4. User is notified with a non-intrusive message
5. Retry is attempted on next connectivity restoration

## User Experience

### Pending Operations Indicator

- `SyncManager.hasPendingOperations` Flow indicates if there are pending changes
- UI can display a visual indicator (e.g., badge, icon) when operations are pending
- Count of pending operations available via `SyncState.PendingOperations(count)`

### Sync State

The `SyncManager` exposes a `syncState` Flow with the following states:

```kotlin
sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
    data class PendingOperations(val count: Int) : SyncState()
}
```

### Non-Intrusive Notifications

- Success: Brief toast or snackbar with sync count
- Error: Snackbar with retry option
- No blocking dialogs or interruptions

## Implementation Details

### Database Schema

All entities that support offline sync must include:

```kotlin
@ColumnInfo(name = "sync_status")
val syncStatus: String = "synced" // "synced", "pending", "error"
```

### DAO Methods

Each DAO must implement:

```kotlin
@Query("SELECT * FROM table_name WHERE sync_status = 'pending'")
suspend fun getPendingSync(): List<Entity>

@Query("UPDATE table_name SET sync_status = :status WHERE id = :id")
suspend fun updateSyncStatus(id: String, status: String)
```

### Repository Pattern

Repositories should:

1. Perform local database operations immediately
2. Set `sync_status = "pending"` for offline operations
3. Attempt backend sync if connected
4. Update `sync_status` based on result

## Testing

### Manual Testing

1. Enable airplane mode
2. Perform operations (create activity, toggle packing item)
3. Verify operations are saved locally with `pending` status
4. Disable airplane mode
5. Verify automatic sync occurs
6. Verify `sync_status` updated to `synced`

### Edge Cases

- **Rapid connectivity changes**: Debouncing prevents multiple sync attempts
- **App restart with pending operations**: Sync triggered on next connectivity
- **Backend unavailable**: Operations remain pending, retry on next connectivity
- **Partial sync failure**: Failed entities marked as `error`, successful ones as `synced`

## Future Enhancements

1. **Conflict Detection**: Detect server-side changes during offline period
2. **Merge Strategies**: Allow user to choose between local/remote version
3. **Batch Sync**: Group operations for efficiency
4. **Priority Queue**: Sync critical operations first
5. **Exponential Backoff**: Retry failed syncs with increasing delays
6. **Sync History**: Log of sync operations for debugging

## Performance Considerations

- Sync operations run on background thread (Dispatchers.IO)
- No UI blocking during sync
- Minimal battery impact (triggered by system connectivity events)
- Database queries optimized with indexes on `sync_status` field

## Security

- All sync operations use authenticated API calls
- Token refresh handled automatically
- Failed auth results in redirect to login
- No sensitive data stored in sync logs
