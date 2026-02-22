package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vaia.domain.repository.AuthRepository
import com.vaia.domain.repository.TripRepository

class TripsViewModelFactory(
    private val tripRepository: TripRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TripsViewModel::class.java)) {
            return TripsViewModel(tripRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}