package com.vaia.data.repository

import com.vaia.data.api.VaiaApiService
import com.vaia.data.auth.TokenProvider
import com.vaia.data.auth.TokenStorage
import com.vaia.domain.model.AppError
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AuthRepositoryImpl
    private lateinit var tokenStorage: FakeTokenStorage
    private lateinit var tokenProvider: TokenProvider

    private class FakeTokenStorage : TokenStorage {
        private var stored: String? = null
        override fun getToken(): String? = stored
        override fun saveToken(token: String) { stored = token }
        override fun clearToken() { stored = null }
    }

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val apiService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VaiaApiService::class.java)
        tokenStorage = FakeTokenStorage()
        tokenProvider = TokenProvider()
        repository = AuthRepositoryImpl(apiService, tokenStorage, tokenProvider)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    fun `login exitoso mapea los tokens y persiste el token`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "data": {
                        "user": {"id": "u1", "name": "Leo", "email": "leo@test.com"},
                        "access_token": "tok-123",
                        "token_type": "Bearer"
                    }
                }
                """.trimIndent()
            )
        )

        val result = repository.login("leo@test.com", "secret")

        assertTrue(result.isSuccess)
        val tokens = result.getOrThrow()
        assertEquals("tok-123", tokens.accessToken)
        assertEquals("Bearer", tokens.tokenType)
        assertEquals("tok-123", tokenStorage.getToken())
        assertEquals("tok-123", tokenProvider.token)
    }

    @Test
    fun `login con 422 devuelve Validation con el mensaje del backend`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(422).setBody(
                """
                {
                    "message": "Los datos son inválidos.",
                    "errors": {"email": ["El correo es obligatorio."]}
                }
                """.trimIndent()
            )
        )

        val result = repository.login("", "secret")

        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is AppError.Validation)
        assertEquals("El correo es obligatorio.", error?.message)
    }

    @Test
    fun `login con 401 devuelve Unauthorized`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"message": "Credenciales inválidas."}""")
        )

        val result = repository.login("leo@test.com", "wrong")

        assertTrue(result.exceptionOrNull() is AppError.Unauthorized)
        assertNull(tokenStorage.getToken())
    }

    @Test
    fun `login con 500 devuelve Unknown`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(500)
                .setBody("""{"message": "Error interno del servidor."}""")
        )

        val result = repository.login("leo@test.com", "secret")

        val error = result.exceptionOrNull()
        assertTrue(error is AppError.Unknown)
        assertEquals("Error interno del servidor.", error?.message)
    }

    @Test
    fun `login sin conexión devuelve Network`() = runTest {
        server.shutdown()

        val result = repository.login("leo@test.com", "secret")

        assertTrue(result.exceptionOrNull() is AppError.Network)
    }

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    fun `register exitoso mapea y persiste el token`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"data": {"access_token": "reg-tok", "token_type": "Bearer"}}"""
            )
        )

        val result = repository.register("Leo", "leo@test.com", "pass", "pass")

        assertTrue(result.isSuccess)
        assertEquals("reg-tok", result.getOrThrow().accessToken)
        assertEquals("reg-tok", tokenProvider.token)
    }

    // ── getCurrentUser ────────────────────────────────────────────────────────

    @Test
    fun `getCurrentUser mapea el usuario con claves snake_case`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "data": {
                        "id": "u1",
                        "name": "Leo",
                        "email": "leo@test.com",
                        "bio": "Viajero",
                        "avatar_url": "https://cdn.vaia.app/a.jpg"
                    }
                }
                """.trimIndent()
            )
        )

        val result = repository.getCurrentUser()

        val user = result.getOrThrow()
        assertEquals("u1", user.id)
        assertEquals("Viajero", user.bio)
        assertEquals("https://cdn.vaia.app/a.jpg", user.avatarUrl)
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    fun `logout limpia el token incluso si la API falla`() = runTest {
        tokenStorage.saveToken("tok-viejo")
        tokenProvider.token = "tok-viejo"
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        val result = repository.logout()

        assertTrue(result.isSuccess)
        assertNull(tokenStorage.getToken())
        assertNull(tokenProvider.token)
    }
}
