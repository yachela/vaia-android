package com.vaia.di

import com.vaia.data.network.DemoModeController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import javax.inject.Singleton

/**
 * Implementación nula del modo demo para el flavor `prod`:
 * no existe MockInterceptor y el modo demo no puede activarse.
 */
@Module
@InstallIn(SingletonComponent::class)
object ProdFlavorModule {

    @Provides
    @Singleton
    fun provideDemoModeController(): DemoModeController = object : DemoModeController {
        override val mockInterceptor: Interceptor? = null

        override var isDemoEnabled: Boolean
            get() = false
            set(_) { /* no-op: el flavor prod no soporta modo demo */ }
    }
}
