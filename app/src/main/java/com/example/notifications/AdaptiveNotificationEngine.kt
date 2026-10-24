package com.example.notifications

import com.example.data.local.AdmissionTargetEntity
import com.example.data.local.AppSettingsEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.NotificationItemEntity
import com.example.data.local.StudyNoteEntity
import com.example.data.local.StudyTaskEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.UserProfile
import java.util.Calendar
import kotlin.random.Random

object AdaptiveNotificationEngine {

    fun calculateScore(
        template: NotificationTemplate,
        userProfile: UserProfile?,
        subjects: List<SubjectEntity>,
        chapters: List<ChapterEntity>,
        tasks: List<StudyTaskEntity>,
        admissionTargets: List<AdmissionTargetEntity>,
        notes: List<StudyNoteEntity>,
        recentNotifications: List<NotificationItemEntity>,
        currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): Double {
        var score = 50.0 // Base score

        // 1. Priority Weight
        score += template.priority * 10.0

        // 2. Admission & Exam Urgency Boost
        if (template.category in listOf(NotificationCategory.EXAM_ALERTS, NotificationCategory.ADMISSION_READINESS)) {
            val hasUpcomingDeadline = admissionTargets.any { target ->
                !target.isArchived && isWithinDays(target.applicationDeadline, 14)
            }
            if (hasUpcomingDeadline) {
                score += 40.0 // Highest priority boost for upcoming deadlines
            }
        }

        // 3. Task & Plan Relevance
        if (template.category in listOf(NotificationCategory.TASKS_PLANNING, NotificationCategory.EXAM_STRATEGY)) {
            val pendingTasksToday = tasks.count { it.status == "PENDING" }
            if (pendingTasksToday > 0) {
                score += 25.0
            }
        }

        // 4. Syllabus & Chapter Progress Boost
        if (template.category in listOf(NotificationCategory.SYLLABUS_CHAPTERS, NotificationCategory.CHAPTER_MASTERY, NotificationCategory.REVISION_SCIENCE)) {
            val uncompletedChapters = chapters.count { it.status != "COMPLETED" }
            if (uncompletedChapters > 0) {
                score += 20.0
            }
        }

        // 5. Time-of-day Alignment with User's Preferred Study Time
        val preferredTime = userProfile?.preferredStudyTime ?: "Morning"
        val timeMatches = when (preferredTime) {
            "Morning" -> currentHour in 6..11
            "Afternoon" -> currentHour in 12..16
            "Evening" -> currentHour in 17..20
            "Night" -> currentHour in 21..23 || currentHour in 0..2
            else -> false
        }
        if (timeMatches) {
            score += 15.0
        }

        // 6. Tone Rotation & Freshness Bonus
        val lastCategory = recentNotifications.firstOrNull()?.category
        if (lastCategory == template.category.name) {
            score -= 25.0 // Tone rotation penalty
        }

        val shownRecently = recentNotifications.take(30).any { it.title == template.title }
        if (!shownRecently) {
            score += 20.0
        } else {
            score -= 40.0 // Repetition penalty
        }

        return score.coerceAtLeast(1.0)
    }

    fun selectBestCandidate(
        templates: List<NotificationTemplate>,
        userProfile: UserProfile?,
        subjects: List<SubjectEntity>,
        chapters: List<ChapterEntity>,
        tasks: List<StudyTaskEntity>,
        admissionTargets: List<AdmissionTargetEntity>,
        notes: List<StudyNoteEntity>,
        recentNotifications: List<NotificationItemEntity>,
        currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    ): NotificationTemplate? {
        if (templates.isEmpty()) return null

        val scoredList = templates.map { template ->
            val score = calculateScore(
                template = template,
                userProfile = userProfile,
                subjects = subjects,
                chapters = chapters,
                tasks = tasks,
                admissionTargets = admissionTargets,
                notes = notes,
                recentNotifications = recentNotifications,
                currentHour = currentHour
            )
            template to score
        }

        val totalScore = scoredList.sumOf { it.second }
        if (totalScore <= 0) return templates.randomOrNull()

        var randomVal = Random.nextDouble() * totalScore
        for ((template, score) in scoredList) {
            randomVal -= score
            if (randomVal <= 0) {
                return template
            }
        }

        return scoredList.maxByOrNull { it.second }?.first
    }

    private fun isWithinDays(dateStr: String?, daysThreshold: Int): Boolean {
        if (dateStr.isNullOrBlank()) return false
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val cal = Calendar.getInstance()
                val targetCal = Calendar.getInstance().apply {
                    set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 23, 59)
                }
                val diffMs = targetCal.timeInMillis - cal.timeInMillis
                val diffDays = diffMs / (1000 * 60 * 60 * 24)
                diffDays in 0..daysThreshold
            } else false
        } catch (e: Exception) {
            false
        }
    }
}
