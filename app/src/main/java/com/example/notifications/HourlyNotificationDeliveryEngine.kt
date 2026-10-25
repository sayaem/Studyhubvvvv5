package com.example.notifications

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettingsEntity
import com.example.data.local.NotificationItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class DeliveryResult {
    data class Success(
        val template: NotificationTemplate,
        val source: String,
        val slotKey: String
    ) : DeliveryResult()

    data class Skipped(val reason: String) : DeliveryResult()
}

object HourlyNotificationDeliveryEngine {

    const val PREFS_NAME = "studyhub_hourly_notifications"
    const val KEY_LAST_DELIVERED_SLOT = "last_delivered_slot"
    const val KEY_LAST_DELIVERED_DATE = "last_delivered_date"
    const val KEY_DELIVERED_SET_IDS_TODAY = "delivered_set_ids_today"
    const val KEY_LAST_DELIVERY_TIME_MS = "last_delivery_time_ms"
    const val KEY_LAST_DELIVERED_SOURCE = "last_delivered_source"
    const val KEY_LAST_DELIVERED_TITLE = "last_delivered_title"

    const val SOURCE_NOTIFICATION_SET = "NOTIFICATION_SET"
    const val SOURCE_ALGORITHM = "ALGORITHM"

    private val executionMutex = Mutex()

