package com.vaia.worker

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vaia.domain.model.Activity
import com.vaia.presentation.ui.common.normalizeDateForApi
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    /**
     * Programa un recordatorio 1 día antes de la actividad.
     * Si la actividad es hoy o ya pasó, no programa nada.
     */
    fun schedule(activity: Activity, tripTitle: String) {
        val dateStr = normalizeDateForApi(activity.date) ?: return
        val activityDate = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return
        val activityTime = runCatching { LocalTime.parse(activity.time) }.getOrNull() ?: LocalTime.of(9, 0)

        val activityDateTime = LocalDateTime.of(activityDate, activityTime)
        val reminderDateTime = activityDateTime.minusDays(1)
        val nowMillis = System.currentTimeMillis()
        val reminderMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val delayMs = reminderMillis - nowMillis

        if (delayMs <= 0) return

        val inputData = Data.Builder()
            .putString(ActivityReminderWorker.KEY_ACTIVITY_ID, activity.id)
            .putString(ActivityReminderWorker.KEY_ACTIVITY_TITLE, activity.title)
            .putString(ActivityReminderWorker.KEY_TRIP_TITLE, tripTitle)
            .build()

        val request = OneTimeWorkRequestBuilder<ActivityReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(tagFor(activity.id))
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    fun cancel(activityId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tagFor(activityId))
    }

    private fun tagFor(activityId: String) = "reminder_$activityId"
}
