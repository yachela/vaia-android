package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.data.network.ConnectivityObserver
import com.vaia.data.network.ConnectivityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Estado de conexión para toda la app. Alimenta el banner de modo offline.
 */
@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    val isOffline: StateFlow<Boolean> = connectivityObserver.observe()
        .map { status ->
            when (status) {
                is ConnectivityStatus.Available, is ConnectivityStatus.Losing -> false
                is ConnectivityStatus.Lost, is ConnectivityStatus.Unavailable -> true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = !connectivityObserver.isConnected()
        )
}