    fun getSlotKey(timeMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMs }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}-${cal.get(Calendar.HOUR_OF_DAY)}"
    }

    fun getDateKey(timeMs: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMs }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    fun isSlotDelivered(context: Context, slotKey: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastSlot = prefs.getString(KEY_LAST_DELIVERED_SLOT, null)
        return lastSlot == slotKey
    }

    fun getDeliveredSetIdsToday(context: Context, timeMs: Long = System.currentTimeMillis()): Set<String> {
        resetDailyHistoryIfNewDay(context, timeMs)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_DELIVERED_SET_IDS_TODAY, emptySet())?.toSet() ?: emptySet()
    }

    fun recordDelivery(
        context: Context,
        slotKey: String,
        dateKey: String,
        templateId: String,
        source: String,
        title: String,
        timeMs: Long = System.currentTimeMillis()
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentSet = prefs.getStringSet(KEY_DELIVERED_SET_IDS_TODAY, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (source == SOURCE_NOTIFICATION_SET) {
            currentSet.add(templateId)
        }

        prefs.edit()
            .putString(KEY_LAST_DELIVERED_SLOT, slotKey)
            .putString(KEY_LAST_DELIVERED_DATE, dateKey)
            .putLong(KEY_LAST_DELIVERY_TIME_MS, timeMs)
            .putString(KEY_LAST_DELIVERED_SOURCE, source)
            .putString(KEY_LAST_DELIVERED_TITLE, title)
            .putStringSet(KEY_DELIVERED_SET_IDS_TODAY, currentSet)
            .apply()
    }

    fun resetDailyHistoryIfNewDay(context: Context, timeMs: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_LAST_DELIVERED_DATE, null)
        val currentDate = getDateKey(timeMs)

        if (savedDate != null && savedDate != currentDate) {
            // New local calendar day has arrived.
            // Reset daily used Notification Set items while preserving the last delivered slot key.
            prefs.edit()
                .putString(KEY_LAST_DELIVERED_DATE, currentDate)
                .putStringSet(KEY_DELIVERED_SET_IDS_TODAY, emptySet())
                .apply()
        } else if (savedDate == null) {
            prefs.edit().putString(KEY_LAST_DELIVERED_DATE, currentDate).apply()
        }
    }

    fun clearAllHistoryForTesting(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * Executes the hourly delivery logic with global hourly slot protection,
     * same-day Notification Set deduplication, and algorithm fallback.
     */
    suspend fun executeHourlyDelivery(
        context: Context,
        timeMs: Long = System.currentTimeMillis(),
        forceSlotKey: String? = null,
        customNotificationSet: List<NotificationTemplate>? = null
    ): DeliveryResult = executionMutex.withLock {
        val db = AppDatabase.getDatabase(context)
        val settings = db.settingsDao().getSettingsSync() ?: AppSettingsEntity()

        // 1. Master toggle check
        if (!settings.studyRemindersEnabled) {
            return DeliveryResult.Skipped("DISABLED")
        }

        // 2. Quiet hours check
        val cal = Calendar.getInstance().apply { this.timeInMillis = timeMs }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        if (NotificationEligibilityEvaluator.isQuietHour(currentHour, settings.quietHoursStartHour, settings.quietHoursEndHour)) {
            return DeliveryResult.Skipped("QUIET_HOURS")
        }

        // 3. Focus timer session suppression
        val activeSession = db.activeFocusSessionDao().getActiveSession()
        if (activeSession != null && activeSession.sessionState.contains("RUNNING")) {
            return DeliveryResult.Skipped("FOCUS_ACTIVE")
        }

        // 4. System notification permission
        val notificationManager = StudyHubNotificationManager(context)
        if (!notificationManager.hasNotificationPermission()) {
            return DeliveryResult.Skipped("NO_PERMISSION")
        }

        // 5. Global hourly slot duplicate check
        val slotKey = forceSlotKey ?: getSlotKey(timeMs)
        if (isSlotDelivered(context, slotKey)) {
            return DeliveryResult.Skipped("ALREADY_DELIVERED_THIS_HOUR")
        }

        // 6. Reset daily used items if rollover occurred
        resetDailyHistoryIfNewDay(context, timeMs)
        val dateKey = getDateKey(timeMs)
        val deliveredSetIds = getDeliveredSetIdsToday(context, timeMs)

        // 7. Gather student context
        val userProfile = db.userDao().getUserSync()
        val subjects = db.subjectDao().getAllSubjects().first()
        val chapters = db.chapterDao().getAllChapters().first()
        val tasks = db.taskDao().getAllTasks().first()
        val admissionTargets = db.admissionDao().getAllTargets().first()
        val notes = db.noteDao().getAllNotes().first()
        val recentNotifications = db.notificationDao().getRecentNotificationsSync(30)

        val startOfDay = Calendar.getInstance().apply {
            this.timeInMillis = timeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayCount = db.notificationDao().getTodayNotificationCount(startOfDay)

        // 8. EVALUATE SOURCE 1: Existing Notification Set
        val baseSet = customNotificationSet ?: NotificationContentLibrary.ALL_NOTIFICATIONS
        val unusedTemplates = baseSet.filter { it.id !in deliveredSetIds }

        val eligibleSetTemplates = unusedTemplates.filter { template ->
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
                todayNotificationCount = todayCount,
                currentHour = currentHour,
                currentTimeMs = timeMs
            )
        }

        var selectedTemplate: NotificationTemplate? = null
        var selectedSource = SOURCE_NOTIFICATION_SET

        if (eligibleSetTemplates.isNotEmpty()) {
            selectedTemplate = AdaptiveNotificationEngine.selectBestCandidate(
                templates = eligibleSetTemplates,
                userProfile = userProfile,
                subjects = subjects,
                chapters = chapters,
                tasks = tasks,
                admissionTargets = admissionTargets,
                notes = notes,
                recentNotifications = recentNotifications,
                currentHour = currentHour
            )
        }

        // 9. EVALUATE SOURCE 2: Algorithm-Generated Notification (when Notification Set is exhausted for the day)
        if (selectedTemplate == null) {
            val algorithmCandidate = AdaptiveNotificationEngine.generateContextualAlgorithmNotification(
                userProfile = userProfile,
                subjects = subjects,
                chapters = chapters,
                tasks = tasks,
                admissionTargets = admissionTargets,
                notes = notes,
                currentHour = currentHour
            )

            if (algorithmCandidate != null) {
                val isAlgoEligible = NotificationEligibilityEvaluator.isEligible(
                    template = algorithmCandidate,
                    settings = settings,
                    userProfile = userProfile,
                    subjects = subjects,
                    chapters = chapters,
                    tasks = tasks,
                    admissionTargets = admissionTargets,
                    notes = notes,
                    isFocusTimerActive = false,
                    recentNotifications = recentNotifications,
                    todayNotificationCount = todayCount,
                    currentHour = currentHour,
                    currentTimeMs = timeMs
                )
                if (isAlgoEligible) {
                    selectedTemplate = algorithmCandidate
                    selectedSource = SOURCE_ALGORITHM
                }
            }
        }

        // 10. Handle empty state: do not fabricate notifications if neither source has eligible content
        if (selectedTemplate == null) {
            return DeliveryResult.Skipped("NO_ELIGIBLE_CONTENT")
        }

        // 11. Post system notification
        val notifId = (selectedTemplate.id + timeMs.toString()).hashCode()
        val channelId = when (selectedTemplate.category) {
            NotificationCategory.EXAM_ALERTS, NotificationCategory.ADMISSION_READINESS ->
                StudyHubNotificationManager.CHANNEL_ADMISSION
            NotificationCategory.STREAKS_PROGRESS, NotificationCategory.HEALTHY_HABITS ->
                StudyHubNotificationManager.CHANNEL_DAILY_GOAL
            else -> StudyHubNotificationManager.CHANNEL_STUDY
        }

        val posted = notificationManager.postNotification(
            id = notifId,
            channelId = channelId,
            title = selectedTemplate.title,
            message = selectedTemplate.body,
            actionRoute = selectedTemplate.actionRoute.name.lowercase()
        )

        // 12. Insert into in-app notification history
        db.notificationDao().insertNotification(
            NotificationItemEntity(
                title = selectedTemplate.title,
                body = selectedTemplate.body,
                category = selectedTemplate.category.name,
                isRead = false,
                createdAt = timeMs,
                actionRoute = selectedTemplate.actionRoute.name.lowercase()
            )
        )

        // 13. Update cooldown in settings
        db.settingsDao().upsertSettings(
            settings.copy(lastNotificationTime = timeMs)
        )

        // 14. Record persistent delivery history (slot + daily IDs)
        recordDelivery(
            context = context,
            slotKey = slotKey,
            dateKey = dateKey,
            templateId = selectedTemplate.id,
            source = selectedSource,
            title = selectedTemplate.title,
            timeMs = timeMs
        )

        // 15. Show in-app banner for active foreground experience
        InAppBannerManager.showBanner(
            InAppBannerData(
                title = selectedTemplate.title,
                body = selectedTemplate.body,
                actionRoute = selectedTemplate.actionRoute.name.lowercase()
            )
        )

        return DeliveryResult.Success(
            template = selectedTemplate,
            source = selectedSource,
            slotKey = slotKey
        )
    }
}
