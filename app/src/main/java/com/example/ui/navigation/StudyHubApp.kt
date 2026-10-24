package com.example.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppSettingsEntity
import com.example.ui.StudyHubViewModel
import com.example.ui.components.NavDestination
import com.example.ui.components.StudyHubBottomNav
import com.example.ui.components.StudyHubTopBar
import com.example.ui.screens.AdmissionCalendarScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressAnalyticsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SyllabusScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.StudyHubTheme
import com.example.ui.theme.ThemeMode

import com.example.notifications.StudyHubNotificationManager
import com.example.ui.components.InAppNotificationBanner
import java.util.Locale

enum class AppScreen {
    SPLASH,
    ONBOARDING,
    MAIN,
    PROFILE,
    NOTIFICATIONS,
    ADMISSION_CALENDAR,
    PROGRESS_ANALYTICS,
    NOTES,
    SETTINGS
}

@Composable
fun StudyHubApp(
    viewModel: StudyHubViewModel,
    initialTargetRoute: String? = null
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val admissionTargets by viewModel.admissionTargets.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()

    val todayStudySeconds by viewModel.todayStudySeconds.collectAsStateWithLifecycle()
    val overallSyllabusProgress by viewModel.overallSyllabusProgress.collectAsStateWithLifecycle()

    // Timer state
    val timerStatus by viewModel.timerStatus.collectAsStateWithLifecycle()
    val timeRemainingSeconds by viewModel.timeRemainingSeconds.collectAsStateWithLifecycle()
    val totalDurationSeconds by viewModel.totalDurationSeconds.collectAsStateWithLifecycle()
    val selectedSessionType by viewModel.selectedSessionType.collectAsStateWithLifecycle()
    val timerSubjectId by viewModel.timerSubjectId.collectAsStateWithLifecycle()

    var currentScreen by remember { mutableStateOf(AppScreen.SPLASH) }
    var currentNavDestination by remember { mutableStateOf(NavDestination.HOME) }

    fun navigateToRoute(route: String?) {
        if (route.isNullOrBlank()) return
        when (route.lowercase(Locale.ROOT)) {
            "focus" -> {
                currentScreen = AppScreen.MAIN
                currentNavDestination = NavDestination.FOCUS
            }
            "syllabus" -> {
                currentScreen = AppScreen.MAIN
                currentNavDestination = NavDestination.SYLLABUS
            }
            "calendar" -> {
                currentScreen = AppScreen.MAIN
                currentNavDestination = NavDestination.CALENDAR
            }
            "admission" -> {
                currentScreen = AppScreen.ADMISSION_CALENDAR
            }
            "notes" -> {
                currentScreen = AppScreen.NOTES
            }
            "tools" -> {
                currentScreen = AppScreen.MAIN
                currentNavDestination = NavDestination.TOOLS
            }
            "notifications" -> {
                currentScreen = AppScreen.NOTIFICATIONS
            }
            "profile" -> {
                currentScreen = AppScreen.PROFILE
            }
            "analytics" -> {
                currentScreen = AppScreen.PROGRESS_ANALYTICS
            }
            "settings" -> {
                currentScreen = AppScreen.SETTINGS
            }
            else -> {
                currentScreen = AppScreen.MAIN
                currentNavDestination = NavDestination.HOME
            }
        }
    }

    LaunchedEffect(initialTargetRoute) {
        if (!initialTargetRoute.isNullOrBlank()) {
            navigateToRoute(initialTargetRoute)
        }
    }

    // Quick action trigger flags
    var requestAddSubjectOnSyllabus by remember { mutableStateOf(false) }
    var requestAddAdmissionOnCalendar by remember { mutableStateOf(false) }

    // Notification Permission Rationale Dialog State
    var showNotificationRationaleDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.notificationManager.postNotification(
                id = 999,
                channelId = "studyhub_study",
                title = "StudyHub Notifications Active",
                message = "You will now receive timely study reminders and admission deadline alerts."
            )
        }
    }

    // Determine current ThemeMode
    val currentThemeMode = when (appSettings?.themeMode) {
        "DARK" -> ThemeMode.DARK
        "SYSTEM" -> ThemeMode.SYSTEM
        else -> ThemeMode.LIGHT
    }

    // Total hours focused
    val totalHoursFocused = sessions.filter { it.status == "COMPLETED" }.sumOf { it.elapsedSeconds } / 3600f
    val completedChaptersCount = chapters.count { it.status == "COMPLETED" }

    val isProfileLoaded by viewModel.isProfileLoaded.collectAsStateWithLifecycle()

    var isSubjectDetailOpen by remember { mutableStateOf(false) }

    LaunchedEffect(currentNavDestination) {
        isSubjectDetailOpen = false
    }

    StudyHubTheme(themeMode = currentThemeMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
                when (screen) {
                AppScreen.SPLASH -> {
                    SplashScreen(
                        isReady = isProfileLoaded,
                        onSplashFinished = {
                            if (userProfile?.isOnboardingCompleted == true) {
                                currentScreen = AppScreen.MAIN
                            } else {
                                currentScreen = AppScreen.ONBOARDING
                            }
                        }
                    )
                }

                AppScreen.ONBOARDING -> {
                    OnboardingScreen(
                        onComplete = { name, avatarId, level, group, year, track, goalH, time ->
                            viewModel.saveOnboardingProfile(
                                name = name,
                                avatarId = avatarId,
                                academicLevel = level,
                                group = group,
                                targetYear = year,
                                admissionTrack = track,
                                dailyGoalHours = goalH,
                                preferredTime = time,
                                onSuccess = {
                                    currentScreen = AppScreen.MAIN
                                    currentNavDestination = NavDestination.HOME
                                    // Check notification permission nicely
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !viewModel.notificationManager.hasNotificationPermission()
                                    ) {
                                        showNotificationRationaleDialog = true
                                    }
                                }
                            )
                        }
                    )
                }

                AppScreen.MAIN -> {
                    Scaffold(
                        topBar = {
                            if (!isSubjectDetailOpen || currentNavDestination != NavDestination.SYLLABUS) {
                                StudyHubTopBar(
                                    unreadNotificationCount = unreadNotificationsCount,
                                    onNotificationClick = { currentScreen = AppScreen.NOTIFICATIONS }
                                )
                            }
                        },
                        bottomBar = {
                            StudyHubBottomNav(
                                currentDestination = currentNavDestination,
                                onNavigate = { destination ->
                                    currentNavDestination = destination
                                }
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentNavDestination) {
                                NavDestination.HOME -> {
                                    HomeScreen(
                                        userProfile = userProfile,
                                        todayStudySeconds = todayStudySeconds,
                                        overallSyllabusProgress = overallSyllabusProgress,
                                        subjects = subjects,
                                        chapters = chapters,
                                        tasks = tasks,
                                        admissionTargets = admissionTargets,
                                        onAddSubjectClick = {
                                            requestAddSubjectOnSyllabus = true
                                            currentNavDestination = NavDestination.SYLLABUS
                                        },
                                        onStartFocusClick = {
                                            currentNavDestination = NavDestination.FOCUS
                                        },
                                        onTodayPlanClick = {
                                            currentNavDestination = NavDestination.CALENDAR
                                        },
                                        onAddAdmissionClick = {
                                            requestAddAdmissionOnCalendar = true
                                            currentScreen = AppScreen.ADMISSION_CALENDAR
                                        },
                                        onOpenAdmissionCalendar = {
                                            currentScreen = AppScreen.ADMISSION_CALENDAR
                                        },
                                        onSubjectClick = {
                                            currentNavDestination = NavDestination.SYLLABUS
                                        },
                                        onAvatarClick = {
                                            currentScreen = AppScreen.PROFILE
                                        }
                                    )
                                }

                                NavDestination.SYLLABUS -> {
                                    SyllabusScreen(
                                        subjects = subjects,
                                        chapters = chapters,
                                        onAddSubject = { name, bangla, color, icon, cat, count ->
                                            viewModel.createSubject(name, bangla, color, icon, cat, count) {}
                                        },
                                        onDeleteSubject = { viewModel.deleteSubject(it) },
                                        onUpdateChapterStatus = { chap, status ->
                                            viewModel.updateChapterStatus(chap, status)
                                        },
                                        onAddChapter = { subId, title ->
                                            viewModel.addChapter(subId, title)
                                        },
                                        onDeleteChapter = { viewModel.deleteChapter(it) },
                                        initialAddSubjectRequested = requestAddSubjectOnSyllabus,
                                        onAddSubjectDismissed = { requestAddSubjectOnSyllabus = false },
                                        onSubjectDetailStateChanged = { isOpen ->
                                            isSubjectDetailOpen = isOpen
                                        }
                                    )
                                }

                                NavDestination.FOCUS -> {
                                    FocusTimerScreen(
                                        timerStatus = timerStatus,
                                        timeRemainingSeconds = timeRemainingSeconds,
                                        totalDurationSeconds = totalDurationSeconds,
                                        selectedSessionType = selectedSessionType,
                                        selectedSubjectId = timerSubjectId,
                                        subjects = subjects,
                                        sessions = sessions,
                                        onSelectDuration = { mins, type ->
                                            viewModel.selectTimerDuration(mins, type)
                                        },
                                        onSelectSubject = { subId ->
                                            viewModel.setTimerSubject(subId)
                                        },
                                        onStartTimer = { viewModel.startTimer() },
                                        onPauseTimer = { viewModel.pauseTimer() },
                                        onResumeTimer = { viewModel.resumeTimer() },
                                        onResetTimer = { viewModel.resetTimer() },
                                        onFinishTimer = { viewModel.finishTimer() }
                                    )
                                }

                                NavDestination.CALENDAR -> {
                                    CalendarScreen(
                                        tasks = tasks,
                                        admissionTargets = admissionTargets,
                                        subjects = subjects,
                                        onCreateTask = { title, date, time, dur, subId, priority, notesText ->
                                            viewModel.createTask(title, date, time, dur, subId, priority, notesText) {}
                                        },
                                        onToggleTaskStatus = { viewModel.toggleTaskStatus(it) },
                                        onDeleteTask = { viewModel.deleteTask(it) }
                                    )
                                }

                                NavDestination.TOOLS -> {
                                    ToolsScreen(
                                        onOpenAdmissionCalendar = { currentScreen = AppScreen.ADMISSION_CALENDAR },
                                        onOpenAnalytics = { currentScreen = AppScreen.PROGRESS_ANALYTICS },
                                        onOpenNotes = { currentScreen = AppScreen.NOTES }
                                    )
                                }
                            }
                        }
                    }
                }

                AppScreen.NOTIFICATIONS -> {
                    BackHandler { currentScreen = AppScreen.MAIN }
                    NotificationsScreen(
                        notifications = notifications,
                        onMarkAllRead = { viewModel.markAllNotificationsAsRead() },
                        onDeleteNotification = { viewModel.deleteNotification(it) },
                        onNavigateRoute = { route -> navigateToRoute(route) },
                        onBack = { currentScreen = AppScreen.MAIN }
                    )
                }

                AppScreen.ADMISSION_CALENDAR -> {
                    BackHandler { currentScreen = AppScreen.MAIN }
                    AdmissionCalendarScreen(
                        targets = admissionTargets,
                        tasks = tasks,
                        onSaveTarget = { viewModel.saveAdmissionTarget(it) {} },
                        onDeleteTarget = { viewModel.deleteAdmissionTarget(it) },
                        onCreateTask = { title, date, targetId, priority, notes ->
                            viewModel.createTargetTask(title, date, targetId, priority, notes)
                        },
                        onToggleTaskStatus = { viewModel.toggleTaskStatus(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onBack = { currentScreen = AppScreen.MAIN },
                        initialAddRequested = requestAddAdmissionOnCalendar,
                        onAddDismissed = { requestAddAdmissionOnCalendar = false }
                    )
                }

                AppScreen.PROGRESS_ANALYTICS -> {
                    BackHandler { currentScreen = AppScreen.MAIN }
                    ProgressAnalyticsScreen(
                        userProfile = userProfile,
                        sessions = sessions,
                        subjects = subjects,
                        chapters = chapters,
                        onBack = { currentScreen = AppScreen.MAIN },
                        onStartFocus = {
                            currentScreen = AppScreen.MAIN
                            currentNavDestination = NavDestination.FOCUS
                        }
                    )
                }

                AppScreen.PROFILE -> {
                    BackHandler { currentScreen = AppScreen.MAIN }
                    ProfileScreen(
                        userProfile = userProfile,
                        completedChaptersCount = completedChaptersCount,
                        totalStudyHours = totalHoursFocused,
                        onOpenSettings = { currentScreen = AppScreen.SETTINGS },
                        onOpenAdmissionCalendar = { currentScreen = AppScreen.ADMISSION_CALENDAR },
                        onOpenAnalytics = { currentScreen = AppScreen.PROGRESS_ANALYTICS },
                        onOpenNotes = { currentScreen = AppScreen.NOTES },
                        onEditProfile = { currentScreen = AppScreen.ONBOARDING },
                        onBack = { currentScreen = AppScreen.MAIN }
                    )
                }

                AppScreen.NOTES -> {
                    BackHandler { currentScreen = AppScreen.MAIN }
                    NotesScreen(
                        notes = notes,
                        subjects = subjects,
                        onSaveNote = { viewModel.saveNote(it) {} },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onBack = { currentScreen = AppScreen.MAIN }
                    )
                }

                AppScreen.SETTINGS -> {
                    BackHandler { currentScreen = AppScreen.MAIN }
                    SettingsScreen(
                        appSettings = appSettings,
                        currentThemeMode = currentThemeMode,
                        onThemeModeChanged = { viewModel.updateThemeMode(it) },
                        onSettingsUpdated = { viewModel.updateSettings(it) },
                        onOpenNotifications = { currentScreen = AppScreen.NOTIFICATIONS },
                        onMasterToggleChanged = { viewModel.updateMasterNotificationToggle(it) },
                        onResetOnboarding = {
                            currentScreen = AppScreen.ONBOARDING
                        },
                        onResetAllData = {
                            viewModel.resetAllData {
                                currentScreen = AppScreen.ONBOARDING
                            }
                        },
                        onBack = { currentScreen = AppScreen.MAIN }
                    )
                }
            }

            InAppNotificationBanner(
                onNavigateRoute = { route ->
                    navigateToRoute(route)
                }
            )
        }

        // Notification rationale dialog per PRD Section 8.13 & Appendix B
        if (showNotificationRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showNotificationRationaleDialog = false },
                title = { Text("Stay on Track", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Enable reminders so important study tasks and admission exam dates don't slip by.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNotificationRationaleDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                    ) {
                        Text("Enable Reminders")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNotificationRationaleDialog = false }) {
                        Text("Maybe Later")
                    }
                }
            )
        }
        }
    }
}
