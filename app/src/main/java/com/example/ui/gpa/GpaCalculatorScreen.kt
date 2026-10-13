package com.example.ui.gpa

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SemesterResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentLifeViewModel
import com.example.util.CourseGrade
import com.example.util.GpaCalculator
import com.example.util.GradingScale
import com.example.util.SemesterGpaResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaCalculatorScreen(
    viewModel: StudentLifeViewModel
) {
    val results by viewModel.results.collectAsState()
    var selectedScale by remember { mutableStateOf(GradingScale.TEN_POINT) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: SGPA Calculator, 1: CGPA Cumulative Tracker
    var showAddSemesterDialog by remember { mutableStateOf(false) }

    // Courses for SGPA calculator
    var semesterCourses by remember {
        mutableStateOf(
            listOf(
                CourseGrade("Course 1", 4, 9.0),
                CourseGrade("Course 2", 4, 8.0),
                CourseGrade("Course 3", 3, 9.0),
                CourseGrade("Course 4", 3, 7.0),
                CourseGrade("Course 5", 2, 10.0)
            )
        )
    }

    val calculatedSgpa = remember(semesterCourses) {
        GpaCalculator.calculateSgpa(semesterCourses)
    }

    val totalSemesterCredits = remember(semesterCourses) {
        semesterCourses.sumOf { it.credits }
    }

    val cumulativeCgpa = remember(results) {
        val converted = results.map { SemesterGpaResult(it.semesterNumber, it.totalCredits, it.sgpa) }
        GpaCalculator.calculateCgpa(converted)
    }

    val totalCumulativeCredits = remember(results) {
        results.sumOf { it.totalCredits }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("gpa_calculator_screen"),
        topBar = {
            TopAppBar(
                title = { Text("SGPA & CGPA Calculator", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = {
                        selectedScale = if (selectedScale == GradingScale.TEN_POINT) GradingScale.FOUR_POINT else GradingScale.TEN_POINT
                    }) {
                        Text(if (selectedScale == GradingScale.TEN_POINT) "10-Pt Scale" else "4-Pt Scale")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Semester SGPA") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Cumulative CGPA (${results.size} Sems)") }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    // SGPA Calculator
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = IndigoContainerLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Calculated Semester GPA",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = IndigoDark
                                )
                                Text(
                                    text = String.format("%.2f", calculatedSgpa),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = IndigoPrimary
                                    )
                                )
                                Text(
                                    text = "Total Credits: $totalSemesterCredits • Scale: ${selectedScale.maxPoint}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndigoDark
                                )
                            }

                            Button(
                                onClick = {
                                    val nextSem = (results.maxOfOrNull { it.semesterNumber } ?: 0) + 1
                                    viewModel.saveSemesterResult(
                                        semesterNumber = nextSem,
                                        totalCredits = totalSemesterCredits,
                                        sgpa = calculatedSgpa,
                                        gradingScale = if (selectedScale == GradingScale.TEN_POINT) "10_POINT" else "4_POINT"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Save Result")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Course Grades (${semesterCourses.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(
                            onClick = {
                                semesterCourses = semesterCourses + CourseGrade("Course ${semesterCourses.size + 1}", 3, 8.0)
                            }
                        ) {
                            Text("+ Add Course")
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(semesterCourses.indices.toList()) { index ->
                            val course = semesterCourses[index]
                            CourseGradeRow(
                                course = course,
                                scale = selectedScale,
                                onUpdate = { updated ->
                                    val mutable = semesterCourses.toMutableList()
                                    mutable[index] = updated
                                    semesterCourses = mutable
                                },
                                onDelete = {
                                    if (semesterCourses.size > 1) {
                                        val mutable = semesterCourses.toMutableList()
                                        mutable.removeAt(index)
                                        semesterCourses = mutable
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                }
                1 -> {
                    // Cumulative CGPA
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldContainerLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Overall Cumulative CGPA",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldContainerDark
                                )
                                Text(
                                    text = if (results.isEmpty()) "--" else String.format("%.2f", cumulativeCgpa),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldContainerDark
                                    )
                                )
                                Text(
                                    text = "Total Completed Credits: $totalCumulativeCredits across ${results.size} semesters",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldContainerDark
                                )
                            }

                            Button(
                                onClick = { showAddSemesterDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                            ) {
                                Text("+ Add Sem")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Recorded Semesters",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (results.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No semester results saved yet. Calculate and save SGPA, or add manually above.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(results.sortedBy { it.semesterNumber }) { res ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Semester ${res.semesterNumber}",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Credits: ${res.totalCredits} • Scale: ${res.gradingScale}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "SGPA: ${String.format("%.2f", res.sgpa)}",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = IndigoPrimary
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(onClick = { viewModel.deleteSemesterResult(res.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = RoseDanger,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(32.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (showAddSemesterDialog) {
        AddSemesterManualDialog(
            nextSemNumber = (results.maxOfOrNull { it.semesterNumber } ?: 0) + 1,
            onDismiss = { showAddSemesterDialog = false },
            onSave = { sem, credits, sgpa ->
                viewModel.saveSemesterResult(
                    semesterNumber = sem,
                    totalCredits = credits,
                    sgpa = sgpa,
                    gradingScale = if (selectedScale == GradingScale.TEN_POINT) "10_POINT" else "4_POINT"
                )
                showAddSemesterDialog = false
            }
        )
    }
}

@Composable
fun CourseGradeRow(
    course: CourseGrade,
    scale: GradingScale,
    onUpdate: (CourseGrade) -> Unit,
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
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = course.courseName,
                onValueChange = { onUpdate(course.copy(courseName = it)) },
                label = { Text("Course") },
                modifier = Modifier.weight(1.5f),
                singleLine = true
            )

            // Credits selector
            OutlinedTextField(
                value = course.credits.toString(),
                onValueChange = { str ->
                    val c = str.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 20) ?: 1
                    onUpdate(course.copy(credits = c))
                },
                label = { Text("Credits") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            // Grade Point
            OutlinedTextField(
                value = course.gradePoint.toString(),
                onValueChange = { str ->
                    val gp = str.toDoubleOrNull()?.coerceIn(0.0, scale.maxPoint) ?: 0.0
                    onUpdate(course.copy(gradePoint = gp))
                },
                label = { Text("Grade Pt") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = RoseDanger,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AddSemesterManualDialog(
    nextSemNumber: Int,
    onDismiss: () -> Unit,
    onSave: (semester: Int, credits: Int, sgpa: Double) -> Unit
) {
    var semStr by remember { mutableStateOf(nextSemNumber.toString()) }
    var creditsStr by remember { mutableStateOf("20") }
    var sgpaStr by remember { mutableStateOf("8.50") }

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
                Text("Add Semester Score", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                OutlinedTextField(
                    value = semStr,
                    onValueChange = { semStr = it.filter { char -> char.isDigit() } },
                    label = { Text("Semester Number (1-12)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = creditsStr,
                    onValueChange = { creditsStr = it.filter { char -> char.isDigit() } },
                    label = { Text("Total Semester Credits") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = sgpaStr,
                    onValueChange = { sgpaStr = it },
                    label = { Text("Semester SGPA / GPA (e.g. 8.75)") },
                    singleLine = true,
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
                            val sem = semStr.toIntOrNull()?.coerceIn(1, 12) ?: 1
                            val credits = creditsStr.toIntOrNull()?.coerceIn(1, 60) ?: 20
                            val sgpa = sgpaStr.toDoubleOrNull()?.coerceIn(0.0, 10.0) ?: 0.0
                            onSave(sem, credits, sgpa)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                    ) {
                        Text("Add Record")
                    }
                }
            }
        }
    }
}
