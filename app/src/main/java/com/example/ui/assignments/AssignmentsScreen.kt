package com.example.ui.assignments

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
import com.example.data.model.Assignment
import com.example.data.model.Subject
import com.example.ui.dashboard.PriorityPill
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentLifeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    viewModel: StudentLifeViewModel
) {
    val assignments by viewModel.assignments.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAssignment by remember { mutableStateOf<Assignment?>(null) }
    var assignmentToDelete by remember { mutableStateOf<Assignment?>(null) }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val filteredAssignments = remember(assignments, selectedStatusFilter, todayStr) {
        when (selectedStatusFilter) {
            "OVERDUE" -> assignments.filter { it.dueDate < todayStr && it.status != "COMPLETED" && it.status != "SUBMITTED" }
            "IN_PROGRESS" -> assignments.filter { it.status == "IN_PROGRESS" }
            "NOT_STARTED" -> assignments.filter { it.status == "NOT_STARTED" }
            "SUBMITTED" -> assignments.filter { it.status == "SUBMITTED" }
            "COMPLETED" -> assignments.filter { it.status == "COMPLETED" }
            else -> assignments
        }.sortedBy { it.dueDate }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("assignments_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Assignments (${assignments.size})", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_assignment_top_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Assignment")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_assignment")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Assignment")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Filter chips
            val filterOptions = listOf(
                "ALL" to "All",
                "NOT_STARTED" to "Not Started",
                "IN_PROGRESS" to "In Progress",
                "SUBMITTED" to "Submitted",
                "COMPLETED" to "Completed",
                "OVERDUE" to "Overdue"
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { (key, label) ->
                    FilterChip(
                        selected = selectedStatusFilter == key,
                        onClick = { selectedStatusFilter = key },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredAssignments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (assignments.isEmpty()) "No assignments logged yet." else "No assignments under this filter.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Text("Create Assignment")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredAssignments, key = { it.id }) { assignment ->
                        val sub = subjects.find { it.id == assignment.subjectId }
                        AssignmentCard(
                            assignment = assignment,
                            subject = sub,
                            isOverdue = assignment.dueDate < todayStr && assignment.status != "COMPLETED" && assignment.status != "SUBMITTED",
                            onStatusChange = { newStatus ->
                                viewModel.updateAssignmentStatus(assignment, newStatus)
                            },
                            onEdit = { editingAssignment = assignment },
                            onDelete = { assignmentToDelete = assignment }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AssignmentDialog(
            assignment = null,
            subjects = subjects,
            onDismiss = { showAddDialog = false },
            onSave = { title, subId, desc, dueDate, priority, status ->
                viewModel.saveAssignment(
                    title = title,
                    subjectId = subId,
                    description = desc,
                    dueDate = dueDate,
                    priority = priority,
                    status = status
                )
                showAddDialog = false
            }
        )
    }

    editingAssignment?.let { assn ->
        AssignmentDialog(
            assignment = assn,
            subjects = subjects,
            onDismiss = { editingAssignment = null },
            onSave = { title, subId, desc, dueDate, priority, status ->
                viewModel.saveAssignment(
                    id = assn.id,
                    title = title,
                    subjectId = subId,
                    description = desc,
                    dueDate = dueDate,
                    priority = priority,
                    status = status
                )
                editingAssignment = null
            }
        )
    }

    assignmentToDelete?.let { assn ->
        AlertDialog(
            onDismissRequest = { assignmentToDelete = null },
            title = { Text("Delete Assignment") },
            text = { Text("Are you sure you want to delete '${assn.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAssignment(assn.id)
                        assignmentToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RoseDanger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { assignmentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AssignmentCard(
    assignment: Assignment,
    subject: Subject?,
    isOverdue: Boolean,
    onStatusChange: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("assignment_card_${assignment.id}"),
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
                    PriorityPill(priority = assignment.priority)
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
                text = assignment.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            if (!assignment.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = assignment.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isOverdue) RoseDanger else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isOverdue) "OVERDUE (${assignment.dueDate})" else "Due: ${assignment.dueDate}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOverdue) RoseDanger else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Status chip / toggle dropdown
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (assignment.status) {
                        "COMPLETED" -> EmeraldContainerLight
                        "SUBMITTED" -> SkyContainerLight
                        "IN_PROGRESS" -> AmberContainerLight
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = assignment.status.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = when (assignment.status) {
                                "COMPLETED" -> EmeraldContainerDark
                                "SUBMITTED" -> SkyContainerDark
                                "IN_PROGRESS" -> AmberContainerDark
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: quickly mark completed or submitted
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (assignment.status != "IN_PROGRESS" && assignment.status != "COMPLETED") {
                    OutlinedButton(
                        onClick = { onStatusChange("IN_PROGRESS") },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("Start", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (assignment.status != "SUBMITTED" && assignment.status != "COMPLETED") {
                    Button(
                        onClick = { onStatusChange("SUBMITTED") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SkySecondary),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("Submit", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (assignment.status != "COMPLETED") {
                    Button(
                        onClick = { onStatusChange("COMPLETED") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("Complete", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun AssignmentDialog(
    assignment: Assignment?,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (title: String, subjectId: String, desc: String?, dueDate: String, priority: String, status: String) -> Unit
) {
    var title by remember { mutableStateOf(assignment?.title ?: "") }
    var selectedSubjectId by remember { mutableStateOf(assignment?.subjectId ?: subjects.firstOrNull()?.id ?: "") }
    var description by remember { mutableStateOf(assignment?.description ?: "") }
    var dueDate by remember { mutableStateOf(assignment?.dueDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var priority by remember { mutableStateOf(assignment?.priority ?: "MEDIUM") }
    var status by remember { mutableStateOf(assignment?.status ?: "NOT_STARTED") }
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
                    text = if (assignment == null) "New Assignment" else "Edit Assignment",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                if (errorMsg != null) {
                    Text(text = errorMsg ?: "", color = RoseDanger, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("assignment_title_input")
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
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (YYYY-MM-DD) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("assignment_due_date_input")
                )

                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("LOW", "MEDIUM", "HIGH", "URGENT").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Requirements") },
                    maxLines = 3,
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
                            if (title.isBlank() || selectedSubjectId.isBlank() || dueDate.isBlank()) {
                                errorMsg = "Title, Subject, and Due Date are required."
                                return@Button
                            }
                            onSave(title.trim(), selectedSubjectId, description.trim().ifBlank { null }, dueDate.trim(), priority, status)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        modifier = Modifier.testTag("save_assignment_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
