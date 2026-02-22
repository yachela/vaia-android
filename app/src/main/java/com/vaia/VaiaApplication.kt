package com.vaia

import android.app.Application
import com.vaia.di.AppContainer

class VaiaApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}