package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vaia.domain.repository.ActivityRepository
import com.vaia.domain.repository.TripRepository

class ActivitiesViewModelFactory(
    private val activityRepository: ActivityRepository,
    private val tripRepository: TripRepository,
    private val tripId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivitiesViewModel::class.java)) {
            return ActivitiesViewModel(activityRepository, tripRepository, tripId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}