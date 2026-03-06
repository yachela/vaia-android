package com.vaia.presentation.viewmodel

import com.vaia.domain.model.AuthTokens
import com.vaia.domain.model.User
import com.vaia.domain.repository.AuthRepository
import com.vaia.testutils.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    fun `login transitions to Success with tokens`() = runTest {
        val tokens = AuthTokens("tok-123")
        val repo = FakeAuthRepository(loginResult = Result.success(tokens))
        val viewModel = AuthViewModel(repo)

        viewModel.login("leo@test.com", "secret")
        advanceUntilIdle()

        val state = viewModel.loginState.value
        assertTrue(state is AuthViewModel.AuthState.Success)
        assertEquals(tokens, (state as AuthViewModel.AuthState.Success).tokens)
    }

    @Test
    fun `login sets Error state on failure`() = runTest {
        val repo = FakeAuthRepository(loginResult = Result.failure(RuntimeException("credenciales inválidas")))
        val viewModel = AuthViewModel(repo)

        viewModel.login("x@x.com", "wrong")
        advanceUntilIdle()

        val state = viewModel.loginState.value
        assertTrue(state is AuthViewModel.AuthState.Error)
        assertEquals("credenciales inválidas", (state as AuthViewModel.AuthState.Error).message)
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    fun `register transitions to Success with tokens`() = runTest {
        val tokens = AuthTokens("reg-tok")
        val repo = FakeAuthRepository(registerResult = Result.success(tokens))
        val viewModel = AuthViewModel(repo)

        viewModel.register("Leo", "leo@test.com", "pass", "pass")
        advanceUntilIdle()

        val state = viewModel.registerState.value
        assertTrue(state is AuthViewModel.AuthState.Success)
        assertEquals(tokens, (state as AuthViewModel.AuthState.Success).tokens)
    }

    @Test
    fun `register sets Error state on failure`() = runTest {
        val repo = FakeAuthRepository(registerResult = Result.failure(RuntimeException("email duplicado")))
        val viewModel = AuthViewModel(repo)

        viewModel.register("Leo", "dup@test.com", "pass", "pass")
        advanceUntilIdle()

        val state = viewModel.registerState.value
        assertTrue(state is AuthViewModel.AuthState.Error)
        assertEquals("email duplicado", (state as AuthViewModel.AuthState.Error).message)
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    fun `logout clears currentUser and resets states`() = runTest {
        val user = User(id = "u1", name = "Leo", email = "leo@test.com")
        val repo = FakeAuthRepository(userResult = Result.success(user))
        val viewModel = AuthViewModel(repo)
        viewModel.loadCurrentUser()
        advanceUntilIdle()
        assertEquals(user, viewModel.currentUser.value)

        viewModel.logout()
        advanceUntilIdle()

        assertNull(viewModel.currentUser.value)
        assertTrue(viewModel.loginState.value is AuthViewModel.AuthState.Idle)
        assertTrue(viewModel.registerState.value is AuthViewModel.AuthState.Idle)
        assertTrue(viewModel.profileState.value is AuthViewModel.ProfileState.Idle)
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    fun `updateProfile sets Saved state and updates currentUser on success`() = runTest {
        val updated = User(id = "u1", name = "Leo Updated", email = "leo@test.com", bio = "Viajero")
        val repo = FakeAuthRepository(updateProfileResult = Result.success(updated))
        val viewModel = AuthViewModel(repo)

        viewModel.updateProfile("Leo Updated", "Viajero", null, null, null)
        advanceUntilIdle()

        assertTrue(viewModel.profileState.value is AuthViewModel.ProfileState.Saved)
        assertEquals(updated, viewModel.currentUser.value)
    }

    @Test
    fun `updateProfile sets Error state on failure`() = runTest {
        val repo = FakeAuthRepository(updateProfileResult = Result.failure(RuntimeException("servidor no disponible")))
        val viewModel = AuthViewModel(repo)

        viewModel.updateProfile("X", null, null, null, null)
        advanceUntilIdle()

        val state = viewModel.profileState.value
        assertTrue(state is AuthViewModel.ProfileState.Error)
        assertEquals("servidor no disponible", (state as AuthViewModel.ProfileState.Error).message)
    }

    // ── uploadAvatar ──────────────────────────────────────────────────────────

    @Test
    fun `uploadAvatar sets Saved state and updates currentUser on success`() = runTest {
        val userWithAvatar = User(id = "u1", name = "Leo", email = "leo@test.com", avatarUrl = "https://cdn.vaia.app/avatar.jpg")
        val repo = FakeAuthRepository(uploadAvatarResult = Result.success(userWithAvatar))
        val viewModel = AuthViewModel(repo)

        viewModel.uploadAvatar(byteArrayOf(1, 2, 3), "image/jpeg")
        advanceUntilIdle()

        assertTrue(viewModel.profileState.value is AuthViewModel.ProfileState.Saved)
        assertEquals(userWithAvatar, viewModel.currentUser.value)
    }

    @Test
    fun `uploadAvatar sets Error state on failure`() = runTest {
        val repo = FakeAuthRepository(uploadAvatarResult = Result.failure(RuntimeException("archivo muy grande")))
        val viewModel = AuthViewModel(repo)

        viewModel.uploadAvatar(byteArrayOf(9), "image/png")
        advanceUntilIdle()

        val state = viewModel.profileState.value
        assertTrue(state is AuthViewModel.ProfileState.Error)
        assertEquals("archivo muy grande", (state as AuthViewModel.ProfileState.Error).message)
    }

    private class FakeAuthRepository(
        private val loginResult: Result<AuthTokens> = Result.failure(NotImplementedError()),
        private val registerResult: Result<AuthTokens> = Result.failure(NotImplementedError()),
        private val userResult: Result<User> = Result.failure(NotImplementedError()),
        private val updateProfileResult: Result<User> = Result.failure(NotImplementedError()),
        private val uploadAvatarResult: Result<User> = Result.failure(NotImplementedError())
    ) : AuthRepository {
        override suspend fun login(email: String, password: String): Result<AuthTokens> = loginResult

        override suspend fun register(
            name: String,
            email: String,
            password: String,
            passwordConfirmation: String
        ): Result<AuthTokens> = registerResult

        override suspend fun logout(): Result<Unit> = Result.success(Unit)

        override suspend fun getCurrentUser(): Result<User> = userResult

        override suspend fun updateProfile(
            name: String,
            bio: String?,
            country: String?,
            language: String?,
            currency: String?
        ): Result<User> = updateProfileResult

        override suspend fun uploadAvatar(imageBytes: ByteArray, mimeType: String): Result<User> = uploadAvatarResult

        override fun isLoggedIn(): Boolean = true
        override fun getAccessToken(): String? = null
    }
}
