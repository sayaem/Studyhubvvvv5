package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChapterEntity
import com.example.data.local.StudySessionEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.UserProfile
import com.example.ui.components.StudyHubDetailTopBar
import com.example.ui.components.getSubjectIcon
import com.example.ui.components.parseColorHex
import com.example.ui.theme.BorderSubtleLight
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandPaleTeal
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ProgressAnalyticsScreen(
    userProfile: UserProfile?,
    sessions: List<StudySessionEntity>,
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    onBack: () -> Unit,
    onStartFocus: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("7 Days") } // "7 Days", "30 Days", "3 Months"

    // Calculate time window
    val calendar = Calendar.getInstance()
    val periodDays = when (selectedPeriod) {
        "30 Days" -> 30
        "3 Months" -> 90
        else -> 7
    }

    val cutoffTime = remember(selectedPeriod) {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -periodDays)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    val filteredSessions = sessions.filter {
        it.status == "COMPLETED" && it.startedAt >= cutoffTime
    }

    val totalPeriodSeconds = filteredSessions.sumOf { it.elapsedSeconds }
    val totalPeriodMinutes = totalPeriodSeconds / 60

    // Day by day distribution for the last 7 days
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val last7Days = remember {
        val list = mutableListOf<Pair<String, Int>>() // DayName, Minutes
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0..6) {
            val startOfDay = cal.clone() as Calendar
            startOfDay.set(Calendar.HOUR_OF_DAY, 0)
            startOfDay.set(Calendar.MINUTE, 0)
            startOfDay.set(Calendar.SECOND, 0)

            val endOfDay = cal.clone() as Calendar
            endOfDay.set(Calendar.HOUR_OF_DAY, 23)
            endOfDay.set(Calendar.MINUTE, 59)
            endOfDay.set(Calendar.SECOND, 59)

            val dayName = dayFormat.format(cal.time)
            val daySecs = sessions.filter {
                it.status == "COMPLETED" && it.startedAt in startOfDay.timeInMillis..endOfDay.timeInMillis
            }.sumOf { it.elapsedSeconds }

            list.add(dayName to (daySecs / 60))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val maxDayMinutes = (last7Days.maxOfOrNull { it.second } ?: 60).coerceAtLeast(60)

    // Subject breakdown
    val subjectStats = subjects.map { sub ->
        val subMinutes = filteredSessions.filter { it.subjectId == sub.id }.sumOf { it.elapsedSeconds } / 60
        val subChapters = chapters.filter { it.subjectId == sub.id }
        val completedChapters = subChapters.count { it.status == "COMPLETED" }
        Triple(sub, subMinutes, Pair(completedChapters, subChapters.size))
    }.sortedByDescending { it.second }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        StudyHubDetailTopBar(
            title = "Progress & Review",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Period Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("7 Days", "30 Days", "3 Months").forEach { period ->
                    val isSelected = selectedPeriod == period
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedPeriod = period },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Bar Chart Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Total Study Time",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatMinutes(totalPeriodMinutes),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // 7-day Bar Chart
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        last7Days.forEach { (dayName, dayMins) ->
                            val barHeightRatio = (dayMins.toFloat() / maxDayMinutes).coerceIn(0.04f, 1f)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (dayMins > 0) {
                                    Text(
                                        text = "${dayMins}m",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTeal
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height((barHeightRatio * 85).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(if (dayMins > 0) BrandBlue else MaterialTheme.colorScheme.surfaceVariant)
                                 )
                                 Spacer(modifier = Modifier.height(6.dp))
                                 Text(
                                     text = dayName,
                                     fontSize = 11.sp,
                                     color = MaterialTheme.colorScheme.onSurfaceVariant
                                 )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Subject-wise Study Time
            Text(
                text = "Subject Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (subjects.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No study sessions recorded yet",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Good things take time. Start a study session to see your progress here.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onStartFocus,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Start Focus")
                        }
                    }
                }
            } else {
                subjectStats.forEach { (sub, mins, chapStats) ->
                    val (completedChap, totalChap) = chapStats
                    val subColor = parseColorHex(sub.colorHex)
                    val shareRatio = if (totalPeriodMinutes > 0) {
                        (mins.toFloat() / totalPeriodMinutes).coerceIn(0f, 1f)
                    } else 0f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                        imageVector = getSubjectIcon(sub.iconKey),
                                        contentDescription = sub.name,
                                        tint = subColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "$completedChap / $totalChap chapters completed",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = formatMinutes(mins),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = BrandTeal
                                )
                            }

                            if (totalPeriodMinutes > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { shareRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = subColor,
                                    trackColor = subColor.copy(alpha = 0.15f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
