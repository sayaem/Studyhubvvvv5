package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettingsEntity
import com.example.data.local.NotificationItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class StudyHubNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_NOTIFICATION = "com.example.studyhub.TRIGGER_NOTIFICATION"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TRIGGER_NOTIFICATION) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val settings = db.settingsDao().getSettingsSync() ?: AppSettingsEntity()

                // Check master ON/OFF
                if (!settings.studyRemindersEnabled) {
                    return@launch
                }

                // Gather student context
                val userProfile = db.userDao().getUserSync()
                val subjects = db.subjectDao().getAllSubjects().first()
                val chapters = db.chapterDao().getAllChapters().first()
                val tasks = db.taskDao().getAllTasks().first()
                val admissionTargets = db.admissionDao().getAllTargets().first()
                val notes = db.noteDao().getAllNotes().first()
                val recentNotifications = db.notificationDao().getRecentNotificationsSync(30)

                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val todayCount = db.notificationDao().getTodayNotificationCount(startOfDay)

                // Filter candidates
                val eligibleTemplates = NotificationContentLibrary.ALL_NOTIFICATIONS.filter { template ->
                    NotificationEligibilityEvaluator.isEligible(
                        template = template,
                        settings = settings,
                        userProfile = userProfile,
                        subjects = subjects,
                        chapters = chapters,
                        tasks = tasks,
                        admissionTargets = admissionTargets,
                        notes = notes,
                        isFocusTimerActive = false,
                        recentNotifications = recentNotifications,
                        todayNotificationCount = todayCount
                    )
                }

                val selectedTemplate = AdaptiveNotificationEngine.selectBestCandidate(
                    templates = eligibleTemplates,
                    userProfile = userProfile,
                    subjects = subjects,
                    chapters = chapters,
                    tasks = tasks,
                    admissionTargets = admissionTargets,
                    notes = notes,
                    recentNotifications = recentNotifications
                )

                if (selectedTemplate != null) {
                    val notifId = (selectedTemplate.id + System.currentTimeMillis().toString()).hashCode()

                    // 1. Insert into history
                    db.notificationDao().insertNotification(
                        NotificationItemEntity(
                            title = selectedTemplate.title,
                            body = selectedTemplate.body,
                            category = selectedTemplate.category.name,
                            isRead = false,
                            createdAt = System.currentTimeMillis(),
                            actionRoute = selectedTemplate.actionRoute.name.lowercase()
                        )
                    )

                    // 2. Update cooldown timestamp
                    db.settingsDao().upsertSettings(
                        settings.copy(lastNotificationTime = System.currentTimeMillis())
                    )

                    // 3. Post notification
                    val manager = StudyHubNotificationManager(context)
                    val channelId = when (selectedTemplate.category) {
                        NotificationCategory.EXAM_ALERTS, NotificationCategory.ADMISSION_READINESS -> StudyHubNotificationManager.CHANNEL_ADMISSION
                        NotificationCategory.STREAKS_PROGRESS, NotificationCategory.HEALTHY_HABITS -> StudyHubNotificationManager.CHANNEL_DAILY_GOAL
                        else -> StudyHubNotificationManager.CHANNEL_STUDY
                    }

                    manager.postNotification(
                        id = notifId,
                        channelId = channelId,
                        title = selectedTemplate.title,
                        message = selectedTemplate.body,
                        actionRoute = selectedTemplate.actionRoute.name.lowercase()
                    )

                    // 4. Show in-app banner
                    InAppBannerManager.showBanner(
                        InAppBannerData(
                            title = selectedTemplate.title,
                            body = selectedTemplate.body,
                            actionRoute = selectedTemplate.actionRoute.name.lowercase()
                        )
                    )
                }

                // 5. Schedule next adaptive opportunity (~3.5 hours later)
                NotificationScheduler.scheduleNextAdaptiveOpportunity(context, delayMinutes = 210L)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
