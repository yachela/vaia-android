package com.vaia.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.vaia.domain.model.Activity
import com.vaia.domain.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    application: Application,
    private val activityRepository: ActivityRepository
) : AndroidViewModel(application) {

    private val placesClient: PlacesClient = Places.createClient(application)

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

    // New states for recommendations
    private val _searchPointerLocation = MutableStateFlow<LatLng?>(null)
    val searchPointerLocation: StateFlow<LatLng?> = _searchPointerLocation

    private val _recommendations = MutableStateFlow<List<Place>>(emptyList())
    val recommendations: StateFlow<List<Place>> = _recommendations

    private val _selectedPlace = MutableStateFlow<Place?>(null)
    val selectedPlace: StateFlow<Place?> = _selectedPlace

    private val _selectedPlacePhoto = MutableStateFlow<Bitmap?>(null)
    val selectedPlacePhoto: StateFlow<Bitmap?> = _selectedPlacePhoto

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
        if (activityId != null) _selectedPlace.value = null
    }

    fun updateSearchPointer(latLng: LatLng) {
        _searchPointerLocation.value = latLng
    }

    fun searchNearby(includedTypes: List<String>) {
        val location = _searchPointerLocation.value ?: _accommodationLocation.value ?: _destinationLocation.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Using CircularBounds for the 2km radius requirement
                val locationRestriction = CircularBounds.newInstance(location, 2000.0)
                // Applying Field Masking: ID, NAME (as DISPLAY_NAME), LAT_LNG (as LOCATION)
                val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
                
                val request = SearchNearbyRequest.builder(locationRestriction, placeFields)
                    .setIncludedTypes(includedTypes)
                    .setMaxResultCount(20)
                    .build()

                val response = placesClient.searchNearby(request).await()
                _recommendations.value = response.places
            } catch (e: Exception) {
                _recommendations.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchPlaceDetails(placeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedPlacePhoto.value = null
            try {
                // Detailed fields including requested ones: PHOTOS, SUMMARY, OPENING HOURS, PHONE
                val placeFields = listOf(
                    Place.Field.ID, 
                    Place.Field.NAME, 
                    Place.Field.LAT_LNG, 
                    Place.Field.RATING, 
                    Place.Field.ADDRESS,
                    Place.Field.PHOTO_METADATAS,
                    Place.Field.EDITORIAL_SUMMARY,
                    Place.Field.CURRENT_OPENING_HOURS,
                    Place.Field.PHONE_NUMBER,
                    Place.Field.UTC_OFFSET
                )
                val request = FetchPlaceRequest.builder(placeId, placeFields).build()
                val response = placesClient.fetchPlace(request).await()
                val place = response.place
                _selectedPlace.value = place
                _selectedActivityId.value = null

                // Fetch the first photo if available
                place.photoMetadatas?.firstOrNull()?.let { photoMetadata ->
                    val photoRequest = FetchPhotoRequest.builder(photoMetadata)
                        .setMaxWidth(1000)
                        .setMaxHeight(600)
                        .build()
                    val photoResponse = placesClient.fetchPhoto(photoRequest).await()
                    _selectedPlacePhoto.value = photoResponse.bitmap
                }
            } catch (e: Exception) {
                _selectedPlace.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearSelectedPlace() {
        _selectedPlace.value = null
        _selectedPlacePhoto.value = null
    }

    fun clearRecommendations() {
        _recommendations.value = emptyList()
    }
}
