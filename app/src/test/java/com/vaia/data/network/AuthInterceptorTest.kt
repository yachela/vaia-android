package com.vaia.data.network

import com.vaia.data.auth.TokenProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenProvider: TokenProvider
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        tokenProvider = TokenProvider()
        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun execute() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        client.newCall(Request.Builder().url(server.url("/user")).build())
            .execute()
            .close()
    }

    @Test
    fun `agrega la cabecera Authorization cuando hay token`() {
        tokenProvider.token = "tok-123"

        execute()

        val recorded = server.takeRequest()
        assertEquals("Bearer tok-123", recorded.getHeader("Authorization"))
    }

    @Test
    fun `no agrega Authorization cuando no hay token`() {
        tokenProvider.token = null

        execute()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `no agrega Authorization cuando el token está en blanco`() {
        tokenProvider.token = "   "

        execute()

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun `agrega siempre la cabecera Accept application json`() {
        execute()

        assertEquals("application/json", server.takeRequest().getHeader("Accept"))
    }

    @Test
    fun `usa el token vigente en el momento de cada request`() {
        tokenProvider.token = "tok-viejo"
        execute()
        assertEquals("Bearer tok-viejo", server.takeRequest().getHeader("Authorization"))

        tokenProvider.token = "tok-nuevo"
        execute()
        assertEquals("Bearer tok-nuevo", server.takeRequest().getHeader("Authorization"))
    }
}
