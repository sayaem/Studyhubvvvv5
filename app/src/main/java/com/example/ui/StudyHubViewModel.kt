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

    val focusPresets: StateFlow<List<com.example.data.local.FocusPresetEntity>> = repository.allFocusPresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusGamification: StateFlow<com.example.data.local.FocusGamificationEntity?> = repository.focusGamification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Focus 2.0 Timer State
    private val _timerStatus = MutableStateFlow(TimerStatus.IDLE)
    val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

    private val _timerMode = MutableStateFlow("POMODORO") // "POMODORO", "DEEP_FOCUS", "SIMPLE_TIMER", "STOPWATCH"
    val timerMode: StateFlow<String> = _timerMode.asStateFlow()

    private val _pomodoroPhase = MutableStateFlow("WORK") // "WORK", "SHORT_BREAK", "LONG_BREAK"
    val pomodoroPhase: StateFlow<String> = _pomodoroPhase.asStateFlow()

    private val _currentCycle = MutableStateFlow(1)
    val currentCycle: StateFlow<Int> = _currentCycle.asStateFlow()

    private val _targetCycles = MutableStateFlow(4)
    val targetCycles: StateFlow<Int> = _targetCycles.asStateFlow()

    private val _isStrictFocus = MutableStateFlow(false)
    val isStrictFocus: StateFlow<Boolean> = _isStrictFocus.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow(25 * 60)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private val _totalDurationSeconds = MutableStateFlow(25 * 60)
    val totalDurationSeconds: StateFlow<Int> = _totalDurationSeconds.asStateFlow()

    private val _selectedSessionType = MutableStateFlow("POMODORO_FOCUS")
    val selectedSessionType: StateFlow<String> = _selectedSessionType.asStateFlow()

    private val _timerSubjectId = MutableStateFlow<Long?>(null)
    val timerSubjectId: StateFlow<Long?> = _timerSubjectId.asStateFlow()

    private val _timerChapterId = MutableStateFlow<Long?>(null)
    val timerChapterId: StateFlow<Long?> = _timerChapterId.asStateFlow()

    private val _selectedGoalTask = MutableStateFlow<String?>(null)
    val selectedGoalTask: StateFlow<String?> = _selectedGoalTask.asStateFlow()

    private val _ambientSoundType = MutableStateFlow("OFF")
    val ambientSoundType: StateFlow<String> = _ambientSoundType.asStateFlow()

    private val _ambientVolume = MutableStateFlow(0.7f)
    val ambientVolume: StateFlow<Float> = _ambientVolume.asStateFlow()

    val ambientSoundManager = com.example.notifications.AmbientSoundManager()

    // Persistent Wall-Clock Timing Parameters
    private var startTimeMs: Long = 0L
    private var targetDurationMs: Long = 25 * 60 * 1000L
    private var accumulatedPauseMs: Long = 0L
    private var lastPauseMs: Long = 0L

    private var timerJob: Job? = null
    private var lastRecordedSessionId: Long? = null
    private var isTimerRestored = false

    init {
        viewModelScope.launch {
            repository.seedDefaultFocusPresetsIfEmpty()
        }
        viewModelScope.launch {
            repository.appSettings.collect { settings ->
                if (settings != null && !isTimerRestored) {
                    isTimerRestored = true
                    _timerStatus.value = try { TimerStatus.valueOf(settings.timerStatus) } catch (e: Exception) { TimerStatus.IDLE }
                    _timerMode.value = settings.timerMode
                    _pomodoroPhase.value = settings.pomodoroPhase
                    _currentCycle.value = settings.currentCycle
                    _targetCycles.value = settings.targetCycles
                    _totalDurationSeconds.value = settings.totalDurationSeconds
                    _isStrictFocus.value = settings.isStrictFocus
                    _timerSubjectId.value = settings.timerSubjectId
                    _timerChapterId.value = settings.timerChapterId
                    _selectedGoalTask.value = settings.selectedGoalTask

                    startTimeMs = settings.startTimeMs
                    targetDurationMs = settings.targetDurationMs
                    accumulatedPauseMs = settings.accumulatedPauseMs
                    lastPauseMs = settings.lastPauseMs

                    if (_timerStatus.value == TimerStatus.RUNNING) {
                        val now = System.currentTimeMillis()
                        val elapsedMs = maxOf(0L, now - startTimeMs - accumulatedPauseMs)
                        if (_timerMode.value == "STOPWATCH") {
                            _timeRemainingSeconds.value = (elapsedMs / 1000L).toInt()
                            startTimerInternal()
                        } else {
                            val remainMs = maxOf(0L, targetDurationMs - elapsedMs)
                            val remainSecs = (remainMs / 1000L).toInt()
                            if (remainMs <= 0) {
                                finishTimer(completedNormally = true)
                            } else {
                                _timeRemainingSeconds.value = remainSecs
                                startTimerInternal()
                            }
                        }
                    } else if (_timerStatus.value == TimerStatus.PAUSED) {
                        _timeRemainingSeconds.value = settings.timeRemainingSeconds
                    } else {
                        _timeRemainingSeconds.value = settings.timeRemainingSeconds.takeIf { it > 0 } ?: settings.totalDurationSeconds
                    }
                }
            }
        }
    }

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



    fun setTimerChapter(chapterId: Long?) {
        _timerChapterId.value = chapterId
        saveTimerStateToDb()
    }

    private fun saveTimerStateToDb() {
        viewModelScope.launch {
            val current = repository.getAppSettingsSync() ?: AppSettingsEntity()
            repository.saveAppSettings(
                current.copy(
                    timerStatus = _timerStatus.value.name,
                    timerMode = _timerMode.value,
                    pomodoroPhase = _pomodoroPhase.value,
                    currentCycle = _currentCycle.value,
                    targetCycles = _targetCycles.value,
                    totalDurationSeconds = _totalDurationSeconds.value,
                    timeRemainingSeconds = _timeRemainingSeconds.value,
                    startTimeMs = startTimeMs,
                    targetDurationMs = targetDurationMs,
                    accumulatedPauseMs = accumulatedPauseMs,
                    lastPauseMs = lastPauseMs,
                    isStrictFocus = _isStrictFocus.value,
                    timerSubjectId = _timerSubjectId.value,
                    timerChapterId = _timerChapterId.value,
                    selectedGoalTask = _selectedGoalTask.value
                )
            )
        }
    }

    // Timer Actions
    fun setTimerMode(mode: String) {
        if (_timerStatus.value == TimerStatus.RUNNING) return
        _timerMode.value = mode
        when (mode) {
            "POMODORO" -> {
                _pomodoroPhase.value = "WORK"
                _currentCycle.value = 1
                selectTimerDuration(25, "POMODORO_FOCUS")
            }
            "DEEP_FOCUS" -> {
                selectTimerDuration(50, "POMODORO_FOCUS")
            }
            "SIMPLE_TIMER" -> {
                selectTimerDuration(15, "CUSTOM")
            }
            "STOPWATCH" -> {
                _timerJobCancel()
                _totalDurationSeconds.value = 0
                _timeRemainingSeconds.value = 0
                _selectedSessionType.value = "CUSTOM"
                _timerStatus.value = TimerStatus.IDLE
            }
        }
        saveTimerStateToDb()
    }

    fun applyPreset(preset: com.example.data.local.FocusPresetEntity) {
        if (_timerStatus.value == TimerStatus.RUNNING) return
        _timerMode.value = preset.mode
        _isStrictFocus.value = preset.isStrict
        _targetCycles.value = preset.targetCycles
        _currentCycle.value = 1
        _pomodoroPhase.value = "WORK"
        selectTimerDuration(preset.workMinutes, if (preset.mode == "POMODORO") "POMODORO_FOCUS" else "CUSTOM")
        saveTimerStateToDb()
    }

    fun saveCustomPreset(
        title: String,
        mode: String,
        workMins: Int,
        shortBreakMins: Int,
        longBreakMins: Int,
        cycles: Int,
        isStrict: Boolean
    ) {
        viewModelScope.launch {
            repository.saveFocusPreset(
                com.example.data.local.FocusPresetEntity(
                    title = title.ifBlank { "Custom $workMins m" },
                    mode = mode,
                    workMinutes = workMins,
                    shortBreakMinutes = shortBreakMins,
                    longBreakMinutes = longBreakMins,
                    targetCycles = cycles,
                    isStrict = isStrict,
                    isDefault = false
                )
            )
        }
    }

    fun deleteFocusPreset(preset: com.example.data.local.FocusPresetEntity) {
        viewModelScope.launch {
            repository.deleteFocusPreset(preset)
        }
    }

    fun setGoalTask(goal: String?) {
        _selectedGoalTask.value = goal
        saveTimerStateToDb()
    }

    fun selectTimerDuration(minutes: Int, sessionType: String) {
        if (_timerStatus.value == TimerStatus.RUNNING) return
        _timerJobCancel()
        val secs = minutes * 60
        _totalDurationSeconds.value = secs
        _timeRemainingSeconds.value = secs
        targetDurationMs = secs * 1000L
        _selectedSessionType.value = sessionType
        _timerStatus.value = TimerStatus.IDLE
        saveTimerStateToDb()
    }

    fun setTimerSubject(subjectId: Long?) {
        _timerSubjectId.value = subjectId
        _timerChapterId.value = null // reset chapter when subject changes
        saveTimerStateToDb()
    }

    fun toggleStrictFocus(enabled: Boolean) {
        _isStrictFocus.value = enabled
        saveTimerStateToDb()
    }

    fun setAmbientSound(soundType: String) {
        _ambientSoundType.value = soundType
        ambientSoundManager.setSound(soundType)
    }

    fun setAmbientVolume(volume: Float) {
        _ambientVolume.value = volume
        ambientSoundManager.setVolume(volume)
    }

    fun startTimer() {
        val now = System.currentTimeMillis()
        if (_timerStatus.value == TimerStatus.IDLE || _timerStatus.value == TimerStatus.COMPLETED) {
            startTimeMs = now
            accumulatedPauseMs = 0L
        } else if (_timerStatus.value == TimerStatus.PAUSED) {
            accumulatedPauseMs += (now - lastPauseMs)
        }

        _timerStatus.value = TimerStatus.RUNNING
        startTimerInternal()
        saveTimerStateToDb()
    }

    private fun startTimerInternal() {
        if (_ambientSoundType.value != "OFF") {
            ambientSoundManager.setSound(_ambientSoundType.value)
        }

        _timerJobCancel()
        timerJob = viewModelScope.launch {
            while (_timerStatus.value == TimerStatus.RUNNING) {
                delay(500L)
                updateTimerTick()
            }
        }
    }

    private fun updateTimerTick() {
        if (_timerStatus.value != TimerStatus.RUNNING) return
        val now = System.currentTimeMillis()
        val elapsedMs = maxOf(0L, now - startTimeMs - accumulatedPauseMs)

        if (_timerMode.value == "STOPWATCH") {
            _timeRemainingSeconds.value = (elapsedMs / 1000L).toInt()
        } else {
            val remainMs = maxOf(0L, targetDurationMs - elapsedMs)
            val remainSecs = (remainMs / 1000L).toInt()
            _timeRemainingSeconds.value = remainSecs

            if (remainMs <= 0) {
                finishTimer(completedNormally = true)
            }
        }
    }

    fun pauseTimer() {
        if (_timerStatus.value == TimerStatus.RUNNING) {
            _timerStatus.value = TimerStatus.PAUSED
            lastPauseMs = System.currentTimeMillis()
            _timerJobCancel()
            ambientSoundManager.stop()
            saveTimerStateToDb()
        }
    }

    fun resumeTimer() {
        if (_timerStatus.value == TimerStatus.PAUSED) {
            startTimer()
        }
    }

    fun resetTimer() {
        _timerJobCancel()
        ambientSoundManager.stop()
        if (_timerMode.value == "STOPWATCH") {
            _timeRemainingSeconds.value = 0
        } else {
            _timeRemainingSeconds.value = _totalDurationSeconds.value
        }
        _timerStatus.value = TimerStatus.IDLE
        saveTimerStateToDb()
    }

    fun skipPhase() {
        _timerJobCancel()
        ambientSoundManager.stop()

        if (_timerMode.value == "POMODORO") {
            if (_pomodoroPhase.value == "WORK") {
                if (_currentCycle.value >= _targetCycles.value) {
                    _pomodoroPhase.value = "LONG_BREAK"
                    selectTimerDuration(15, "LONG_BREAK")
                } else {
                    _pomodoroPhase.value = "SHORT_BREAK"
                    selectTimerDuration(5, "SHORT_BREAK")
                }
            } else {
                if (_pomodoroPhase.value == "LONG_BREAK") {
                    _currentCycle.value = 1
                } else {
                    _currentCycle.value += 1
                }
                _pomodoroPhase.value = "WORK"
                selectTimerDuration(25, "POMODORO_FOCUS")
            }
        } else {
            resetTimer()
        }
        saveTimerStateToDb()
    }

    fun finishTimer(
        completedNormally: Boolean = false,
        rating: Int = 5,
        notes: String? = null,
        distractions: Int = 0,
        onGamificationUpdated: (com.example.data.local.FocusGamificationEntity) -> Unit = {}
    ) {
        val now = System.currentTimeMillis()
        val elapsedMs = if (_timerMode.value == "STOPWATCH") {
            _timeRemainingSeconds.value * 1000L
        } else {
            val totalMs = _totalDurationSeconds.value * 1000L
            if (completedNormally) totalMs else (totalMs - (_timeRemainingSeconds.value * 1000L))
        }

        val elapsedSecs = (elapsedMs / 1000L).toInt()

        _timerJobCancel()
        ambientSoundManager.stop()
        _timerStatus.value = TimerStatus.COMPLETED
        saveTimerStateToDb()

        if (elapsedSecs >= 10) { // Log meaningful sessions
            viewModelScope.launch {
                val start = if (startTimeMs > 0) startTimeMs else (now - elapsedMs)
                val id = repository.logStudySession(
                    subjectId = _timerSubjectId.value,
                    chapterId = _timerChapterId.value,
                    taskId = null,
                    startedAt = start,
                    endedAt = now,
                    elapsedSeconds = elapsedSecs,
                    status = "COMPLETED",
                    sessionType = _selectedSessionType.value
                )
                lastRecordedSessionId = id

                val updatedGamification = repository.awardFocusSession(
                    elapsedSeconds = elapsedSecs,
                    isCompleted = completedNormally
                )
                onGamificationUpdated(updatedGamification)

                // Notify completion
                val mins = elapsedSecs / 60
                val subjectName = subjects.value.find { it.id == _timerSubjectId.value }?.name ?: "Study"
                val title = "Focus Session Completed!"
                val msg = "Awesome! You focused on $subjectName for $mins minutes and earned XP!"

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

    override fun onCleared() {
        super.onCleared()
        ambientSoundManager.stop()
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
