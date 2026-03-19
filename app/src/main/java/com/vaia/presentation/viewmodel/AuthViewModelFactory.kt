package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vaia.domain.repository.AuthRepository
import com.vaia.fcm.FcmTokenManager

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val fcmTokenManager: FcmTokenManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository, fcmTokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}