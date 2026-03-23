package com.vaia.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vaia.domain.model.AuthTokens
import com.vaia.domain.model.User
import com.vaia.domain.repository.AuthRepository
import com.vaia.fcm.FcmTokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val fcmTokenManager: FcmTokenManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState

    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> = _registerState

    private val _logoutState = MutableStateFlow<AuthState>(AuthState.Idle)
    val logoutState: StateFlow<AuthState> = _logoutState

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState

    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    fun setOnboardingShown() {
        _showOnboarding.value = false
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthState.Loading
            val result = authRepository.login(email, password)
            _loginState.value = result.fold(
                onSuccess = { 
                    // Enviar token FCM al backend después de login exitoso
                    sendFcmTokenToBackend()
                    AuthState.Success(it) 
                },
                onFailure = { AuthState.Error(it.message ?: "Login failed") }
            )
        }
    }

    fun register(name: String, email: String, password: String, passwordConfirmation: String) {
        viewModelScope.launch {
            _registerState.value = AuthState.Loading
            val result = authRepository.register(name, email, password, passwordConfirmation)
            _registerState.value = result.fold(
                onSuccess = {
                    _showOnboarding.value = true
                    AuthState.Success(it)
                },
                onFailure = { AuthState.Error(it.message ?: "Registration failed") }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _logoutState.value = AuthState.Loading
            
            // Eliminar token FCM del backend antes de cerrar sesión
            try {
                fcmTokenManager.deleteTokenFromBackend()
            } catch (e: Exception) {
                // Log error but continue with logout
            }
            
            authRepository.logout()
            _logoutState.value = AuthState.Idle
            _loginState.value = AuthState.Idle
            _registerState.value = AuthState.Idle
            _currentUser.value = null
            _profileState.value = ProfileState.Idle
        }
    }

    private fun sendFcmTokenToBackend() {
        viewModelScope.launch {
            try {
                val token = fcmTokenManager.getCurrentToken()
                if (token != null) {
                    fcmTokenManager.sendTokenToBackend(token)
                }
            } catch (e: Exception) {
                // Log error but don't fail login
            }
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            val result = authRepository.getCurrentUser()
            result.fold(
                onSuccess = {
                    _currentUser.value = it
                    _profileState.value = ProfileState.Idle
                },
                onFailure = {
                    _profileState.value = ProfileState.Error(it.message ?: "No se pudo cargar el perfil")
                }
            )
        }
    }

    fun updateProfile(
        name: String,
        bio: String?,
        country: String?,
        language: String?,
        currency: String?
    ) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Saving
            val result = authRepository.updateProfile(
                name = name,
                bio = bio,
                country = country,
                language = language,
                currency = currency
            )
            result.fold(
                onSuccess = {
                    _currentUser.value = it
                    _profileState.value = ProfileState.Saved
                },
                onFailure = {
                    _profileState.value = ProfileState.Error(it.message ?: "No se pudo actualizar el perfil")
                }
            )
        }
    }

    fun uploadAvatar(imageBytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Saving
            authRepository.uploadAvatar(imageBytes, mimeType).fold(
                onSuccess = {
                    _currentUser.value = it
                    _profileState.value = ProfileState.Saved
                },
                onFailure = {
                    _profileState.value = ProfileState.Error(it.message ?: "No se pudo subir el avatar")
                }
            )
        }
    }

    fun resetProfileState() {
        _profileState.value = ProfileState.Idle
    }

    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val tokens: AuthTokens) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    sealed class ProfileState {
        object Idle : ProfileState()
        object Loading : ProfileState()
        object Saving : ProfileState()
        object Saved : ProfileState()
        data class Error(val message: String) : ProfileState()
    }
}
