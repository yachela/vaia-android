package com.vaia

import android.app.Application
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import com.google.android.libraries.places.api.Places

@HiltAndroidApp
class VaiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = this
        createNotificationChannel()

        // Initialize Places
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyDvFWKqyPjK1bvXhTgPE-wCJDZemGo0RAk")
        }
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