package com.vaia.domain.usecase

import com.vaia.domain.repository.AuthRepository

class LoginUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String) =
        authRepository.login(email, password)
}

class RegisterUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(name: String, email: String, password: String, passwordConfirmation: String) =
        authRepository.register(name, email, password, passwordConfirmation)
}

class LogoutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.logout()
}

class IsLoggedInUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke() = authRepository.isLoggedIn()
}