package com.vaia.presentation.viewmodel

import com.vaia.domain.model.AuthTokens
import com.vaia.domain.model.User
import com.vaia.domain.repository.AuthRepository
import com.vaia.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loadCurrentUser sets current user and idle state on success`() = runTest {
        val expectedUser = User(id = "u1", name = "Leo", email = "leo@test.com")
        val repo = FakeAuthRepository(userResult = Result.success(expectedUser))
        val viewModel = AuthViewModel(repo)

        viewModel.loadCurrentUser()
        advanceUntilIdle()

        assertEquals(expectedUser, viewModel.currentUser.value)
        assertTrue(viewModel.profileState.value is AuthViewModel.ProfileState.Idle)
    }

    @Test
    fun `loadCurrentUser sets error state on failure`() = runTest {
        val repo = FakeAuthRepository(userResult = Result.failure(RuntimeException("fallo perfil")))
        val viewModel = AuthViewModel(repo)

        viewModel.loadCurrentUser()
        advanceUntilIdle()

        val state = viewModel.profileState.value
        assertTrue(state is AuthViewModel.ProfileState.Error)
        assertEquals("fallo perfil", (state as AuthViewModel.ProfileState.Error).message)
    }

    private class FakeAuthRepository(
        private val userResult: Result<User>
    ) : AuthRepository {
        override suspend fun login(email: String, password: String): Result<AuthTokens> =
            Result.failure(NotImplementedError())

        override suspend fun register(
            name: String,
            email: String,
            password: String,
            passwordConfirmation: String
        ): Result<AuthTokens> = Result.failure(NotImplementedError())

        override suspend fun logout(): Result<Unit> = Result.success(Unit)

        override suspend fun getCurrentUser(): Result<User> = userResult

        override suspend fun updateProfile(
            name: String,
            bio: String?,
            country: String?,
            language: String?,
            currency: String?
        ): Result<User> = Result.failure(NotImplementedError())

        override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Result<User> =
            Result.failure(NotImplementedError())

        override fun isLoggedIn(): Boolean = true
        override fun getAccessToken(): String? = null
    }
}
