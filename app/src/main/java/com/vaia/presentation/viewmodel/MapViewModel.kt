package com.vaia.presentation.viewmodel

import android.app.Application
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.vaia.domain.model.Activity
import com.vaia.domain.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    application: Application,
    private val activityRepository: ActivityRepository
) : AndroidViewModel(application) {

    private val _activities = MutableStateFlow<List<Activity>>(emptyList())
    val activities: StateFlow<List<Activity>> = _activities

    private val _geocodedLocations = MutableStateFlow<Map<String, LatLng>>(emptyMap())
    val geocodedLocations: StateFlow<Map<String, LatLng>> = _geocodedLocations

    private val _destinationLocation = MutableStateFlow<LatLng?>(null)
    val destinationLocation: StateFlow<LatLng?> = _destinationLocation

    private val _accommodationLocation = MutableStateFlow<LatLng?>(null)
    val accommodationLocation: StateFlow<LatLng?> = _accommodationLocation

    private val _selectedActivityId = MutableStateFlow<String?>(null)
    val selectedActivityId: StateFlow<String?> = _selectedActivityId

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentTripId: String? = null

    fun loadActivities(tripId: String, tripDestination: String) {
        if (tripId == currentTripId) return
        currentTripId = tripId
        viewModelScope.launch {
            _isLoading.value = true
            _activities.value = emptyList()
            _geocodedLocations.value = emptyMap()
            _destinationLocation.value = null
            _accommodationLocation.value = null
            _selectedActivityId.value = null

            // Geocode trip destination first
            geocodeDestination(tripDestination)

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

    private suspend fun geocodeDestination(destination: String) {
        val geocoder = Geocoder(getApplication())
        if (!Geocoder.isPresent() || destination.isBlank()) return

        withContext(Dispatchers.IO) {
            runCatching {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(destination, 1)
                addresses?.firstOrNull()?.let { addr ->
                    _destinationLocation.value = LatLng(addr.latitude, addr.longitude)
                }
            }
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
                        val latLng = LatLng(addr.latitude, addr.longitude)
                        result[activity.id] = latLng
                        
                        // Identify accommodation
                        val titleLower = activity.title.lowercase()
                        if (titleLower.contains("alojamiento") || 
                            titleLower.contains("hotel") || 
                            titleLower.contains("hostel") || 
                            titleLower.contains("hospedaje") ||
                            titleLower.contains("stay") ||
                            titleLower.contains("airbnb")) {
                            _accommodationLocation.value = latLng
                        }
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
