package com.example.data.repository

import com.example.data.local.AdmissionTargetEntity
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettingsEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.FocusGamificationEntity
import com.example.data.local.FocusPresetEntity
import com.example.data.local.NotificationItemEntity
import com.example.data.local.StudyNoteEntity
import com.example.data.local.StudySessionEntity
import com.example.data.local.StudyTaskEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.UserProfile
import kotlinx.coroutines.flow.Flow

class StudyHubRepository(private val db: AppDatabase) {

    // User Profile
    val userProfile: Flow<UserProfile?> = db.userDao().getUser()

    suspend fun saveUserProfile(profile: UserProfile) {
        db.userDao().upsertUser(profile)
    }

    suspend fun getUserProfileSync(): UserProfile? {
        return db.userDao().getUserSync()
    }

    // Subjects
    val allSubjects: Flow<List<SubjectEntity>> = db.subjectDao().getAllSubjects()

    fun getSubjectById(id: Long): Flow<SubjectEntity?> = db.subjectDao().getSubjectById(id)

    suspend fun createSubjectWithChapters(
        name: String,
        banglaName: String?,
        colorHex: String,
        iconKey: String,
        category: String,
        initialChapterCount: Int
    ): Long {
        val subject = SubjectEntity(
            name = name.trim(),
            banglaName = banglaName?.trim()?.ifBlank { null },
            colorHex = colorHex,
            iconKey = iconKey,
            category = category
        )
        val subjectId = db.subjectDao().insertSubject(subject)

        if (initialChapterCount > 0) {
            val chapters = (1..initialChapterCount).map { index ->
                ChapterEntity(
                    subjectId = subjectId,
                    title = "Chapter $index",
                    sortOrder = index,
                    status = "NOT_STARTED"
                )
            }
            db.chapterDao().insertChapters(chapters)
        }
        return subjectId
    }

    suspend fun updateSubject(subject: SubjectEntity) {
        db.subjectDao().updateSubject(subject)
    }

    suspend fun deleteSubject(subject: SubjectEntity) {
        db.chapterDao().deleteChaptersForSubject(subject.id)
        db.subjectDao().deleteSubject(subject)
    }

    // Chapters
    val allChapters: Flow<List<ChapterEntity>> = db.chapterDao().getAllChapters()

    fun getChaptersForSubject(subjectId: Long): Flow<List<ChapterEntity>> =
        db.chapterDao().getChaptersForSubject(subjectId)

    suspend fun addChapter(subjectId: Long, title: String, sortOrder: Int): Long {
        return db.chapterDao().insertChapter(
            ChapterEntity(
                subjectId = subjectId,
                title = title.trim(),
                sortOrder = sortOrder,
                status = "NOT_STARTED"
            )
        )
    }

