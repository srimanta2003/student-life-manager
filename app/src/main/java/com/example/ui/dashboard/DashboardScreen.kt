package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentLifeViewModel
import com.example.util.AttendanceStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: StudentLifeViewModel,
    onNavigateToSubjects: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToExams: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToGpa: () -> Unit
) {
    val profile by viewModel.profile.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    val assignments by viewModel.assignments.collectAsState()
    val exams by viewModel.exams.collectAsState()
    val studyTasks by viewModel.studyTasks.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()
    val subjectAttendanceMap by viewModel.subjectAttendanceMap.collectAsState()

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayTasks = remember(studyTasks, todayStr) { studyTasks.filter { it.date == todayStr } }
    val upcomingExams = remember(exams, todayStr) {
        exams.filter { it.examDate >= todayStr }.sortedBy { it.examDate }.take(3)
    }
    val pendingAssignments = remember(assignments) {
        assignments.filter { it.status != "COMPLETED" && it.status != "SUBMITTED" }
            .sortedBy { it.dueDate }.take(3)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (profile?.fullName.isNullOrBlank()) "Welcome, Student" else "Hello, ${profile?.fullName}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${profile?.college ?: "University"} • Sem ${profile?.currentSemester ?: 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToChat,
                        modifier = Modifier.testTag("ai_assistant_icon_button")
                    ) {
                        Badge(
                            containerColor = EmeraldAccent,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "EduMate AI Assistant",
                                tint = IndigoPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Quick Summary Cards Row
                SummaryCardsSection(
                    summary = summary,
                    onNavigateToAttendance = onNavigateToAttendance,
                    onNavigateToSubjects = onNavigateToSubjects,
                    onNavigateToAssignments = onNavigateToAssignments,
                    onNavigateToExams = onNavigateToExams
                )
            }

            // Quick Actions Bar
            item {
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                QuickActionsRow(
                    onAddAttendance = onNavigateToAttendance,
                    onAddAssignment = onNavigateToAssignments,
                    onAddExam = onNavigateToExams,
                    onAddStudyTask = onNavigateToPlanner,
                    onAskAi = onNavigateToChat
                )
            }

            // Attendance Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAttendance() }
                        .testTag("attendance_overview_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FactCheck,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attendance Overview",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "View All →",
                                style = MaterialTheme.typography.labelMedium,
                                color = IndigoPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val overall = summary.overallAttendanceStats
                        if (overall == null || subjects.isEmpty()) {
                            Text(
                                text = "No attendance logged yet. Add your subjects and mark attendance to view live analytics.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${String.format("%.1f", overall.percentage)}%",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = when (overall.status) {
                                                AttendanceStatus.ON_TRACK -> EmeraldAccent
                                                AttendanceStatus.WARNING -> AmberWarning
                                                AttendanceStatus.CRITICAL -> RoseDanger
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    )
                                    Text(
                                        text = "${overall.attended} Attended • ${overall.missed} Missed • Target: ${overall.targetPercentage}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                StatusBadge(status = overall.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { (overall.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = when (overall.status) {
                                    AttendanceStatus.ON_TRACK -> EmeraldAccent
                                    AttendanceStatus.WARNING -> AmberWarning
                                    AttendanceStatus.CRITICAL -> RoseDanger
                                    else -> IndigoPrimary
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            if (overall.requiredConsecutiveClasses > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "⚠️ You must attend next ${overall.requiredConsecutiveClasses} consecutive classes to reach ${overall.targetPercentage}% target.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AmberWarning
                                )
                            }
                        }
                    }
                }
            }

            // Subject-wise Breakdown Carousel
            if (subjects.isNotEmpty()) {
                item {
                    Text(
                        text = "Subject-Wise Attendance",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(subjects) { subject ->
                            val stats = subjectAttendanceMap[subject.id]
                            Card(
                                modifier = Modifier
                                    .width(180.dp)
                                    .clickable { onNavigateToAttendance() },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = subject.code,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = IndigoPrimary
                                    )
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val percent = stats?.percentage ?: 0.0
                                    Text(
                                        text = "${String.format("%.0f", percent)}%",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (percent >= subject.minAttendance) EmeraldAccent else RoseDanger
                                        )
                                    )
                                    Text(
                                        text = "${stats?.attended ?: 0}/${stats?.totalConducted ?: 0} classes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Upcoming Deadlines (Assignments)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAssignments() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Assignment,
                                    contentDescription = null,
                                    tint = SkySecondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Pending Assignments (${pendingAssignments.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "All →",
                                style = MaterialTheme.typography.labelMedium,
                                color = SkySecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (pendingAssignments.isEmpty()) {
                            Text(
                                text = "No pending assignments! You are all caught up.",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldAccent
                            )
                        } else {
                            pendingAssignments.forEach { assn ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = assn.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Due: ${assn.dueDate} • Priority: ${assn.priority}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    PriorityPill(priority = assn.priority)
                                }
                            }
                        }
                    }
                }
            }

            // Upcoming Examinations
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToExams() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = AmberWarning
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Upcoming Exams",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "Timetable →",
                                style = MaterialTheme.typography.labelMedium,
                                color = AmberWarning,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (upcomingExams.isEmpty()) {
                            Text(
                                text = "No upcoming exams scheduled.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            upcomingExams.forEach { exam ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exam.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${exam.examDate} • ${exam.startTime} - ${exam.endTime} (${exam.venue ?: "Hall TBA"})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AmberContainerLight
                                    ) {
                                        Text(
                                            text = exam.examType,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AmberContainerDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Today's Study Tasks
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPlanner() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = EmeraldAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Today's Study Plan (${todayTasks.count { it.isCompleted }}/${todayTasks.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "Planner →",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (todayTasks.isEmpty()) {
                            Text(
                                text = "No study tasks scheduled for today. Tap Planner to set daily goals.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            todayTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { viewModel.toggleStudyTaskCompletion(task) }
                                    )
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${task.targetMinutes}m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SummaryCardsSection(
    summary: com.example.ui.viewmodel.DashboardSummary,
    onNavigateToAttendance: () -> Unit,
    onNavigateToSubjects: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToExams: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val overallAtt = summary.overallAttendanceStats?.percentage
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Attendance",
            value = if (overallAtt != null) "${String.format("%.0f", overallAtt)}%" else "--",
            color = IndigoPrimary,
            icon = Icons.Default.FactCheck,
            onClick = onNavigateToAttendance
        )
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Subjects",
            value = "${summary.totalSubjects}",
            color = SkySecondary,
            icon = Icons.Default.MenuBook,
            onClick = onNavigateToSubjects
        )
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Pending",
            value = "${summary.pendingAssignmentsCount}",
            color = RoseDanger,
            icon = Icons.Default.AssignmentLate,
            onClick = onNavigateToAssignments
        )
        MetricMiniCard(
            modifier = Modifier.weight(1f),
            label = "Exams",
            value = "${summary.upcomingExamsCount}",
            color = AmberWarning,
            icon = Icons.Default.CalendarMonth,
            onClick = onNavigateToExams
        )
    }
}

@Composable
private fun MetricMiniCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(96.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickActionsRow(
    onAddAttendance: () -> Unit,
    onAddAssignment: () -> Unit,
    onAddExam: () -> Unit,
    onAddStudyTask: () -> Unit,
    onAskAi: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item {
            ActionChip(label = "+ Attendance", icon = Icons.Default.Check, onClick = onAddAttendance)
        }
        item {
            ActionChip(label = "+ Assignment", icon = Icons.Default.Add, onClick = onAddAssignment)
        }
        item {
            ActionChip(label = "+ Exam", icon = Icons.Default.Event, onClick = onAddExam)
        }
        item {
            ActionChip(label = "+ Study Task", icon = Icons.Default.Timer, onClick = onAddStudyTask)
        }
        item {
            ActionChip(label = "Ask EduMate AI", icon = Icons.Default.AutoAwesome, onClick = onAskAi, isAi = true)
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isAi: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isAi) IndigoContainerLight else MaterialTheme.colorScheme.surface,
        border = if (isAi) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isAi) IndigoPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAi) IndigoDark else MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun StatusBadge(status: AttendanceStatus) {
    val (label, bg, text) = when (status) {
        AttendanceStatus.ON_TRACK -> Triple("On Track", EmeraldContainerLight, EmeraldContainerDark)
        AttendanceStatus.WARNING -> Triple("Warning", AmberContainerLight, AmberContainerDark)
        AttendanceStatus.CRITICAL -> Triple("At Risk", RoseContainerLight, RoseContainerDark)
        AttendanceStatus.NOT_AVAILABLE -> Triple("Pending", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun PriorityPill(priority: String) {
    val (bg, text) = when (priority.uppercase()) {
        "URGENT" -> Pair(RoseContainerLight, RoseContainerDark)
        "HIGH" -> Pair(AmberContainerLight, AmberContainerDark)
        "MEDIUM" -> Pair(SkyContainerLight, SkyContainerDark)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = priority,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
