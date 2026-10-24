package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Long = 1L,
    val displayName: String = "",
    val avatarId: String = "avatar_1",
    val academicLevel: String = "HSC", // "SSC", "HSC", "Admission"
    val academicGroup: String = "Science", // "Science", "Humanities", "Business Studies", "Class 9-10"
    val targetYear: String = "2025",
    val admissionTrack: String? = null, // "General University", "Medical", "Engineering", "Other"
    val dailyGoalMinutes: Int = 240, // 4 hours default
    val preferredStudyTime: String = "Morning", // "Morning", "Afternoon", "Evening", "Night"
    val isOnboardingCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val banglaName: String? = null,
    val colorHex: String = "#0796A3",
    val iconKey: String = "calculator", // "calculator", "book", "atom", "flask", "stethoscope", "compass", "pen", "palette"
    val category: String = "Compulsory", // "Compulsory", "Optional"
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val subjectId: Long,
    val title: String,
    val sortOrder: Int = 0,
    val status: String = "NOT_STARTED", // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:mm
    val durationMinutes: Int = 30,
    val subjectId: Long? = null,
    val chapterId: Long? = null,
    val targetId: Long? = null,
    val status: String = "PENDING", // "PENDING", "COMPLETED", "CANCELLED"
    val priority: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH"
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val subjectId: Long? = null,
    val chapterId: Long? = null,
    val taskId: Long? = null,
    val startedAt: Long,
    val endedAt: Long,
    val elapsedSeconds: Int,
    val status: String = "COMPLETED", // "COMPLETED", "ABANDONED"
    val sessionType: String = "POMODORO_FOCUS" // "POMODORO_FOCUS", "SHORT_BREAK", "LONG_BREAK", "CUSTOM"
)

@Entity(tableName = "admission_targets")
data class AdmissionTargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val institutionName: String,
    val category: String = "General University", // "General University", "Medical", "Engineering", "Other"
    val programUnit: String? = null, // "A Unit", "Engineering & Architecture", "MBBS"
    val cycleLabel: String = "2025",
    val applicationOpenDate: String? = null, // YYYY-MM-DD
    val applicationDeadline: String, // YYYY-MM-DD
    val admitCardDate: String? = null,
    val examDate: String? = null,
    val resultDate: String? = null,
    val applicationFee: String? = null,
    val applicationStatus: String = "PLANNING", // "INTERESTED", "PLANNING", "APPLICATION_OPEN", "IN_PROGRESS", "APPLIED", "ADMIT_CARD_AVAILABLE", "EXAM_COMPLETED", "RESULT_PUBLISHED", "NOT_APPLYING"
    val prepProgress: Int = 0, // 0 - 100
    val notes: String? = null,
    val officialUrl: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val subjectId: Long? = null,
    val title: String,
    val body: String,
    val tags: String? = null,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_items")
data class NotificationItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val body: String,
    val category: String = "STUDY", // "STUDY", "ADMISSION", "DAILY_GOAL", "SYSTEM"
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val actionRoute: String? = null
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeMode: String = "LIGHT", // "LIGHT", "DARK", "SYSTEM"
    val studyRemindersEnabled: Boolean = true, // Master Notifications ON/OFF
    val soundEnabled: Boolean = true,
    val quietHoursStartHour: Int = 22, // 10 PM
    val quietHoursEndHour: Int = 7,    // 7 AM
    val lastNotificationTime: Long = 0L,
    val consecutiveIgnoredCount: Int = 0
)
