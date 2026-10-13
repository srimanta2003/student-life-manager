package com.example.ui.attendance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.Subject
import com.example.ui.dashboard.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentLifeViewModel
import com.example.util.AttendanceCalculator
import com.example.util.AttendanceStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: StudentLifeViewModel
) {
    val subjects by viewModel.subjects.collectAsState()
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()
    val subjectAttendanceMap by viewModel.subjectAttendanceMap.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Subjects, 1: Projection Simulator, 2: History Log
    var historySubjectFilter by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("attendance_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Attendance Manager", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Overall Stats Card
            summary.overallAttendanceStats?.let { stats ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            Column {
                                Text(
                                    text = "Overall Attendance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.1f", stats.percentage)}%",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = when (stats.status) {
                                            AttendanceStatus.ON_TRACK -> EmeraldAccent
                                            AttendanceStatus.WARNING -> AmberWarning
                                            AttendanceStatus.CRITICAL -> RoseDanger
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                )
                            }
                            StatusBadge(status = stats.status)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { (stats.percentage / 100.0).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = when (stats.status) {
                                AttendanceStatus.ON_TRACK -> EmeraldAccent
                                AttendanceStatus.WARNING -> AmberWarning
                                AttendanceStatus.CRITICAL -> RoseDanger
                                else -> IndigoPrimary
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Attended: ${stats.attended} • Missed: ${stats.missed}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (stats.requiredConsecutiveClasses > 0) {
                                Text(
                                    text = "Need ${stats.requiredConsecutiveClasses} more to reach ${stats.targetPercentage}%",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = AmberWarning
                                )
                            } else if (stats.safeBunksAvailable > 0) {
                                Text(
                                    text = "Can safely miss ${stats.safeBunksAvailable} classes",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldAccent
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Subjects (${subjects.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Projection Simulator") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("History (${attendanceRecords.size})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    if (subjects.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Add subjects first to record attendance.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(subjects, key = { it.id }) { subject ->
                                val stats = subjectAttendanceMap[subject.id]
                                SubjectAttendanceCard(
                                    subject = subject,
                                    stats = stats,
                                    onMarkPresent = {
                                        viewModel.markAttendance(
                                            subjectId = subject.id,
                                            status = "PRESENT"
                                        )
                                    },
                                    onMarkAbsent = {
                                        viewModel.markAttendance(
                                            subjectId = subject.id,
                                            status = "ABSENT"
                                        )
                                    }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    }
                }
                1 -> {
                    // Projection Simulator
                    ProjectionSimulatorTab(
                        subjects = subjects,
                        subjectAttendanceMap = subjectAttendanceMap
                    )
                }
                2 -> {
                    // History Log
                    AttendanceHistoryTab(
                        records = attendanceRecords,
                        subjects = subjects,
                        selectedFilter = historySubjectFilter,
                        onFilterChanged = { historySubjectFilter = it },
                        onDeleteRecord = { id -> viewModel.deleteAttendance(id) }
                    )
                }
            }
        }
    }
}

@Composable
fun SubjectAttendanceCard(
    subject: Subject,
    stats: com.example.util.AttendanceStats?,
    onMarkPresent: () -> Unit,
    onMarkAbsent: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_card_${subject.code}"),
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
                Column {
                    Text(
                        text = "${subject.code} • ${subject.name}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Target: ${subject.minAttendance}% • Conducted: ${stats?.totalConducted ?: 0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val pct = stats?.percentage ?: 0.0
                Text(
                    text = "${String.format("%.1f", pct)}%",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (pct >= subject.minAttendance) EmeraldAccent else RoseDanger
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { ((stats?.percentage ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if ((stats?.percentage ?: 0.0) >= subject.minAttendance) EmeraldAccent else RoseDanger,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Present: ${stats?.attended ?: 0} | Absent: ${stats?.missed ?: 0}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (stats != null && stats.requiredConsecutiveClasses > 0) {
                    Text(
                        text = "Need ${stats.requiredConsecutiveClasses} more",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AmberWarning
                    )
                } else if (stats != null && stats.safeBunksAvailable > 0) {
                    Text(
                        text = "${stats.safeBunksAvailable} bunks safe",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // One-tap attendance buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onMarkPresent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_present_${subject.code}"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Present (+1)")
                }

                Button(
                    onClick = onMarkAbsent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_absent_${subject.code}"),
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Absent (+1)")
                }
            }
        }
    }
}

@Composable
fun ProjectionSimulatorTab(
    subjects: List<Subject>,
    subjectAttendanceMap: Map<String, com.example.util.AttendanceStats>
) {
    if (subjects.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Add subjects to use simulator.")
        }
        return
    }

    var selectedSubjectId by remember { mutableStateOf(subjects.first().id) }
    var futureAttendCount by remember { mutableIntStateOf(5) }
    var futureMissCount by remember { mutableIntStateOf(0) }

    val currentSub = subjects.find { it.id == selectedSubjectId } ?: subjects.first()
    val stats = subjectAttendanceMap[currentSub.id]
    val currentAttended = stats?.attended ?: 0
    val currentTotal = stats?.totalConducted ?: 0

    val projectedPct = remember(currentAttended, currentTotal, futureAttendCount, futureMissCount) {
        AttendanceCalculator.projectAttendance(
            currentAttended = currentAttended,
            currentTotal = currentTotal,
            futureAttended = futureAttendCount,
            futureMissed = futureMissCount
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Attendance Projection Calculator",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Simulate how future attendance or absences will change your percentage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Select Subject:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subjects.forEach { s ->
                            FilterChip(
                                selected = s.id == selectedSubjectId,
                                onClick = { selectedSubjectId = s.id },
                                label = { Text(s.code) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Future Attend stepper
                    Text("If I attend next classes:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = { if (futureAttendCount > 0) futureAttendCount-- }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                        }
                        Text(
                            text = "+$futureAttendCount classes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldAccent)
                        )
                        IconButton(onClick = { futureAttendCount++ }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Future Miss stepper
                    Text("If I miss next classes:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = { if (futureMissCount > 0) futureMissCount-- }) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                        }
                        Text(
                            text = "+$futureMissCount classes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = RoseDanger)
                        )
                        IconButton(onClick = { futureMissCount++ }) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Result
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Attendance", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${String.format("%.1f", stats?.percentage ?: 0.0)}%",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text("➔", style = MaterialTheme.typography.titleLarge)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Projected Attendance", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${String.format("%.1f", projectedPct)}%",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (projectedPct >= currentSub.minAttendance) EmeraldAccent else RoseDanger
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (projectedPct >= currentSub.minAttendance)
                            "✅ Goal achieved! You will exceed the ${currentSub.minAttendance}% target."
                        else
                            "⚠️ Warning: Still below the ${currentSub.minAttendance}% minimum requirement.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (projectedPct >= currentSub.minAttendance) EmeraldAccent else RoseDanger,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun AttendanceHistoryTab(
    records: List<AttendanceRecord>,
    subjects: List<Subject>,
    selectedFilter: String?,
    onFilterChanged: (String?) -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    val filtered = remember(records, selectedFilter) {
        if (selectedFilter == null) records.sortedByDescending { it.date }
        else records.filter { it.subjectId == selectedFilter }.sortedByDescending { it.date }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterChanged(null) },
                label = { Text("All") }
            )
            subjects.forEach { s ->
                FilterChip(
                    selected = selectedFilter == s.id,
                    onClick = { onFilterChanged(if (selectedFilter == s.id) null else s.id) },
                    label = { Text(s.code) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No attendance logs found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { item ->
                    val sub = subjects.find { it.id == item.subjectId }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (item.status == "PRESENT") EmeraldContainerLight else RoseContainerLight,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (item.status == "PRESENT") Icons.Default.Check else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (item.status == "PRESENT") EmeraldContainerDark else RoseContainerDark,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "${sub?.code ?: "Course"} • ${item.status}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${item.date} • ${item.sessionType}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { onDeleteRecord(item.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete record",
                                    tint = RoseDanger,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}
