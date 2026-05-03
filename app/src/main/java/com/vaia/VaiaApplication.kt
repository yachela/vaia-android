package com.vaia

import android.app.Application
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.vaia.di.AppContainer
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VaiaApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        context = this
        appContainer = AppContainer(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de actividades",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de actividades de viaje próximas"
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "vaia_reminders"
        lateinit var context: Context
            private set
    }
}