package com.vaia.data.auth

import com.vaia.data.api.VaiaApiService
import com.vaia.data.network.AuthInterceptor
import com.vaia.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Tests de integración del flujo completo del token:
 * login → TokenProvider/TokenStorage actualizados → requests autenticados →
 * logout → token limpiado y requests sin Authorization.
 */
class TokenFlowTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenProvider: TokenProvider
    private lateinit var tokenStorage: FakeTokenStorage
    private lateinit var repository: AuthRepositoryImpl

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
        tokenProvider = TokenProvider()
        tokenStorage = FakeTokenStorage()

        // El mismo cliente que usa la app: AuthInterceptor leyendo TokenProvider.
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()
        val apiService = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VaiaApiService::class.java)
        repository = AuthRepositoryImpl(apiService, tokenStorage, tokenProvider)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueLoginSuccess(token: String) {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "data": {
                        "user": {"id": "u1", "name": "Leo", "email": "leo@test.com"},
                        "access_token": "$token",
                        "token_type": "Bearer"
                    }
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun `el login actualiza TokenProvider y TokenStorage`() = runTest {
        enqueueLoginSuccess("tok-nuevo")

        repository.login("leo@test.com", "secret")

        assertEquals("tok-nuevo", tokenProvider.token)
        assertEquals("tok-nuevo", tokenStorage.getToken())
        assertTrue(repository.isLoggedIn())
    }

    @Test
    fun `el request de login no lleva Authorization y los siguientes sí`() = runTest {
        enqueueLoginSuccess("tok-123")
        repository.login("leo@test.com", "secret")
        val loginRequest = server.takeRequest()
        assertNull(loginRequest.getHeader("Authorization"))

        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"data": {"id": "u1", "name": "Leo", "email": "leo@test.com"}}""")
        )
        repository.getCurrentUser()

        val userRequest = server.takeRequest()
        assertEquals("Bearer tok-123", userRequest.getHeader("Authorization"))
    }

    @Test
    fun `el login fallido no modifica el token`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"message": "Credenciales inválidas."}""")
        )

        repository.login("leo@test.com", "wrong")

        assertNull(tokenProvider.token)
        assertNull(tokenStorage.getToken())
        assertFalse(repository.isLoggedIn())
    }

    @Test
    fun `el logout limpia TokenProvider y TokenStorage`() = runTest {
        enqueueLoginSuccess("tok-123")
        repository.login("leo@test.com", "secret")
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        repository.logout()

        assertNull(tokenProvider.token)
        assertNull(tokenStorage.getToken())
        assertFalse(repository.isLoggedIn())
    }

    @Test
    fun `tras el logout los requests salen sin Authorization`() = runTest {
        enqueueLoginSuccess("tok-123")
        repository.login("leo@test.com", "secret")
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        repository.logout()
        server.takeRequest() // login
        server.takeRequest() // logout

        server.enqueue(
            MockResponse().setResponseCode(401)
                .setBody("""{"message": "Unauthenticated."}""")
        )
        repository.getCurrentUser()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `el logout limpia el token aunque la API falle`() = runTest {
        enqueueLoginSuccess("tok-123")
        repository.login("leo@test.com", "secret")
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        val result = repository.logout()

        assertTrue(result.isSuccess)
        assertNull(tokenProvider.token)
        assertNull(tokenStorage.getToken())
    }

    @Test
    fun `getAccessToken expone el token en memoria`() = runTest {
        assertNull(repository.getAccessToken())
        enqueueLoginSuccess("tok-abc")

        repository.login("leo@test.com", "secret")

        assertEquals("tok-abc", repository.getAccessToken())
    }
}
