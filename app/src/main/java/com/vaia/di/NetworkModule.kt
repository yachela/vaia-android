package com.vaia.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vaia.BuildConfig
import com.vaia.data.api.VaiaApiService
import com.vaia.data.api.CurrencyApiService
import com.vaia.data.network.ConnectivityObserver
import com.vaia.data.network.ConnectivityObserverImpl
import com.vaia.data.network.ErrorInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val accessTokenKey = stringPreferencesKey("access_token")

    /** Timeout de lectura para endpoints que dependen del modelo de IA (más lentos que el resto). */
    private const val AI_READ_TIMEOUT_SECONDS = 90

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return AppContainer.getDataStore(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(dataStore: DataStore<Preferences>): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val mockInterceptor = com.vaia.data.MockInterceptor()

        return OkHttpClient.Builder()
            .addInterceptor(mockInterceptor)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                    .header("Accept", "application/json")

                val token = runBlocking { dataStore.data.first()[accessTokenKey] }
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(ErrorInterceptor())
            .addInterceptor(loggingInterceptor)
            // Los endpoints de IA llaman a OpenRouter (hasta 30s solo de modelo) y pueden
            // superar el timeout general: sin esto la app cortaba antes y caía al fallback.
            .addInterceptor { chain ->
                val path = chain.request().url.encodedPath
                val isAiEndpoint = path.endsWith("/suggestions") ||
                    path.endsWith("/budget-advice") ||
                    path.contains("/packing-list/generate-ai") ||
                    path.endsWith("/weather-suggestions")

                if (isAiEndpoint) {
                    chain.withReadTimeout(AI_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS).proceed(chain.request())
                } else {
                    chain.proceed(chain.request())
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideVaiaApiService(retrofit: Retrofit): VaiaApiService {
        return retrofit.create(VaiaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCurrencyApiService(): CurrencyApiService {
        return Retrofit.Builder()
            .baseUrl("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CurrencyApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver {
        return ConnectivityObserverImpl(context)
    }
}
