package com.vaia.presentation.viewmodel

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.vaia.domain.model.Activity
import com.vaia.domain.repository.ActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapViewModel(
    application: Application,
    private val activityRepository: ActivityRepository
) : AndroidViewModel(application) {

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities

    private val _geocodedLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val geocodedLocations: StateFlow<Map<String, LatLng>> = _geocodedLocations

    private val _selectedActivityId = MutableStateFlow<String?>(null)
    val selectedActivityId: StateFlow<String?> = _selectedActivityId

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentTripId: String? = null

    fun loadActivities(tripId: String) {
        if (tripId == currentTripId) return
        currentTripId = tripId
        viewModelScope.launch {
            _isLoading.value = true
            _activities.value = emptyList()
            _geocodedLocations.value = emptyMap()
            _selectedActivityId.value = null

            activityRepository.getActivities(tripId).fold(
                onSuccess = { activities ->
                    _activities.value = activities
                    geocodeActivities(activities)
                    _isLoading.value = false
                },
                onFailure = {
                    _isLoading.value = false
                }
            )
        }
    }

    private suspend fun geocodeActivities(activities: List<Activity>) {
        val geocoder = Geocoder(getApplication())
        if (!Geocoder.isPresent()) return

        withContext(Dispatchers.IO) {
            val result = mutableMapOf<String, LatLng>()
            activities.forEach { activity ->
                if (activity.location.isBlank()) return@forEach
                runCatching {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(activity.location, 1)
                    addresses?.firstOrNull()?.let { addr ->
                        result[activity.id] = LatLng(addr.latitude, addr.longitude)
                    }
                }
                // Emit incrementally so the map shows markers as they resolve
                if (result.isNotEmpty()) {
                    _geocodedLocations.value = result.toMap()
                }
            }
        }
    }

    fun selectActivity(activityId: String?) {
        _selectedActivityId.value = activityId
    }
}
