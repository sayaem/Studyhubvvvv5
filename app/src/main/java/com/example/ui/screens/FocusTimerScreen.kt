package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChapterEntity
import com.example.data.local.FocusGamificationEntity
import com.example.data.local.FocusPresetEntity
import com.example.data.local.StudySessionEntity
import com.example.data.local.SubjectEntity
import com.example.ui.TimerStatus
import com.example.ui.components.getSubjectIcon
import com.example.ui.components.parseColorHex
import com.example.ui.theme.BrandDeepTeal
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerScreen(
    timerStatus: TimerStatus,
    timerMode: String,
    pomodoroPhase: String,
    currentCycle: Int,
    targetCycles: Int,
    timeRemainingSeconds: Int,
    totalDurationSeconds: Int,
    selectedSessionType: String,
    selectedSubjectId: Long?,
    selectedGoalTask: String?,
    isStrictFocus: Boolean,
    ambientSoundType: String,
    ambientVolume: Float,
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    sessions: List<StudySessionEntity>,
    presets: List<FocusPresetEntity>,
    gamification: FocusGamificationEntity?,
    dailyGoalMinutes: Int = 240,
    onSelectMode: (String) -> Unit,
    onApplyPreset: (FocusPresetEntity) -> Unit,
    onSaveCustomPreset: (String, String, Int, Int, Int, Int, Boolean) -> Unit,
    onDeletePreset: (FocusPresetEntity) -> Unit,
    onSelectSubject: (Long?) -> Unit,
    onSetGoalTask: (String?) -> Unit,
    onToggleStrict: (Boolean) -> Unit,
    onSetAmbientSound: (String) -> Unit,
    onSetAmbientVolume: (Float) -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onSkipPhase: () -> Unit,
    onFinishTimer: (Boolean, Int, String?, Int) -> Unit,
    onDeleteSession: (StudySessionEntity) -> Unit = {}
) {
    var activeTab by remember { mutableStateOf("TIMER") } // "TIMER" or "ANALYTICS"
    var showAmbientSheet by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showCustomPresetSheet by remember { mutableStateOf(false) }
    var showReflectionDialog by remember { mutableStateOf(false) }
    var showStrictOverlay by remember { mutableStateOf(false) }

    // Gamification level calculation
    val xp = gamification?.totalXp ?: 0
    val userLevel = 1 + (xp / 1000)
    val streakDays = gamification?.currentStreakDays ?: 0

    // Auto trigger reflection dialog when timer completes normally
    var reflectionTriggered by remember { mutableStateOf(false) }
    if (timerStatus == TimerStatus.COMPLETED && !reflectionTriggered) {
        showReflectionDialog = true
        reflectionTriggered = true
    } else if (timerStatus != TimerStatus.COMPLETED) {
        reflectionTriggered = false
    }

    // Auto trigger strict overlay when strict mode is active and running
    if (isStrictFocus && timerStatus == TimerStatus.RUNNING) {
        showStrictOverlay = true
    }

    val activeSubject = subjects.find { it.id == selectedSubjectId }

    val primaryModeColor = when (timerMode) {
        "POMODORO" -> if (pomodoroPhase == "WORK") BrandTeal else Color(0xFFF59E0B) // Amber for break
        "DEEP_FOCUS" -> Color(0xFF8B5CF6) // Purple
        "SIMPLE_TIMER" -> Color(0xFF2563EB) // Blue
        else -> Color(0xFF10B981) // Stopwatch Green
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        // --- Top Bar Header ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Focus Command Center",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Level $userLevel Scholar • $streakDays Day Streak 🔥",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BrandTeal,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Ambient Sound Chip
                Surface(
                    onClick = { showAmbientSheet = true },
                    shape = RoundedCornerShape(20.dp),
                    color = if (ambientSoundType != "OFF") BrandTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (ambientSoundType != "OFF") BrandTeal else MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (ambientSoundType != "OFF") Icons.Default.GraphicEq else Icons.Default.MusicNote,
                            contentDescription = "Ambient Sounds",
                            tint = if (ambientSoundType != "OFF") BrandTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (ambientSoundType != "OFF") ambientSoundType else "Sound",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ambientSoundType != "OFF") BrandTeal else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // View Switcher (Timer / Analytics)
                Surface(
                    onClick = { activeTab = if (activeTab == "TIMER") "ANALYTICS" else "TIMER" },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (activeTab == "TIMER") Icons.Default.Analytics else Icons.Default.Timer,
                            contentDescription = "Toggle View",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (activeTab == "TIMER") "Stats" else "Timer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (activeTab == "TIMER") {
            // ==========================================
            // ============ TIMER TAB VIEW =============
            // ==========================================

            // 1. Mode Switcher Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf(
                    Triple("POMODORO", "Pomodoro", "25m / 5m"),
                    Triple("DEEP_FOCUS", "Deep Focus", "50m Block"),
                    Triple("SIMPLE_TIMER", "Timer", "Custom"),
                    Triple("STOPWATCH", "Stopwatch", "Count-up")
                )
                modes.forEach { (modeKey, label, sub) ->
                    val isSelected = timerMode == modeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) primaryModeColor else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectMode(modeKey) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) primaryModeColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Hero Interactive Circular Timer Display Card
            val progress = if (timerMode == "STOPWATCH") 1f else {
                if (totalDurationSeconds > 0) {
                    1f - (timeRemainingSeconds.toFloat() / totalDurationSeconds)
                } else 0f
            }

            val displayMinutes = timeRemainingSeconds / 60
            val displaySeconds = timeRemainingSeconds % 60
            val displayTime = String.format("%02d:%02d", displayMinutes, displaySeconds)

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
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pomodoro Cycle dots if Pomodoro
                    if (timerMode == "POMODORO") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cycle $currentCycle of $targetCycles:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            (1..targetCycles).forEach { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                idx < currentCycle -> StatusSuccess
                                                idx == currentCycle -> primaryModeColor
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    val trackColor = MaterialTheme.colorScheme.surfaceVariant

                    // Main Circular Ring Timer
                    Box(
                        modifier = Modifier.size(210.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                            val arcSize = Size(diameter, diameter)

                            // Track
                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Progress
                            drawArc(
                                color = primaryModeColor,
                                startAngle = -90f,
                                sweepAngle = progress * 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = displayTime,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = when (timerMode) {
                                    "POMODORO" -> if (pomodoroPhase == "WORK") "Work Focus" else "Break Time"
                                    "DEEP_FOCUS" -> "Deep Focus Block"
                                    "SIMPLE_TIMER" -> "Countdown"
                                    else -> "Stopwatch Elapsed"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryModeColor
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Big Action Button inside Ring
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(primaryModeColor)
                                    .testTag("focus_play_pause_button")
                                    .clickable {
                                        when (timerStatus) {
                                            TimerStatus.IDLE -> onStartTimer()
                                            TimerStatus.RUNNING -> onPauseTimer()
                                            TimerStatus.PAUSED -> onResumeTimer()
                                            TimerStatus.COMPLETED -> onResetTimer()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (timerStatus) {
                                        TimerStatus.RUNNING -> Icons.Default.Pause
                                        else -> Icons.Default.PlayArrow
                                    },
                                    contentDescription = "Timer Action",
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Control Row (Reset, Skip/Finish, Strict Mode)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (timerStatus == TimerStatus.RUNNING || timerStatus == TimerStatus.PAUSED) {
                            OutlinedButton(
                                onClick = onResetTimer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset")
                            }

                            if (timerMode == "POMODORO") {
                                OutlinedButton(
                                    onClick = onSkipPhase,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Skip")
                                }
                            }

                            Button(
                                onClick = { showReflectionDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = primaryModeColor),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Finish Log", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Strict Focus Mode Toggle Badge
                            Surface(
                                onClick = { onToggleStrict(!isStrictFocus) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isStrictFocus) Color(0xFFEF4444).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isStrictFocus) Color(0xFFEF4444) else MaterialTheme.colorScheme.outline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Strict Mode",
                                        tint = if (isStrictFocus) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isStrictFocus) "Strict Focus ON" else "Strict Focus OFF",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isStrictFocus) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Quick Presets Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timer Presets",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                TextButton(onClick = { showCustomPresetSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Custom Setup", fontSize = 12.sp)
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets, key = { it.id }) { preset ->
                    Card(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onApplyPreset(preset) }
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = preset.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${preset.workMinutes}m • ${preset.mode}",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Subject & Session Goal Picker Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Subject & Session Goal",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Subjects Pills
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            val isGeneral = selectedSubjectId == null
                            Surface(
                                onClick = { onSelectSubject(null) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isGeneral) BrandTeal else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "General",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGeneral) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        items(subjects) { sub ->
                            val isSel = selectedSubjectId == sub.id
                            val subColor = parseColorHex(sub.colorHex)
                            Surface(
                                onClick = { onSelectSubject(sub.id) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) subColor else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getSubjectIcon(sub.iconKey),
                                        contentDescription = null,
                                        tint = if (isSel) Color.White else subColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = sub.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Goal Task pill
                    Surface(
                        onClick = { showGoalDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Flag,
                                    contentDescription = "Session Goal",
                                    tint = BrandTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedGoalTask?.ifBlank { null } ?: "Set specific target (e.g. Solve 20 MCQ)",
                                    fontSize = 12.sp,
                                    color = if (!selectedGoalTask.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.Edit, contentDescription = "Edit Goal", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

        } else {
            // ==========================================
            // ======== ANALYTICS & HISTORY VIEW ========
            // ==========================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Daily Target Card
                item {
                    val todayMins = sessions.filter {
                        val sDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.startedAt))
                        sDate == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) && it.status == "COMPLETED"
                    }.sumOf { it.elapsedSeconds } / 60

                    val targetPct = if (dailyGoalMinutes > 0) ((todayMins.toFloat() / dailyGoalMinutes) * 100).toInt().coerceAtMost(100) else 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Today's Focus Goal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(text = "$todayMins of $dailyGoalMinutes mins ($targetPct%)", fontSize = 12.sp, color = TextSecondary)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BrandTeal.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(text = "Level $userLevel Scholar", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BrandTeal)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { targetPct / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = BrandTeal,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                // 2. Weekly Canvas Bar Chart
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "Weekly Focus Minutes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(modifier = Modifier.height(12.dp))

                            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            // Calculate mins per day for current week
                            val dayMinutesMap = remember(sessions) {
                                val cal = Calendar.getInstance()
                                val map = mutableMapOf<Int, Int>() // dayOfWeek -> mins
                                (1..7).forEach { map[it] = 0 }
                                sessions.filter { it.status == "COMPLETED" }.forEach { sess ->
                                    cal.timeInMillis = sess.startedAt
                                    val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
                                    val mappedIdx = if (dow == Calendar.SUNDAY) 7 else (dow - 1)
                                    map[mappedIdx] = (map[mappedIdx] ?: 0) + (sess.elapsedSeconds / 60)
                                }
                                map
                            }

                            val maxMins = (dayMinutesMap.values.maxOrNull() ?: 120).coerceAtLeast(60)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                (1..7).forEach { dayIdx ->
                                    val mins = dayMinutesMap[dayIdx] ?: 0
                                    val barHeightRatio = (mins.toFloat() / maxMins).coerceIn(0.08f, 1f)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = "${mins}m", fontSize = 9.sp, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.5f)
                                                .fillMaxSize(barHeightRatio)
                                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                .background(if (mins > 0) BrandTeal else MaterialTheme.colorScheme.surfaceVariant)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = days[dayIdx - 1], fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Achievements Grid
                item {
                    Text(
                        text = "Focus Achievements",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val unlockedIds = gamification?.unlockedAchievementIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

                    val allBadges = listOf(
                        Triple("first_session", "First Step", "Complete 1 session"),
                        Triple("streak_3", "Streak 3", "3 day streak"),
                        Triple("streak_7", "Streak 7", "7 day streak"),
                        Triple("focus_100", "Centurion", "100 focus mins"),
                        Triple("focus_500", "Scholar", "500 focus mins"),
                        Triple("deep_diver", "Deep Diver", "60m single session")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allBadges.take(3).forEach { (id, title, desc) ->
                            val isUnlocked = unlockedIds.contains(id)
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isUnlocked) BrandTeal else TextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(text = desc, fontSize = 9.sp, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                // 4. Session History List
                item {
                    Text(
                        text = "Session History Log",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (sessions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                Text(text = "No focus sessions logged yet. Complete a session to see history!", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                } else {
                    items(sessions, key = { it.id }) { sess ->
                        val sub = subjects.find { it.id == sess.subjectId }
                        val mins = sess.elapsedSeconds / 60
                        val subColor = sub?.let { parseColorHex(it.colorHex) } ?: BrandTeal

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(subColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getSubjectIcon(sub?.iconKey ?: "book"),
                                        contentDescription = null,
                                        tint = subColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub?.name ?: "General Focus",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(sess.startedAt)),
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = "$mins mins",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = BrandTeal
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // ============= MODALS & SHEETS ============
    // ==========================================

    // Goal Task Dialog
    if (showGoalDialog) {
        var goalInput by remember { mutableStateOf(selectedGoalTask ?: "") }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Session Target Goal", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { goalInput = it },
                    label = { Text("What do you want to accomplish?") },
                    placeholder = { Text("e.g. Solve 20 Physics Numericals") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetGoalTask(goalInput.ifBlank { null })
                        showGoalDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                ) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Ambient Sound Bottom Sheet
    if (showAmbientSheet) {
        ModalBottomSheet(onDismissRequest = { showAmbientSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Ambient Soundscapes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(14.dp))

                val sounds = listOf(
                    Triple("OFF", "Off", "Silence"),
                    Triple("RAIN", "Rainfall", "Calming rain noise"),
                    Triple("OCEAN", "Ocean Waves", "Soothing wave rhythm"),
                    Triple("LOFI", "Lo-Fi Beats", "Warm ambient tones"),
                    Triple("CAFE", "Cozy Cafe", "Soft focus texture"),
                    Triple("WHITE_NOISE", "White Noise", "Constant fan sound")
                )

                sounds.forEach { (typeKey, title, desc) ->
                    val isSel = ambientSoundType == typeKey
                    Surface(
                        onClick = { onSetAmbientSound(typeKey) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) BrandTeal.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) BrandTeal else MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = desc, fontSize = 11.sp, color = TextSecondary)
                            }
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = BrandTeal)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(text = "Volume Control", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = ambientVolume,
                    onValueChange = { onSetAmbientVolume(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = BrandTeal, activeTrackColor = BrandTeal)
                )

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // Custom Preset Setup Sheet
    if (showCustomPresetSheet) {
        var presetTitle by remember { mutableStateOf("") }
        var workMinsInput by remember { mutableIntStateOf(25) }
        var shortBreakInput by remember { mutableIntStateOf(5) }
        var longBreakInput by remember { mutableIntStateOf(15) }
        var cyclesInput by remember { mutableIntStateOf(4) }
        var isStrictInput by remember { mutableStateOf(false) }

        ModalBottomSheet(onDismissRequest = { showCustomPresetSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(text = "Custom Preset Setup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = presetTitle,
                    onValueChange = { presetTitle = it },
                    label = { Text("Preset Name") },
                    placeholder = { Text("e.g. Physics Sprint") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = workMinsInput.toString(),
                        onValueChange = { workMinsInput = it.toIntOrNull() ?: 25 },
                        label = { Text("Work (m)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = shortBreakInput.toString(),
                        onValueChange = { shortBreakInput = it.toIntOrNull() ?: 5 },
                        label = { Text("Short Break (m)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        onSaveCustomPreset(presetTitle, "POMODORO", workMinsInput, shortBreakInput, longBreakInput, cyclesInput, isStrictInput)
                        showCustomPresetSheet = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Preset", color = Color.White)
                }
            }
        }
    }

    // Reflection & Completion Dialog Modal
    if (showReflectionDialog) {
        var rating by remember { mutableIntStateOf(5) }
        var notesText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showReflectionDialog = false },
            title = {
                Column {
                    Text(text = "Session Complete! 🎉", fontWeight = FontWeight.ExtraBold)
                    Text(text = "+${(timeRemainingSeconds / 60) * 10 + 50} XP Earned!", fontSize = 12.sp, color = BrandTeal, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(text = "How productive was this session?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        (1..5).forEach { starIdx ->
                            IconButton(onClick = { rating = starIdx }) {
                                Icon(
                                    imageVector = if (starIdx <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Star $starIdx",
                                    tint = if (starIdx <= rating) Color(0xFFF59E0B) else TextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Quick Reflection / Notes") },
                        placeholder = { Text("What did you complete?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onFinishTimer(true, rating, notesText, 0)
                        showReflectionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                ) {
                    Text("Log Session", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReflectionDialog = false }) { Text("Skip") }
            }
        )
    }
}

