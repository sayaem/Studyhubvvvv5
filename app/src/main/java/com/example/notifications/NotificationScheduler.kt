package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object NotificationScheduler {

    const val PROACTIVE_NOTIFICATION_ID = 8888

    fun scheduleNextAdaptiveOpportunity(
        context: Context,
        delayMinutes: Long = 210L // Default ~3.5 hours opportunity window
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTimeMs = System.currentTimeMillis() + (delayMinutes * 60_000L)

        val intent = Intent(context, StudyHubNotificationReceiver::class.java).apply {
            action = StudyHubNotificationReceiver.ACTION_TRIGGER_NOTIFICATION
            putExtra(StudyHubNotificationReceiver.EXTRA_NOTIFICATION_ID, PROACTIVE_NOTIFICATION_ID)
        }

        val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            PROACTIVE_NOTIFICATION_ID,
            intent,
            pendingFlags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelPendingNotifications(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, StudyHubNotificationReceiver::class.java).apply {
                action = StudyHubNotificationReceiver.ACTION_TRIGGER_NOTIFICATION
            }
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                PROACTIVE_NOTIFICATION_ID,
                intent,
                pendingFlags
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
