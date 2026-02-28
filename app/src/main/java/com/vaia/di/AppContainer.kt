package com.vaia.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.vaia.BuildConfig
import com.vaia.data.api.VaiaApiService
import com.vaia.data.repository.*
import com.vaia.domain.repository.*
import com.vaia.domain.usecase.DeleteDocumentUseCase
import com.vaia.domain.usecase.GetTripDocumentsUseCase
import com.vaia.domain.usecase.UploadDocumentUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {

    // Forzamos el tipo String para evitar ambigüedades en Retrofit
    private val baseUrl: String = BuildConfig.API_BASE_URL
    private val accessTokenKey = stringPreferencesKey("access_token")

    // DataStore
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("auth_prefs") }
        )
    }

    // Network
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = runBlocking {
                    dataStore.data.map { preferences ->
                        preferences[accessTokenKey]
                    }.first()
                }

                val requestBuilder = chain.request().newBuilder()
                    .header("Accept", "application/json")

                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                val request = requestBuilder
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val apiService: VaiaApiService by lazy {
        retrofit.create(VaiaApiService::class.java)
    }

    // Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(apiService, dataStore)
    }

    val tripRepository: TripRepository by lazy {
        TripRepositoryImpl(apiService)
    }

    val activityRepository: ActivityRepository by lazy {
        ActivityRepositoryImpl(apiService)
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(apiService)
    }

    val documentRepository: DocumentRepository by lazy {
        DocumentRepositoryImpl(apiService)
    }

    // Use Cases
    val getTripDocumentsUseCase: GetTripDocumentsUseCase by lazy {
        GetTripDocumentsUseCase(documentRepository)
    }

    val uploadDocumentUseCase: UploadDocumentUseCase by lazy {
        UploadDocumentUseCase(documentRepository)
    }

    val deleteDocumentUseCase: DeleteDocumentUseCase by lazy {
        DeleteDocumentUseCase(documentRepository)
    }
}
