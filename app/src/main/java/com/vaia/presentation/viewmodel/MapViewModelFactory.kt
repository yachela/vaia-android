package com.vaia.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vaia.domain.repository.ActivityRepository

class MapViewModelFactory(
    private val application: Application,
    private val activityRepository: ActivityRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(application, activityRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