    suspend fun updateChapterStatus(chapter: ChapterEntity, newStatus: String) {
        db.chapterDao().updateChapter(
            chapter.copy(status = newStatus, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun deleteChapter(chapter: ChapterEntity) {
        db.chapterDao().deleteChapter(chapter)
    }

    // Study Tasks
    val allTasks: Flow<List<StudyTaskEntity>> = db.taskDao().getAllTasks()

    fun getTasksForDate(date: String): Flow<List<StudyTaskEntity>> =
        db.taskDao().getTasksForDate(date)

    suspend fun createTask(task: StudyTaskEntity): Long = db.taskDao().insertTask(task)

    suspend fun updateTaskStatus(task: StudyTaskEntity, newStatus: String) {
        db.taskDao().updateTask(
            task.copy(status = newStatus, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun updateTask(task: StudyTaskEntity) {
        db.taskDao().updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTask(task: StudyTaskEntity) {
        db.taskDao().deleteTask(task)
    }

    // Study Sessions
    val allSessions: Flow<List<StudySessionEntity>> = db.sessionDao().getAllSessions()

    fun getSessionsForRange(start: Long, end: Long): Flow<List<StudySessionEntity>> =
        db.sessionDao().getSessionsForRange(start, end)

    suspend fun logStudySession(
        subjectId: Long?,
        chapterId: Long?,
        taskId: Long?,
        startedAt: Long,
        endedAt: Long,
        elapsedSeconds: Int,
        status: String = "COMPLETED",
        sessionType: String = "POMODORO_FOCUS"
    ): Long {
        if (elapsedSeconds <= 0) return 0L
        val session = StudySessionEntity(
            subjectId = subjectId,
            chapterId = chapterId,
            taskId = taskId,
            startedAt = startedAt,
            endedAt = endedAt,
            elapsedSeconds = elapsedSeconds,
            status = status,
            sessionType = sessionType
        )
        return db.sessionDao().insertSession(session)
    }

    // Admission Targets
    val allAdmissionTargets: Flow<List<AdmissionTargetEntity>> = db.admissionDao().getAllTargets()

    fun getAdmissionTargetById(id: Long): Flow<AdmissionTargetEntity?> =
        db.admissionDao().getTargetById(id)

    suspend fun saveAdmissionTarget(target: AdmissionTargetEntity): Long =
        if (target.id == 0L) db.admissionDao().insertTarget(target)
        else {
            db.admissionDao().updateTarget(target)
            target.id
        }

    suspend fun deleteAdmissionTarget(target: AdmissionTargetEntity) {
        db.admissionDao().deleteTarget(target)
    }

    // Study Notes
    val allNotes: Flow<List<StudyNoteEntity>> = db.noteDao().getAllNotes()

    suspend fun saveNote(note: StudyNoteEntity): Long =
        if (note.id == 0L) db.noteDao().insertNote(note)
        else {
            db.noteDao().updateNote(note)
            note.id
        }

    suspend fun deleteNote(note: StudyNoteEntity) {
        db.noteDao().deleteNote(note)
    }

    // Notifications
    val allNotifications: Flow<List<NotificationItemEntity>> = db.notificationDao().getAllNotifications()
    val unreadNotificationCount: Flow<Int> = db.notificationDao().getUnreadCount()

    suspend fun postNotification(
        title: String,
        body: String,
        category: String,
        actionRoute: String? = null
    ): Long {
        return db.notificationDao().insertNotification(
            NotificationItemEntity(
                title = title,
                body = body,
                category = category,
                actionRoute = actionRoute
            )
        )
    }

    suspend fun markAllNotificationsAsRead() {
        db.notificationDao().markAllAsRead()
    }

    suspend fun deleteNotification(id: Long) {
        db.notificationDao().deleteNotificationById(id)
    }

    suspend fun getRecentNotificationsSync(limit: Int = 20): List<NotificationItemEntity> {
        return db.notificationDao().getRecentNotificationsSync(limit)
    }

    suspend fun getTodayNotificationCount(startOfDayMillis: Long): Int {
        return db.notificationDao().getTodayNotificationCount(startOfDayMillis)
    }

    // Settings
    val appSettings: Flow<AppSettingsEntity?> = db.settingsDao().getSettings()

    suspend fun getAppSettingsSync(): AppSettingsEntity? {
        return db.settingsDao().getSettingsSync()
    }

    suspend fun saveAppSettings(settings: AppSettingsEntity) {
        db.settingsDao().upsertSettings(settings)
    }

    suspend fun resetAllData() {
        db.clearAllTables()
    }

    // Focus Presets
    val allFocusPresets: Flow<List<FocusPresetEntity>> = db.focusPresetDao().getAllPresets()

    suspend fun seedDefaultFocusPresetsIfEmpty() {
        val dao = db.focusPresetDao()
        val defaultPresets = listOf(
            FocusPresetEntity(title = "Standard Pomodoro", mode = "POMODORO", workMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15, targetCycles = 4, isDefault = true),
            FocusPresetEntity(title = "Deep Focus Block", mode = "DEEP_FOCUS", workMinutes = 50, shortBreakMinutes = 10, longBreakMinutes = 20, targetCycles = 2, isStrict = true, isDefault = true),
            FocusPresetEntity(title = "Quick Review", mode = "SIMPLE_TIMER", workMinutes = 15, shortBreakMinutes = 3, longBreakMinutes = 10, targetCycles = 1, isDefault = true),
            FocusPresetEntity(title = "Exam Sprint", mode = "DEEP_FOCUS", workMinutes = 90, shortBreakMinutes = 15, longBreakMinutes = 30, targetCycles = 1, isStrict = true, isDefault = true)
        )
        dao.insertPresets(defaultPresets)
    }

    suspend fun saveFocusPreset(preset: FocusPresetEntity): Long {
        return if (preset.id == 0L) {
            db.focusPresetDao().insertPreset(preset)
        } else {
            db.focusPresetDao().updatePreset(preset)
            preset.id
        }
    }

    suspend fun deleteFocusPreset(preset: FocusPresetEntity) {
        db.focusPresetDao().deletePreset(preset)
    }

    // Gamification & Streaks
    val focusGamification: Flow<FocusGamificationEntity?> = db.focusGamificationDao().getGamification()

    suspend fun awardFocusSession(elapsedSeconds: Int, isCompleted: Boolean): FocusGamificationEntity {
        val dao = db.focusGamificationDao()
        val current = dao.getGamificationSync() ?: FocusGamificationEntity()

        val mins = elapsedSeconds / 60
        if (mins <= 0) return current

        // XP formula: 10 XP per minute + 50 XP completion bonus
        val baseMultiplier = 10
        val bonusXp = if (isCompleted) 50 else 0
        val earnedXp = (mins * baseMultiplier) + bonusXp

        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        var newCurrentStreak = current.currentStreakDays
        var newLongestStreak = current.longestStreakDays

        if (current.lastFocusDateStr != todayStr) {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val lastDate = if (current.lastFocusDateStr.isNotBlank()) {
                try { sdf.parse(current.lastFocusDateStr) } catch (e: Exception) { null }
            } else null

            val todayDate = sdf.parse(todayStr)
            val diffDays = if (lastDate != null && todayDate != null) {
                ((todayDate.time - lastDate.time) / (1000 * 60 * 60 * 24)).toInt()
            } else -1

            if (diffDays == 1) {
                newCurrentStreak += 1
            } else if (diffDays > 1 || diffDays < 0) {
                newCurrentStreak = 1
            }

            if (newCurrentStreak > newLongestStreak) {
                newLongestStreak = newCurrentStreak
            }
        }

        val totalMins = current.totalFocusMinutes + mins
        val totalSessions = current.totalSessionsCompleted + 1

        val unlockedList = current.unlockedAchievementIds.split(",").filter { it.isNotBlank() }.toMutableSet()

        if (totalSessions >= 1) unlockedList.add("first_session")
        if (newCurrentStreak >= 3) unlockedList.add("streak_3")
        if (newCurrentStreak >= 7) unlockedList.add("streak_7")
        if (totalMins >= 100) unlockedList.add("focus_100")
        if (totalMins >= 500) unlockedList.add("focus_500")
        if (totalMins >= 1000) unlockedList.add("centurion")
        if (mins >= 60) unlockedList.add("deep_diver")

        val updated = current.copy(
            totalXp = current.totalXp + earnedXp,
            currentStreakDays = newCurrentStreak,
            longestStreakDays = newLongestStreak,
            lastFocusDateStr = todayStr,
            totalSessionsCompleted = totalSessions,
            totalFocusMinutes = totalMins,
            unlockedAchievementIds = unlockedList.joinToString(",")
        )

        dao.upsertGamification(updated)
        return updated
    }
}

