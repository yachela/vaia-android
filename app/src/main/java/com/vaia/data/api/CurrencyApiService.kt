package com.vaia.data.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface CurrencyApiService {
    @GET("currencies/{code}.json")
    suspend fun getLatestRates(@Path("code") code: String): Response<JsonObject>

    @GET("currencies.json")
    suspend fun getCurrencies(): Response<Map<String, String>>
}
