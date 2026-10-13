package com.example.ui.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Exam
import com.example.data.model.Subject
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentLifeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsScreen(
    viewModel: StudentLifeViewModel
) {
    val exams by viewModel.exams.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExam by remember { mutableStateOf<Exam?>(null) }
    var examToDelete by remember { mutableStateOf<Exam?>(null) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    // Conflict detection: map date -> exams. If on any date, 2 exams overlap in time, flag them!
    val conflictingExamIds = remember(exams) {
        val conflicts = mutableSetOf<String>()
        val byDate = exams.groupBy { it.examDate }
        for ((_, list) in byDate) {
            if (list.size > 1) {
                for (i in 0 until list.size) {
                    for (j in i + 1 until list.size) {
                        val e1 = list[i]
                        val e2 = list[j]
                        // Simple overlap check: e1.start < e2.end && e2.start < e1.end
                        if (e1.startTime < e2.endTime && e2.startTime < e1.endTime) {
                            conflicts.add(e1.id)
                            conflicts.add(e2.id)
                        }
                    }
                }
            }
        }
        conflicts
    }

    val sortedExams = remember(exams) {
        exams.sortedWith(compareBy({ it.examDate }, { it.startTime }))
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("exams_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Exam Timetable (${exams.size})", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_exam_top_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Exam")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_exam")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Exam")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (conflictingExamIds.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberContainerLight)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AmberContainerDark
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Schedule Conflict Detected! Multiple exams have overlapping times on the same date.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = AmberContainerDark
                        )
                    }
                }
            }

            if (sortedExams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No exams scheduled yet.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text("Add Examination Slot")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedExams, key = { it.id }) { exam ->
                        val sub = subjects.find { it.id == exam.subjectId }
                        val isConflict = conflictingExamIds.contains(exam.id)
                        ExamCard(
                            exam = exam,
                            subject = sub,
                            isConflict = isConflict,
                            isPast = exam.examDate < todayStr,
                            onEdit = { editingExam = exam },
                            onDelete = { examToDelete = exam }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        ExamDialog(
            exam = null,
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onSave = { title, subId, date, start, end, type, venue, notes ->
                viewModel.saveExam(
                    title = title,
                    subjectId = subId,
                    examDate = date,
                    startTime = start,
                    endTime = end,
                    examType = type,
                    venue = venue,
                    notes = notes
                )
                showAddDialog = false
            }
        )
    }

    editingExam?.let { exam ->
        ExamDialog(
            exam = exam,
            subjects = subjects,
            onDismiss = { editingExam = null },
            onSave = { title, subId, date, start, end, type, venue, notes ->
                viewModel.saveExam(
                    id = exam.id,
                    title = title,
                    subjectId = subId,
                    examDate = date,
                    startTime = start,
                    endTime = end,
                    examType = type,
                    venue = venue,
                    notes = notes
                )
                editingExam = null
            }
        )
    }

    examToDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = { Text("Delete Exam") },
            text = { Text("Are you sure you want to delete '${exam.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExam(exam.id)
                        examToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseDanger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { examToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ExamCard(
    exam: Exam,
    subject: Subject?,
    isConflict: Boolean,
    isPast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_card_${exam.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConflict) AmberContainerLight.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isConflict) RoseContainerLight else IndigoContainerLight
                    ) {
                        Text(
                            text = if (isConflict) "⚠️ CONFLICT" else exam.examType,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isConflict) RoseContainerDark else IndigoDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subject?.code ?: "Subject",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = IndigoPrimary
                    )
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseDanger, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = exam.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = exam.examDate,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${exam.startTime} - ${exam.endTime}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!exam.venue.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Venue: ${exam.venue}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!exam.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: ${exam.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ExamDialog(
    exam: Exam?,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (title: String, subjectId: String, date: String, start: String, end: String, type: String, venue: String?, notes: String?) -> Unit
) {
    var title by remember { mutableStateOf(exam?.title ?: "") }
    var selectedSubjectId by remember { mutableStateOf(exam?.subjectId ?: subjects.firstOrNull()?.id ?: "") }
    var examDate by remember { mutableStateOf(exam?.examDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var startTime by remember { mutableStateOf(exam?.startTime ?: "09:30") }
    var endTime by remember { mutableStateOf(exam?.endTime ?: "12:30") }
    var examType by remember { mutableStateOf(exam?.examType ?: "MID_SEM") }
    var venue by remember { mutableStateOf(exam?.venue ?: "") }
    var notes by remember { mutableStateOf(exam?.notes ?: "") }
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
                Text(
                    text = if (exam == null) "Schedule Exam" else "Edit Exam Slot",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (errorMsg != null) {
                    Text(text = errorMsg ?: "", color = RoseDanger, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Exam Title * (e.g. Midterm Test 1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("exam_title_input")
                )

                if (subjects.isNotEmpty()) {
                    Text("Subject *", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(subjects) { s ->
                            FilterChip(
                                selected = selectedSubjectId == s.id,
                                onClick = { selectedSubjectId = s.id },
                                label = { Text(s.code) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = examDate,
                    onValueChange = { examDate = it },
                    label = { Text("Exam Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:mm)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Exam Type", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("INTERNAL", "MID_SEM", "END_SEM", "PRACTICAL", "VIVA", "OTHER")) { t ->
                        FilterChip(
                            selected = examType == t,
                            onClick = { examType = t },
                            label = { Text(t.replace("_", " ")) }
                        )
                    }
                }

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Hall Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Preparation notes / syllabus") },
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
                            if (title.isBlank() || selectedSubjectId.isBlank() || examDate.isBlank()) {
                                errorMsg = "Title, Subject, and Date are required."
                                return@Button
                            }
                            onSave(title.trim(), selectedSubjectId, examDate.trim(), startTime.trim(), endTime.trim(), examType, venue.trim().ifBlank { null }, notes.trim().ifBlank { null })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.testTag("save_exam_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
