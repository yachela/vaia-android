package com.vaia.di

import com.vaia.data.DemoMode
import com.vaia.data.MockInterceptor
import com.vaia.data.network.DemoModeController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import javax.inject.Singleton

/**
 * Implementación del modo demo para el flavor `demo`:
 * expone el MockInterceptor y el estado global de DemoMode.
 */
@Module
@InstallIn(SingletonComponent::class)
object DemoFlavorModule {

    @Provides
    @Singleton
    fun provideDemoModeController(): DemoModeController = object : DemoModeController {
        override val mockInterceptor: Interceptor = MockInterceptor()

        override var isDemoEnabled: Boolean
            get() = DemoMode.isEnabled
            set(value) {
                DemoMode.isEnabled = value
            }
    }
}
