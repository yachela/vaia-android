package com.vaia.data.network

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.vaia.VaiaApplication
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class ErrorInterceptor : Interceptor {

    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())

    data class ErrorResponse(
        @SerializedName("success") val success: Boolean? = null,
        val message: String? = null,
        val data: Any? = null
    )

    private val aiEndpoints = listOf("/suggestions", "/budget-advice", "/weather-suggestions")

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.encodedPath
        val isAiEndpoint = aiEndpoints.any { url.contains(it) }

        // Los fallos de red NO se avisan acá: este interceptor corre antes de que el
        // repositorio pueda caer al cache de Room, así que el toast salía incluso cuando
        // la pantalla terminaba mostrando los datos guardados. La falta de conexión la
        // comunica el banner global de modo offline; si además no hay nada cacheado,
        // cada pantalla muestra su propio mensaje.
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            val bodyString = response.body?.string()
            // Importante: Volver a crear el body porque string() lo consume
            val newBody = bodyString?.toResponseBody(response.body?.contentType())
            
            val errorMessage = try {
                val errorResponse = gson.fromJson(bodyString, ErrorResponse::class.java)
                errorResponse.message ?: "Error del servidor (${response.code})"
            } catch (e: Exception) {
                "Ocurrió un error inesperado"
            }

            if (!isAiEndpoint) {
                mainHandler.post {
                    Toast.makeText(VaiaApplication.context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
            
            return response.newBuilder().body(newBody).build()
        }

        return response
    }
}