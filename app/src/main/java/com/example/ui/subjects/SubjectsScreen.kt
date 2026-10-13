package com.example.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.Subject
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentLifeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectsScreen(
    viewModel: StudentLifeViewModel
) {
    val subjects by viewModel.subjects.collectAsState()
    val subjectAttendanceMap by viewModel.subjectAttendanceMap.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedSemesterFilter by remember { mutableStateOf<Int?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<Subject?>(null) }
    var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

    val filteredSubjects = remember(subjects, searchQuery, selectedSemesterFilter) {
        subjects.filter { sub ->
            val matchesQuery = sub.name.contains(searchQuery, ignoreCase = true) ||
                    sub.code.contains(searchQuery, ignoreCase = true) ||
                    (sub.facultyName?.contains(searchQuery, ignoreCase = true) == true)
            val matchesSem = selectedSemesterFilter == null || sub.semester == selectedSemesterFilter
            matchesQuery && matchesSem
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("subjects_screen"),
        topBar = {
            TopAppBar(
                title = { Text("My Subjects (${subjects.size})", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_subject_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subject")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_subject")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, code, or faculty...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("subject_search_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Semester Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSemesterFilter == null,
                    onClick = { selectedSemesterFilter = null },
                    label = { Text("All Semesters") }
                )
                (1..8).forEach { sem ->
                    if (subjects.any { it.semester == sem }) {
                        FilterChip(
                            selected = selectedSemesterFilter == sem,
                            onClick = { selectedSemesterFilter = if (selectedSemesterFilter == sem) null else sem },
                            label = { Text("Sem $sem") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredSubjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (subjects.isEmpty()) "No subjects added yet." else "No subjects match your filter.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text("Add Your First Subject")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredSubjects, key = { it.id }) { subject ->
                        val stats = subjectAttendanceMap[subject.id]
                        SubjectCard(
                            subject = subject,
                            stats = stats,
                            onEdit = { editingSubject = subject },
                            onDelete = { subjectToDelete = subject }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        SubjectDialog(
            subject = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, code, sem, credits, faculty, minAtt, desc, color ->
                viewModel.saveSubject(
                    name = name,
                    code = code,
                    semester = sem,
                    credits = credits,
                    facultyName = faculty,
                    minAttendance = minAtt,
                    description = desc,
                    colorHex = color
                )
                showAddDialog = false
            }
        )
    }

    editingSubject?.let { sub ->
        SubjectDialog(
            subject = sub,
            onDismiss = { editingSubject = null },
            onSave = { name, code, sem, credits, faculty, minAtt, desc, color ->
                viewModel.saveSubject(
                    id = sub.id,
                    name = name,
                    code = code,
                    semester = sem,
                    credits = credits,
                    facultyName = faculty,
                    minAttendance = minAtt,
                    description = desc,
                    colorHex = color
                )
                editingSubject = null
            }
        )
    }

    subjectToDelete?.let { sub ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete Subject") },
            text = { Text("Are you sure you want to delete ${sub.name} (${sub.code})? This will also remove its attendance association.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSubject(sub.id)
                        subjectToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseDanger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SubjectCard(
    subject: Subject,
    stats: com.example.util.AttendanceStats?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_card_${subject.code}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(subject.colorHex))
                                } catch (e: Exception) {
                                    IndigoPrimary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = subject.code,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = IndigoPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Sem ${subject.semester} • ${subject.credits} Credits",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = RoseDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subject.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            if (!subject.facultyName.isNullOrBlank()) {
                Text(
                    text = "Faculty: ${subject.facultyName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val currentPercentage = stats?.percentage ?: 0.0
            val meetsTarget = currentPercentage >= subject.minAttendance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Attendance: ${String.format("%.1f", currentPercentage)}% (Target: ${subject.minAttendance}%)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (meetsTarget) EmeraldAccent else RoseDanger
                    )
                    Text(
                        text = "${stats?.attended ?: 0} attended / ${stats?.totalConducted ?: 0} total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (stats != null && stats.requiredConsecutiveClasses > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AmberContainerLight
                    ) {
                        Text(
                            text = "Need ${stats.requiredConsecutiveClasses} more",
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

@Composable
fun SubjectDialog(
    subject: Subject?,
    onDismiss: () -> Unit,
    onSave: (name: String, code: String, sem: Int, credits: Int, faculty: String?, minAtt: Int, desc: String?, color: String) -> Unit
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var code by remember { mutableStateOf(subject?.code ?: "") }
    var semesterStr by remember { mutableStateOf(subject?.semester?.toString() ?: "1") }
    var creditsStr by remember { mutableStateOf(subject?.credits?.toString() ?: "3") }
    var faculty by remember { mutableStateOf(subject?.facultyName ?: "") }
    var minAttendanceStr by remember { mutableStateOf(subject?.minAttendance?.toString() ?: "75") }
    var description by remember { mutableStateOf(subject?.description ?: "") }
    var colorHex by remember { mutableStateOf(subject?.colorHex ?: "#4F46E5") }

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
                    text = if (subject == null) "Add New Subject" else "Edit Subject",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (errorMsg != null) {
                    Text(text = errorMsg ?: "", color = RoseDanger, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Code * (e.g. CS101)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("subject_code_input")
                    )
                    OutlinedTextField(
                        value = semesterStr,
                        onValueChange = { semesterStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Semester (1-12)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = creditsStr,
                        onValueChange = { creditsStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Credits *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minAttendanceStr,
                        onValueChange = { minAttendanceStr = it.filter { char -> char.isDigit() } },
                        label = { Text("Min Attendance %") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Faculty / Professor Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Syllabus notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || code.isBlank()) {
                                errorMsg = "Name and Code are required."
                                return@Button
                            }
                            val sem = semesterStr.toIntOrNull()?.coerceIn(1, 12) ?: 1
                            val credits = creditsStr.toIntOrNull()?.coerceIn(0, 30) ?: 3
                            val minAtt = minAttendanceStr.toIntOrNull()?.coerceIn(1, 100) ?: 75

                            onSave(name.trim(), code.trim(), sem, credits, faculty.trim().ifBlank { null }, minAtt, description.trim().ifBlank { null }, colorHex)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.testTag("save_subject_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
