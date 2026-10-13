package com.example.ui.planner

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.StudyTask
import com.example.data.model.Subject
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentLifeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlannerScreen(
    viewModel: StudentLifeViewModel
) {
    val studyTasks by viewModel.studyTasks.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val pomodoro by viewModel.pomodoro.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<StudyTask?>(null) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayTasks = remember(studyTasks, todayStr) { studyTasks.filter { it.date == todayStr } }
    val futureTasks = remember(studyTasks, todayStr) { studyTasks.filter { it.date > todayStr } }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("study_planner_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Study Planner & Focus", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAddTaskDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_study_task")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Study Task")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pomodoro Focus Timer Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (pomodoro.isBreak) EmeraldContainerLight else IndigoContainerLight
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (pomodoro.isBreak) EmeraldContainerDark else IndigoDark
                        ) {
                            Text(
                                text = if (pomodoro.isBreak) "☕ BREAK TIME (5m)" else "🎯 FOCUS SESSION (25m)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large countdown
                        val minutes = pomodoro.remainingSeconds / 60
                        val seconds = pomodoro.remainingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (pomodoro.isBreak) EmeraldContainerDark else IndigoDark,
                                letterSpacing = 2.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = {
                                if (pomodoro.totalSeconds > 0)
                                    (1f - (pomodoro.remainingSeconds.toFloat() / pomodoro.totalSeconds.toFloat())).coerceIn(0f, 1f)
                                else 0f
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (pomodoro.isBreak) EmeraldAccent else IndigoPrimary,
                            trackColor = Color.White.copy(alpha = 0.5f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!pomodoro.isRunning) {
                                Button(
                                    onClick = { viewModel.startPomodoro() },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (pomodoro.isBreak) EmeraldAccent else IndigoPrimary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Start Focus")
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.pausePomodoro() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause")
                                }
                            }

                            OutlinedButton(onClick = { viewModel.resetPomodoro() }) {
                                Icon(Icons.Default.Replay, contentDescription = "Reset")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reset")
                            }
                        }

                        if (pomodoro.sessionsCompleted > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "🔥 ${pomodoro.sessionsCompleted} Pomodoro sessions completed today!",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }

            // Today's Study Tasks
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Tasks (${todayTasks.count { it.isCompleted }}/${todayTasks.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    TextButton(onClick = { showAddTaskDialog = true }) {
                        Text("+ Add Task")
                    }
                }

                if (todayTasks.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "No study tasks scheduled for today. Plan your revision goals!",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        todayTasks.forEach { task ->
                            val sub = subjects.find { it.id == task.subjectId }
                            StudyTaskItem(
                                task = task,
                                subject = sub,
                                onToggle = { viewModel.toggleStudyTaskCompletion(task) },
                                onDelete = { taskToDelete = task }
                            )
                        }
                    }
                }
            }

            // Upcoming Study Sessions
            if (futureTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Upcoming Study Sessions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        futureTasks.forEach { task ->
                            val sub = subjects.find { it.id == task.subjectId }
                            StudyTaskItem(
                                task = task,
                                subject = sub,
                                onToggle = { viewModel.toggleStudyTaskCompletion(task) },
                                onDelete = { taskToDelete = task }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showAddTaskDialog) {
        AddStudyTaskDialog(
            subjects = subjects,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, subId, date, minutes, notes ->
                viewModel.saveStudyTask(
                    title = title,
                    subjectId = subId,
                    date = date,
                    targetMinutes = minutes,
                    notes = notes
                )
                showAddTaskDialog = false
            }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = { Text("Delete '${task.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteStudyTask(task.id)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseDanger)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun StudyTaskItem(
    task: StudyTask,
    subject: Subject?,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${task.date} • ${task.targetMinutes} mins",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (subject != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${subject.code}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = IndigoPrimary
                        )
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddStudyTaskDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (title: String, subjectId: String?, date: String, minutes: Int, notes: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var minutesStr by remember { mutableStateOf("45") }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Add Study Goal / Task", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                if (errorMsg != null) {
                    Text(text = errorMsg ?: "", color = RoseDanger, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title * (e.g. Chapter 4 Practice)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("study_task_title_input")
                )

                if (subjects.isNotEmpty()) {
                    Text("Associated Subject (Optional)", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { s ->
                            FilterChip(
                                selected = selectedSubjectId == s.id,
                                onClick = { selectedSubjectId = if (selectedSubjectId == s.id) null else s.id },
                                label = { Text(s.code) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutesStr,
                        onValueChange = { minutesStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Duration (mins)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Target Exercises") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                errorMsg = "Task title cannot be empty."
                                return@Button
                            }
                            val mins = minutesStr.toIntOrNull()?.coerceIn(5, 720) ?: 30
                            onSave(title.trim(), selectedSubjectId, date.trim(), mins, notes.trim().ifBlank { null })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.testTag("save_study_task_button")
                    ) {
                        Text("Save Task")
                    }
                }
            }
        }
    }
}
