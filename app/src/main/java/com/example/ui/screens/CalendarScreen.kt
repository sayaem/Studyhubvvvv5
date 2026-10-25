package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AdmissionTargetEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.FocusGamificationEntity
import com.example.data.local.StudySessionEntity
import com.example.data.local.StudyTaskEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.UserProfile
import com.example.ui.components.getSubjectIcon
import com.example.ui.components.parseColorHex
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    tasks: List<StudyTaskEntity>,
    admissionTargets: List<AdmissionTargetEntity>,
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity> = emptyList(),
    sessions: List<StudySessionEntity> = emptyList(),
    userProfile: UserProfile? = null,
    gamification: FocusGamificationEntity? = null,
    onCreateTask: (String, String, String?, Int, Long?, String, String?) -> Unit,
    onCreatePlan: ((title: String, date: String, time: String?, duration: Int, subjectId: Long?, chapterId: Long?, priority: String, planType: String, notes: String?) -> Unit)? = null,
    onUpdatePlan: ((StudyTaskEntity) -> Unit)? = null,
    onToggleTaskStatus: (StudyTaskEntity) -> Unit,
    onDeleteTask: (StudyTaskEntity) -> Unit,
    onLaunchInFocus: ((subjectId: Long?, chapterId: Long?, taskTitle: String, durationMins: Int) -> Unit)? = null,
    onNavigateToNotifications: (() -> Unit)? = null,
    onNavigateToProfile: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayHeaderFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }
    val dayNumFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val weekDayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    val todayCalendar = remember { Calendar.getInstance() }
    val todayDateStr = remember { dateFormat.format(todayCalendar.time) }

    // Selected date state - initialized to today
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val selectedDateStr = dateFormat.format(selectedCalendar.time)

    // Current displayed week start (Monday)
    var currentWeekStart by remember {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        mutableStateOf(cal)
    }

    // Modal state for Add / Edit Plan
    var showPlanSheet by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<StudyTaskEntity?>(null) }
    var planToDelete by remember { mutableStateOf<StudyTaskEntity?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Calculate 7 consecutive days for the current week starting from currentWeekStart
    val weekDays = remember(currentWeekStart) {
        (0..6).map { dayOffset ->
            val dCal = currentWeekStart.clone() as Calendar
            dCal.add(Calendar.DAY_OF_MONTH, dayOffset)
            dCal
        }
    }

    // Plans and events for the selected date
    val dayTasks = tasks.filter { it.date == selectedDateStr }
        .sortedWith(compareBy(
            { it.time ?: "99:99" },
            { it.id }
        ))

    val daySessions = sessions.filter {
        val sDate = dateFormat.format(Date(it.startedAt))
        sDate == selectedDateStr && it.status == "COMPLETED"
    }

    // Daily summary calculations
    val totalTasksCount = dayTasks.size
    val completedTasksCount = dayTasks.count { it.status == "COMPLETED" }
    val completedStudyMinutes = daySessions.sumOf { it.elapsedSeconds } / 60
    val dailyGoalMinutes = userProfile?.dailyGoalMinutes ?: 240
    val currentStreak = gamification?.currentStreakDays ?: 0

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingPlan = null
                    showPlanSheet = true
                },
                containerColor = BrandBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 4.dp)
                    .testTag("calendar_add_plan_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Plan", modifier = Modifier.size(28.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ==========================================
            // 1. TOP BAR HEADER
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = "StudyHub",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "StudyHub",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                        Text(
                            text = "Plan • Study • Achieve",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onNavigateToNotifications != null) {
                        IconButton(
                            onClick = onNavigateToNotifications,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (onNavigateToProfile != null) {
                        val initials = userProfile?.displayName?.split(" ")
                            ?.mapNotNull { it.firstOrNull()?.toString() }
                            ?.take(2)?.joinToString("")?.uppercase() ?: "SH"

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BrandBlue)
                                .clickable { onNavigateToProfile() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. HORIZONTAL WEEKLY DATE SELECTOR
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                weekDays.forEach { dayCal ->
                    val dateStr = dateFormat.format(dayCal.time)
                    val isSelected = dateStr == selectedDateStr
                    val isToday = dateStr == todayDateStr
                    val dayName = weekDayFormat.format(dayCal.time)
                    val dayNum = dayNumFormat.format(dayCal.time)

                    // Check if date has scheduled items
                    val hasTasks = tasks.any { it.date == dateStr }
                    val hasCompleted = tasks.any { it.date == dateStr && it.status == "COMPLETED" }

                    val dotColor = when {
                        isSelected -> Color.White
                        hasCompleted -> StatusSuccess
                        hasTasks -> BrandTeal
                        else -> Color.Transparent
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) BrandBlue else Color.Transparent)
                            .clickable {
                                selectedCalendar = dayCal.clone() as Calendar
                            }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = dayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayNum,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ==========================================
            // 3. DATE NAVIGATION & TODAY JUMP BAR
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayHeaderFormat.format(selectedCalendar.time),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Previous Week
                    IconButton(
                        onClick = {
                            val newWeek = currentWeekStart.clone() as Calendar
                            newWeek.add(Calendar.DAY_OF_MONTH, -7)
                            currentWeekStart = newWeek

                            val newSel = selectedCalendar.clone() as Calendar
                            newSel.add(Calendar.DAY_OF_MONTH, -7)
                            selectedCalendar = newSel
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Week", modifier = Modifier.size(20.dp))
                    }

                    // Next Week
                    IconButton(
                        onClick = {
                            val newWeek = currentWeekStart.clone() as Calendar
                            newWeek.add(Calendar.DAY_OF_MONTH, 7)
                            currentWeekStart = newWeek

                            val newSel = selectedCalendar.clone() as Calendar
                            newSel.add(Calendar.DAY_OF_MONTH, 7)
                            selectedCalendar = newSel
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Week", modifier = Modifier.size(20.dp))
                    }

                    // Return to Today Button
                    Surface(
                        onClick = {
                            selectedCalendar = Calendar.getInstance()
                            val cur = Calendar.getInstance()
                            cur.firstDayOfWeek = Calendar.MONDAY
                            cur.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                            currentWeekStart = cur
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Today", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // 4. COMPACT DAILY SUMMARY (Row of 3 cards)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: Today's Progress
                val progressRatio = if (totalTasksCount > 0) completedTasksCount.toFloat() / totalTasksCount else 0f
                val progressTrackColor = MaterialTheme.colorScheme.surfaceVariant
                Card(
                    modifier = Modifier
                        .weight(1.2f)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 4.dp.toPx()
                                drawArc(
                                    color = progressTrackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth)
                                )
                                drawArc(
                                    color = BrandTeal,
                                    startAngle = -90f,
                                    sweepAngle = progressRatio * 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "$completedTasksCount/$totalTasksCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Today's Progress", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(
                                text = "$completedTasksCount tasks completed",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Card 2: Study Time
                val hours = completedStudyMinutes / 60
                val mins = completedStudyMinutes % 60
                val studyTimeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                val targetHours = dailyGoalMinutes / 60

                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(BrandBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Study Time", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("$studyTimeStr / ${targetHours}h", fontSize = 10.sp, color = TextSecondary, maxLines = 1)
                        }
                    }
                }

                // Card 3: Streak
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF97316).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Streak", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("$currentStreak days", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ==========================================
            // 5. VERTICAL TIME-BASED TIMELINE
            // ==========================================
            if (dayTasks.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(BrandBlue.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No Study Plans For This Day",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Build your schedule by adding study sessions, revision blocks, homework, or exam goals.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    editingPlan = null
                                    showPlanSheet = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add New Plan", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dayTasks, key = { it.id }) { task ->
                        val sub = subjects.find { it.id == task.subjectId }
                        val chap = chapters.find { it.id == task.chapterId }
                        val isCompleted = task.status == "COMPLETED"

                        // Determine theme color & icon based on subject / planType
                        val itemColor = when (task.planType) {
                            "REVISION" -> Color(0xFF0D9488) // Cyan / Teal
                            "EXAM" -> Color(0xFF8B5CF6) // Purple
                            "ASSIGNMENT" -> Color(0xFFEC4899) // Pink / Rose
                            "BREAK" -> Color(0xFFF59E0B) // Amber
                            "PERSONAL" -> Color(0xFF3B82F6) // Blue
                            else -> sub?.let { parseColorHex(it.colorHex) } ?: BrandTeal
                        }

                        val itemIcon: ImageVector = when (task.planType) {
                            "REVISION" -> Icons.Default.Autorenew
                            "EXAM" -> Icons.Default.Star
                            "ASSIGNMENT" -> Icons.Default.Assignment
                            "BREAK" -> Icons.Default.Coffee
                            "PERSONAL" -> Icons.Default.Event
                            else -> sub?.let { getSubjectIcon(it.iconKey) } ?: Icons.Default.Book
                        }

                        val formattedTimeRange = formatPlanTimeRange(task.time, task.durationMinutes)
                        val displayStartTime = formatStartTimeOnly(task.time)

                        var menuExpanded by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            // Left Column: Time label & Vertical Timeline Connector
                            Column(
                                modifier = Modifier
                                    .width(70.dp)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = displayStartTime,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // Colored anchor dot
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(itemColor)
                                        )
                                        // Connecting line
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .weight(1f)
                                                .background(itemColor.copy(alpha = 0.3f))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Right Column: Study / Task Card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, itemColor.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = itemColor.copy(alpha = 0.08f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left Icon Box
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(itemColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = itemIcon,
                                            contentDescription = null,
                                            tint = itemColor,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Middle Text Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (task.planType == "STUDY" && sub != null) sub.name else task.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        val subtitle = when {
                                            chap != null -> chap.title
                                            task.planType == "STUDY" && sub != null -> task.title
                                            !task.notes.isNullOrBlank() -> task.notes
                                            else -> task.priority.lowercase().replaceFirstChar { it.uppercase() } + " Priority"
                                        }

                                        Text(
                                            text = subtitle,
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.AccessTime,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = formattedTimeRange,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Right: Status badge & Overflow Menu
                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            onClick = { onToggleTaskStatus(task) },
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isCompleted) StatusSuccess.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.1f),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isCompleted) StatusSuccess else MaterialTheme.colorScheme.outline
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.AccessTime,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = if (isCompleted) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isCompleted) "Completed" else "Upcoming",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCompleted) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Box {
                                            IconButton(
                                                onClick = { menuExpanded = true },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "Options",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = menuExpanded,
                                                onDismissRequest = { menuExpanded = false }
                                            ) {
                                                if (onLaunchInFocus != null && task.planType != "BREAK") {
                                                    DropdownMenuItem(
                                                        text = { Text("Start in Focus Timer") },
                                                        leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = BrandTeal) },
                                                        onClick = {
                                                            menuExpanded = false
                                                            onLaunchInFocus(task.subjectId, task.chapterId, task.title, task.durationMinutes)
                                                        }
                                                    )
                                                }
                                                DropdownMenuItem(
                                                    text = { Text(if (isCompleted) "Mark as Upcoming" else "Mark as Completed") },
                                                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                                    onClick = {
                                                        menuExpanded = false
                                                        onToggleTaskStatus(task)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Edit Plan") },
                                                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                                    onClick = {
                                                        menuExpanded = false
                                                        editingPlan = task
                                                        showPlanSheet = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Delete Plan", color = MaterialTheme.colorScheme.error) },
                                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                                    onClick = {
                                                        menuExpanded = false
                                                        planToDelete = task
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom "+ Add New Plan" Dashed Outline Card
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = BrandBlue.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    editingPlan = null
                                    showPlanSheet = true
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(BrandBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Add New Plan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Study session, exam, assignment, or other task", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    // ==========================================
    // 6. ADD / EDIT PLAN BOTTOM SHEET
    // ==========================================
    if (showPlanSheet) {
        val currentPlan = editingPlan
        var planType by remember { mutableStateOf(currentPlan?.planType ?: "STUDY") }
        var titleInput by remember { mutableStateOf(currentPlan?.title ?: "") }
        var selectedSubjectId by remember { mutableStateOf(currentPlan?.subjectId) }
        var selectedChapterId by remember { mutableStateOf(currentPlan?.chapterId) }
        var dateInput by remember { mutableStateOf(currentPlan?.date ?: selectedDateStr) }
        var timeInput by remember { mutableStateOf(currentPlan?.time ?: "08:00") }
        var durationMinutes by remember { mutableIntStateOf(currentPlan?.durationMinutes ?: 60) }
        var priorityInput by remember { mutableStateOf(currentPlan?.priority ?: "MEDIUM") }
        var notesInput by remember { mutableStateOf(currentPlan?.notes ?: "") }

        val chaptersForSubject = chapters.filter { it.subjectId == selectedSubjectId }

        // Overlap detection
        val conflictingPlan = remember(dateInput, timeInput, durationMinutes, currentPlan) {
            val newStart = parseTimeToMinutes(timeInput)
            val newEnd = newStart + durationMinutes
            tasks.find { existing ->
                existing.id != currentPlan?.id && existing.date == dateInput && existing.time != null && run {
                    val exStart = parseTimeToMinutes(existing.time)
                    val exEnd = exStart + existing.durationMinutes
                    maxOf(newStart, exStart) < minOf(newEnd, exEnd)
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showPlanSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (currentPlan == null) "Schedule Study Plan" else "Edit Study Plan",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Plan Type Selector
                Text("Plan Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        Triple("STUDY", "Study", Icons.Default.Book),
                        Triple("REVISION", "Revision", Icons.Default.Autorenew),
                        Triple("EXAM", "Exam", Icons.Default.Star),
                        Triple("ASSIGNMENT", "Assignment", Icons.Default.Assignment),
                        Triple("BREAK", "Break", Icons.Default.Coffee),
                        Triple("PERSONAL", "Personal", Icons.Default.Event)
                    )
                    items(types) { (key, label, icon) ->
                        val isSel = planType == key
                        Surface(
                            onClick = {
                                planType = key
                                if (titleInput.isBlank() && key != "STUDY") {
                                    titleInput = label
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSel) BrandBlue else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Subject Selection
                if (subjects.isNotEmpty() && planType in listOf("STUDY", "REVISION", "EXAM", "ASSIGNMENT")) {
                    Text("Associated Subject", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val isSel = selectedSubjectId == null
                            Surface(
                                onClick = {
                                    selectedSubjectId = null
                                    selectedChapterId = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) BrandTeal else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "General",
                                    fontSize = 11.sp,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(subjects) { sub ->
                            val isSel = selectedSubjectId == sub.id
                            val subColor = parseColorHex(sub.colorHex)
                            Surface(
                                onClick = {
                                    selectedSubjectId = sub.id
                                    selectedChapterId = null
                                    if (titleInput.isBlank()) {
                                        titleInput = sub.name
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) subColor else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = sub.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // Chapter Selection
                    if (chaptersForSubject.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Syllabus Chapter / Topic (Optional)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chaptersForSubject) { chap ->
                                val isSel = selectedChapterId == chap.id
                                Surface(
                                    onClick = {
                                        selectedChapterId = if (isSel) null else chap.id
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) BrandBlue else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = chap.title,
                                        fontSize = 11.sp,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Title Input
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Plan Title / Task Description *") },
                    placeholder = { Text("e.g. Chapter 3 Numericals & Formulas") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Start Time & Duration Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = timeInput,
                        onValueChange = { timeInput = it },
                        label = { Text("Start Time (HH:mm)") },
                        placeholder = { Text("08:00") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = durationMinutes.toString(),
                        onValueChange = { durationMinutes = it.toIntOrNull() ?: 30 },
                        label = { Text("Duration (mins)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Quick Duration Pills
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 45, 60, 90, 120).forEach { dur ->
                        val isSel = durationMinutes == dur
                        Surface(
                            onClick = { durationMinutes = dur },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) BrandBlue else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${dur}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Calculated End Time Preview
                Spacer(modifier = Modifier.height(10.dp))
                val calculatedTimeRange = formatPlanTimeRange(timeInput, durationMinutes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Time Span: $calculatedTimeRange",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Overlap Warning Banner
                if (conflictingPlan != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StatusWarning.copy(alpha = 0.15f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Schedule Conflict: Overlaps with \"${conflictingPlan.title}\" (${conflictingPlan.time ?: ""})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarning
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes Input
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes / Materials Needed") },
                    placeholder = { Text("e.g. Bring scientific calculator") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Save / Update Plan Action
                Button(
                    onClick = {
                        val finalTitle = titleInput.trim().ifBlank {
                            if (planType == "STUDY") {
                                subjects.find { it.id == selectedSubjectId }?.name ?: "Study Plan"
                            } else planType
                        }

                        if (currentPlan == null) {
                            if (onCreatePlan != null) {
                                onCreatePlan(
                                    finalTitle,
                                    dateInput,
                                    timeInput.ifBlank { null },
                                    durationMinutes,
                                    selectedSubjectId,
                                    selectedChapterId,
                                    priorityInput,
                                    planType,
                                    notesInput.ifBlank { null }
                                )
                            } else {
                                onCreateTask(
                                    finalTitle,
                                    dateInput,
                                    timeInput.ifBlank { null },
                                    durationMinutes,
                                    selectedSubjectId,
                                    priorityInput,
                                    notesInput.ifBlank { null }
                                )
                            }
                        } else {
                            val updated = currentPlan.copy(
                                title = finalTitle,
                                date = dateInput,
                                time = timeInput.ifBlank { null },
                                durationMinutes = durationMinutes,
                                subjectId = selectedSubjectId,
                                chapterId = selectedChapterId,
                                priority = priorityInput,
                                planType = planType,
                                notes = notesInput.ifBlank { null },
                                updatedAt = System.currentTimeMillis()
                            )
                            onUpdatePlan?.invoke(updated)
                        }
                        showPlanSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (currentPlan == null) "Schedule Plan" else "Save Changes",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    // ==========================================
    // 7. DELETE CONFIRMATION DIALOG
    // ==========================================
    planToDelete?.let { plan ->
        AlertDialog(
            onDismissRequest = { planToDelete = null },
            title = { Text("Delete Study Plan?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove \"${plan.title}\" from your calendar?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTask(plan)
                        planToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// TIME FORMATTING HELPERS
// ==========================================

private fun parseTimeToMinutes(timeStr: String?): Int {
    if (timeStr.isNullOrBlank()) return 8 * 60 // Default 8:00 AM
    return try {
        val parts = timeStr.trim().split(":")
        val h = parts[0].toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.take(2)?.toIntOrNull() ?: 0
        h * 60 + m
    } catch (e: Exception) {
        8 * 60
    }
}

private fun formatStartTimeOnly(timeStr: String?): String {
    val minsOfDay = parseTimeToMinutes(timeStr)
    val hour24 = minsOfDay / 60
    val minute = minsOfDay % 60
    val isPm = hour24 >= 12
    val hour12 = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
    val amPm = if (isPm) "PM" else "AM"
    return String.format("%02d:%02d %s", hour12, minute, amPm)
}

private fun formatPlanTimeRange(timeStr: String?, durationMinutes: Int): String {
    val startMins = parseTimeToMinutes(timeStr)
    val endMins = (startMins + durationMinutes) % (24 * 60)

    val startStr = formatMinutesToAmPm(startMins)
    val endStr = formatMinutesToAmPm(endMins)

    val durHours = durationMinutes / 60
    val durMins = durationMinutes % 60
    val durStr = when {
        durHours > 0 && durMins > 0 -> "${durHours}h ${durMins}m"
        durHours > 0 -> "${durHours}h 00m"
        else -> "${durMins}m"
    }

    return "$startStr – $endStr  ($durStr)"
}

private fun formatMinutesToAmPm(totalMinutes: Int): String {
    val hour24 = totalMinutes / 60
    val minute = totalMinutes % 60
    val isPm = hour24 >= 12
    val hour12 = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
    val amPm = if (isPm) "PM" else "AM"
    return String.format("%02d:%02d %s", hour12, minute, amPm)
}
