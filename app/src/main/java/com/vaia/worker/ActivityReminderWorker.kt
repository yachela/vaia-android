package com.vaia.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vaia.R
import com.vaia.VaiaApplication

class ActivityReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val activityTitle = inputData.getString(KEY_ACTIVITY_TITLE) ?: return Result.failure()
        val tripTitle = inputData.getString(KEY_TRIP_TITLE) ?: return Result.failure()
        val activityId = inputData.getString(KEY_ACTIVITY_ID) ?: return Result.failure()

        val notification = NotificationCompat.Builder(applicationContext, VaiaApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_roadmap)
            .setContentTitle("Actividad mañana: $activityTitle")
            .setContentText("Tu viaje \"$tripTitle\" tiene una actividad programada para mañana.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        NotificationManagerCompat.from(applicationContext)
            .notify(activityId.hashCode(), notification)

        return Result.success()
    }

    companion object {
        const val KEY_ACTIVITY_TITLE = "activity_title"
        const val KEY_TRIP_TITLE = "trip_title"
        const val KEY_ACTIVITY_ID = "activity_id"
    }
}
