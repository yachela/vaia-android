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

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            mainHandler.post {
                Toast.makeText(VaiaApplication.context, "Error de conexión: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
            throw e
        }

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

            mainHandler.post {
                Toast.makeText(VaiaApplication.context, errorMessage, Toast.LENGTH_SHORT).show()
            }
            
            return response.newBuilder().body(newBody).build()
        }

        return response
    }
}