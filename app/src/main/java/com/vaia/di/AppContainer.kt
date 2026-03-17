package com.vaia.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.vaia.BuildConfig
import com.vaia.data.MockInterceptor
import com.vaia.data.DemoMode
import com.vaia.data.api.VaiaApiService
import com.vaia.data.local.db.VaiaDatabase
import com.vaia.data.repository.*
import com.vaia.domain.repository.*
import com.vaia.domain.usecase.DeleteDocumentUseCase
import com.vaia.domain.usecase.GetTripDocumentsUseCase
import com.vaia.domain.usecase.UploadDocumentUseCase
import com.vaia.worker.ReminderScheduler
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
    private val demoModeKey = booleanPreferencesKey("demo_mode")

    // Token cacheado en memoria para evitar runBlocking por cada request HTTP.
    // Se inicializa una única vez al construir AppContainer (antes de cualquier llamada de red).
    @Volatile private var cachedToken: String? = null

    // DataStore
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("auth_prefs") }
        )
    }

    init {
        // Lectura única bloqueante al inicio de la app para poblar el caché del token.
        // Ocurre antes de que cualquier llamada de red sea posible; el costo es ~1-5ms.
        val prefs = runBlocking { dataStore.data.first() }
        cachedToken = prefs[accessTokenKey]
        
        // Check demo mode from preferences
        DemoMode.isEnabled = prefs[demoModeKey] ?: false
    }

    internal fun updateCachedToken(token: String?) {
        cachedToken = token
    }

    suspend fun setDemoMode(enabled: Boolean) {
        DemoMode.isEnabled = enabled
        dataStore.edit { prefs ->
            prefs[demoModeKey] = enabled
        }
    }

    // Network
    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    private val mockInterceptor = MockInterceptor()

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .header("Accept", "application/json")

                val token = cachedToken
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                if (DemoMode.isEnabled) {
                    mockInterceptor.intercept(chain)
                } else {
                    chain.proceed(requestBuilder.build())
                }
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

    // Local DB
    private val database: VaiaDatabase by lazy {
        VaiaDatabase.getInstance(context)
    }

    // Repositories
    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(apiService, dataStore, onTokenUpdated = { token ->
            updateCachedToken(token)
        })
    }

    val tripRepository: TripRepository by lazy {
        TripRepositoryImpl(apiService, database.tripDao())
    }

    val activityRepository: ActivityRepository by lazy {
        ActivityRepositoryImpl(apiService, database.activityDao())
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

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(context)
    }
}
