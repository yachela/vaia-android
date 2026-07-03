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
import com.vaia.data.DemoMode
import com.vaia.data.MockInterceptor
import com.vaia.data.api.CurrencyApiService
import com.vaia.data.api.VaiaApiService
import com.vaia.data.auth.EncryptedTokenStorage
import com.vaia.data.auth.TokenProvider
import com.vaia.data.auth.TokenStorage
import com.vaia.data.network.AuthInterceptor
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
    private val demoModeKey = booleanPreferencesKey("demo_mode")

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("auth_prefs") }
        )
    }

    @Provides
    @Singleton
    fun provideTokenStorage(@ApplicationContext context: Context): TokenStorage {
        return EncryptedTokenStorage(context)
    }

    @Provides
    @Singleton
    fun provideTokenProvider(
        tokenStorage: TokenStorage,
        dataStore: DataStore<Preferences>
    ): TokenProvider {
        // Lectura única bloqueante al construir el grafo de dependencias.
        // Ocurre antes de cualquier llamada de red; el costo es ~1-5ms.
        val prefs = runBlocking { dataStore.data.first() }
        DemoMode.isEnabled = prefs[demoModeKey] ?: false

        // Migración única: si queda un token en texto plano en DataStore,
        // se mueve al almacenamiento cifrado y se elimina el original.
        val legacyToken = prefs[accessTokenKey]
        if (!legacyToken.isNullOrBlank()) {
            if (tokenStorage.getToken() == null) {
                tokenStorage.saveToken(legacyToken)
            }
            runBlocking { dataStore.edit { it.remove(accessTokenKey) } }
        }

        return TokenProvider().apply { token = tokenStorage.getToken() }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenProvider: TokenProvider): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(MockInterceptor())
            .addInterceptor(ErrorInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        // Logging HTTP solo en builds de debug: evita filtrar URLs, cabeceras
        // y tamaños de payload en los logs de producción.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }

        return builder.build()
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
