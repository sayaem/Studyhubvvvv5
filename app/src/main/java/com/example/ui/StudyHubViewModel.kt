package com.example.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AdmissionTargetEntity
import com.example.data.local.AppDatabase
import com.example.data.local.AppSettingsEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.NotificationItemEntity
import com.example.data.local.StudyNoteEntity
import com.example.data.local.StudySessionEntity
import com.example.data.local.StudyTaskEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.UserProfile
import com.example.data.repository.StudyHubRepository
import com.example.notifications.StudyHubNotificationManager
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimerStatus {
    IDLE, RUNNING, PAUSED, COMPLETED
}

class StudyHubViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = StudyHubRepository(db)
    val notificationManager = StudyHubNotificationManager(application)

    private val _isProfileLoaded = MutableStateFlow(false)
    val isProfileLoaded: StateFlow<Boolean> = _isProfileLoaded.asStateFlow()

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.userProfile.collect {
                _isProfileLoaded.value = true
            }
        }
    }

    val subjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chapters: StateFlow<List<ChapterEntity>> = repository.allChapters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<StudyTaskEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<StudySessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val admissionTargets: StateFlow<List<AdmissionTargetEntity>> = repository.allAdmissionTargets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<StudyNoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationItemEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val appSettings: StateFlow<AppSettingsEntity?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Focus Timer State
    private val _timerStatus = MutableStateFlow(TimerStatus.IDLE)
    val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow(25 * 60)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private val _totalDurationSeconds = MutableStateFlow(25 * 60)
    val totalDurationSeconds: StateFlow<Int> = _totalDurationSeconds.asStateFlow()

    private val _selectedSessionType = MutableStateFlow("POMODORO_FOCUS")
    val selectedSessionType: StateFlow<String> = _selectedSessionType.asStateFlow()

    private val _timerSubjectId = MutableStateFlow<Long?>(null)
    val timerSubjectId: StateFlow<Long?> = _timerSubjectId.asStateFlow()

    private var timerJob: Job? = null
    private var sessionStartTime: Long = 0L
    private var lastRecordedSessionId: Long? = null

    // Date formatting helper
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayDateString(): String = dateFormat.format(Date())

    // Calculations per PRD Section 11
    val todayStudySeconds: StateFlow<Int> = sessions.combine(MutableStateFlow(Unit)) { sessionList, _ ->
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        sessionList.filter {
            it.status == "COMPLETED" && it.startedAt in todayStart..todayEnd
        }.sumOf { it.elapsedSeconds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val overallSyllabusProgress: StateFlow<Int> = chapters.combine(subjects) { chapterList, subjectList ->
        if (chapterList.isEmpty() || subjectList.isEmpty()) 0
        else {
            val completed = chapterList.count { it.status == "COMPLETED" }
            ((completed.toFloat() / chapterList.size) * 100).toInt()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun calculateDaysRemaining(targetDateStr: String?): Long? {
        if (targetDateStr.isNullOrBlank()) return null
        return try {
            val targetDate = dateFormat.parse(targetDateStr) ?: return null
            val now = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            val diffMs = targetDate.time - now.time
            diffMs / (1000 * 60 * 60 * 24)
        } catch (e: Exception) {
            null
        }
    }

    // Timer Actions
    fun selectTimerDuration(minutes: Int, sessionType: String) {
        if (_timerStatus.value == TimerStatus.RUNNING) return
        _timerJobCancel()
        _totalDurationSeconds.value = minutes * 60
        _timeRemainingSeconds.value = minutes * 60
        _selectedSessionType.value = sessionType
        _timerStatus.value = TimerStatus.IDLE
    }

    fun setTimerSubject(subjectId: Long?) {
        _timerSubjectId.value = subjectId
    }

    fun startTimer() {
        if (_timerStatus.value == TimerStatus.IDLE) {
            sessionStartTime = System.currentTimeMillis()
        }
        _timerStatus.value = TimerStatus.RUNNING
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeRemainingSeconds.value > 0 && _timerStatus.value == TimerStatus.RUNNING) {
                delay(1000L)
                if (_timerStatus.value == TimerStatus.RUNNING) {
                    _timeRemainingSeconds.value -= 1
                }
            }
            if (_timeRemainingSeconds.value <= 0) {
                finishTimer(completedNormally = true)
            }
        }
    }

    fun pauseTimer() {
        if (_timerStatus.value == TimerStatus.RUNNING) {
            _timerStatus.value = TimerStatus.PAUSED
            timerJob?.cancel()
        }
    }

    fun resumeTimer() {
        if (_timerStatus.value == TimerStatus.PAUSED) {
            startTimer()
        }
    }

    fun resetTimer() {
        _timerJobCancel()
        _timeRemainingSeconds.value = _totalDurationSeconds.value
        _timerStatus.value = TimerStatus.IDLE
    }

    fun finishTimer(completedNormally: Boolean = false) {
        val totalSecs = _totalDurationSeconds.value
        val remainSecs = _timeRemainingSeconds.value
        val elapsed = if (completedNormally) totalSecs else (totalSecs - remainSecs)

        _timerJobCancel()
        _timerStatus.value = TimerStatus.COMPLETED

        if (elapsed >= 10) { // Only log meaningful sessions >= 10s
            viewModelScope.launch {
                val endTime = System.currentTimeMillis()
                val startTime = if (sessionStartTime > 0) sessionStartTime else (endTime - elapsed * 1000L)
                val id = repository.logStudySession(
                    subjectId = _timerSubjectId.value,
                    chapterId = null,
                    taskId = null,
                    startedAt = startTime,
                    endedAt = endTime,
                    elapsedSeconds = elapsed,
                    status = "COMPLETED",
                    sessionType = _selectedSessionType.value
                )
                lastRecordedSessionId = id

                // Notify completion
                val mins = elapsed / 60
                val subjectName = subjects.value.find { it.id == _timerSubjectId.value }?.name ?: "Study"
                val title = "Focus Session Finished!"
                val msg = "Great job! You focused on $subjectName for $mins minutes."

                repository.postNotification(
                    title = title,
                    body = msg,
                    category = "STUDY",
                    actionRoute = "focus"
                )
                notificationManager.postNotification(
                    id = 1001,
                    channelId = StudyHubNotificationManager.CHANNEL_STUDY,
                    title = title,
                    message = msg
                )
            }
        }
    }

    private fun _timerJobCancel() {
        timerJob?.cancel()
        timerJob = null
    }

    // Onboarding and User Profile
    fun saveOnboardingProfile(
        name: String,
        avatarId: String,
        academicLevel: String,
        group: String,
        targetYear: String,
        admissionTrack: String?,
        dailyGoalHours: Int,
        preferredTime: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val profile = UserProfile(
                displayName = name.trim().ifBlank { "Student" },
                avatarId = avatarId,
                academicLevel = academicLevel,
                academicGroup = group,
                targetYear = targetYear,
                admissionTrack = admissionTrack,
                dailyGoalMinutes = dailyGoalHours * 60,
                preferredStudyTime = preferredTime,
                isOnboardingCompleted = true,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveUserProfile(profile)

            // Seed initial relevant subjects if user has none yet
            if (subjects.value.isEmpty()) {
                seedInitialSubjects(academicLevel, admissionTrack)
            }

            // Post welcome notification
            repository.postNotification(
                title = "Welcome to StudyHub!",
                body = "Your personal study companion is ready. Plan, focus, and achieve your goals!",
                category = "SYSTEM"
            )

            onSuccess()
        }
    }

    private suspend fun seedInitialSubjects(academicLevel: String, admissionTrack: String?) {
        when (academicLevel) {
            "SSC" -> {
                repository.createSubjectWithChapters("Mathematics", "গণিত", "#2563EB", "calculator", "Compulsory", 12)
                repository.createSubjectWithChapters("Physics", "পদার্থবিজ্ঞান", "#0796A3", "atom", "Compulsory", 10)
                repository.createSubjectWithChapters("Chemistry", "রসায়ন", "#F97316", "flask", "Compulsory", 10)
                repository.createSubjectWithChapters("English", "ইংরেজি", "#EC4899", "book", "Compulsory", 8)
                repository.createSubjectWithChapters("Bangla", "বাংলা", "#8B5CF6", "pen", "Compulsory", 6)
            }
            "HSC" -> {
                repository.createSubjectWithChapters("Higher Mathematics", "উচ্চতর গণিত", "#2563EB", "calculator", "Compulsory", 10)
                repository.createSubjectWithChapters("Physics", "পদার্থবিজ্ঞান", "#0796A3", "atom", "Compulsory", 10)
                repository.createSubjectWithChapters("Chemistry", "রসায়ন", "#F97316", "flask", "Compulsory", 10)
                repository.createSubjectWithChapters("Biology", "জীববিজ্ঞান", "#10B981", "biotech", "Compulsory", 8)
                repository.createSubjectWithChapters("English", "ইংরেজি", "#EC4899", "book", "Compulsory", 8)
            }
            "Admission" -> {
                when (admissionTrack) {
                    "Medical" -> {
                        repository.createSubjectWithChapters("Biology", "জীববিজ্ঞান", "#10B981", "biotech", "Compulsory", 12)
                        repository.createSubjectWithChapters("Chemistry", "রসায়ন", "#F97316", "flask", "Compulsory", 10)
                        repository.createSubjectWithChapters("Physics", "পদার্থবিজ্ঞান", "#0796A3", "atom", "Compulsory", 10)
                        repository.createSubjectWithChapters("English & GK", "ইংরেজি ও সাধারণ জ্ঞান", "#EC4899", "book", "Compulsory", 8)
                    }
                    "Engineering" -> {
                        repository.createSubjectWithChapters("Mathematics", "গণিত", "#2563EB", "calculator", "Compulsory", 12)
                        repository.createSubjectWithChapters("Physics", "পদার্থবিজ্ঞান", "#0796A3", "atom", "Compulsory", 12)
                        repository.createSubjectWithChapters("Chemistry", "রসায়ন", "#F97316", "flask", "Compulsory", 10)
                        repository.createSubjectWithChapters("English", "ইংরেজি", "#EC4899", "book", "Compulsory", 6)
                    }
                    else -> {
                        repository.createSubjectWithChapters("Bangla", "বাংলা", "#8B5CF6", "pen", "Compulsory", 10)
                        repository.createSubjectWithChapters("English", "ইংরেজি", "#EC4899", "book", "Compulsory", 10)
                        repository.createSubjectWithChapters("General Knowledge", "সাধারণ জ্ঞান", "#0796A3", "book", "Compulsory", 8)
                    }
                }
            }
        }
    }

    // Subject operations
    fun createSubject(
        name: String,
        banglaName: String?,
        colorHex: String,
        iconKey: String,
        category: String,
        chapterCount: Int,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createSubjectWithChapters(
                name = name,
                banglaName = banglaName,
                colorHex = colorHex,
                iconKey = iconKey,
                category = category,
                initialChapterCount = chapterCount
            )
            onSuccess()
        }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
        }
    }

    // Chapter operations
    fun updateChapterStatus(chapter: ChapterEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateChapterStatus(chapter, newStatus)
        }
    }

    fun addChapter(subjectId: Long, title: String, count: Int = 1) {
        viewModelScope.launch {
            val existing = chapters.value.filter { it.subjectId == subjectId }
            val nextSort = existing.size + 1
            repository.addChapter(subjectId, title.ifBlank { "Chapter $nextSort" }, nextSort)
        }
    }

    fun deleteChapter(chapter: ChapterEntity) {
        viewModelScope.launch {
            repository.deleteChapter(chapter)
        }
    }

    // Task operations
    fun createTask(
        title: String,
        date: String,
        time: String?,
        duration: Int,
        subjectId: Long?,
        priority: String,
        notes: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createTask(
                StudyTaskEntity(
                    title = title.trim(),
                    date = date,
                    time = time,
                    durationMinutes = duration,
                    subjectId = subjectId,
                    status = "PENDING",
                    priority = priority,
                    notes = notes
                )
            )
            onSuccess()
        }
    }

    fun createTargetTask(
        title: String,
        date: String,
        targetId: Long,
        priority: String = "MEDIUM",
        notes: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.createTask(
                StudyTaskEntity(
                    title = title.trim(),
                    date = date,
                    targetId = targetId,
                    status = "PENDING",
                    priority = priority,
                    notes = notes
                )
            )
            onSuccess()
        }
    }

    fun toggleTaskStatus(task: StudyTaskEntity) {
        viewModelScope.launch {
            val newStatus = if (task.status == "COMPLETED") "PENDING" else "COMPLETED"
            repository.updateTaskStatus(task, newStatus)
        }
    }

    fun deleteTask(task: StudyTaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    // Admission target operations
    fun saveAdmissionTarget(target: AdmissionTargetEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveAdmissionTarget(target)
            onSuccess()
        }
    }

    fun deleteAdmissionTarget(target: AdmissionTargetEntity) {
        viewModelScope.launch {
            repository.deleteAdmissionTarget(target)
        }
    }

    // Study notes operations
    fun saveNote(note: StudyNoteEntity, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveNote(note)
            onSuccess()
        }
    }

    fun deleteNote(note: StudyNoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // Notifications operations
    fun initializeNotificationsOnLaunch() {
        viewModelScope.launch {
            try {
                val settings = repository.getAppSettingsSync() ?: AppSettingsEntity()
                if (settings.studyRemindersEnabled) {
                    com.example.notifications.NotificationScheduler.scheduleNextAdaptiveOpportunity(
                        context = getApplication(),
                        delayMinutes = 180L
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateMasterNotificationToggle(enabled: Boolean) {
        viewModelScope.launch {
            val current = appSettings.value ?: AppSettingsEntity()
            val updated = current.copy(studyRemindersEnabled = enabled)
            repository.saveAppSettings(updated)

            if (enabled) {
                com.example.notifications.NotificationScheduler.scheduleNextAdaptiveOpportunity(
                    context = getApplication(),
                    delayMinutes = 60L
                )
            } else {
                com.example.notifications.NotificationScheduler.cancelPendingNotifications(getApplication())
            }
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    // Settings operations
    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            val current = appSettings.value ?: AppSettingsEntity()
            repository.saveAppSettings(current.copy(themeMode = mode.name))
        }
    }

    fun updateSettings(updated: AppSettingsEntity) {
        viewModelScope.launch {
            repository.saveAppSettings(updated)
        }
    }

    fun resetAllData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.resetAllData()
            onComplete()
        }
    }
}
