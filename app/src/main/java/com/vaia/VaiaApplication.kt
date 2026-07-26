package com.vaia

import android.app.Application
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.vaia.di.AppContainer
import dagger.hilt.android.HiltAndroidApp
import com.google.android.libraries.places.api.Places

@HiltAndroidApp
class VaiaApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        context = this
        appContainer = AppContainer(this)
        createNotificationChannel()

        // Initialize Places. Si MAPS_API_KEY no está configurada (p. ej. build local
        // sin la key en local.properties), no inicializamos en vez de crashear:
        // Places.initialize lanza IllegalArgumentException con key vacía.
        if (BuildConfig.MAPS_API_KEY.isBlank()) {
            Log.w(
                "VaiaApplication",
                "MAPS_API_KEY vacía: Places no se inicializa. Mapas y autocompletado " +
                    "de lugares quedan deshabilitados. Configurá la key en local.properties " +
                    "(o el secret de CI) para habilitarlos."
            )
        } else if (!Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
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