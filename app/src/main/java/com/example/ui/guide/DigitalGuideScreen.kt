package com.example.ui.guide

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.StudentLifeViewModel

data class FaqItem(
    val question: String,
    val answer: String,
    val category: String
)

data class GuideSection(
    val title: String,
    val subtitle: String,
    val content: String,
    val formulaExplanation: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalGuideScreen(
    viewModel: StudentLifeViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showSupportDialog by remember { mutableStateOf(false) }

    val sections = listOf(
        GuideSection(
            title = "1. Getting Started",
            subtitle = "Quick onboarding guide",
            content = "1. Set up your student profile with your college name and current semester in Settings.\n" +
                    "2. Add your subjects under 'Subjects' with credits and required attendance target.\n" +
                    "3. Mark attendance after every class with a single tap (Present / Absent).\n" +
                    "4. Add assignments and exam dates to stay ahead of deadlines.\n" +
                    "5. Use the Pomodoro focus timer to plan daily study tasks."
        ),
        GuideSection(
            title = "2. Attendance Management & Projections",
            subtitle = "Formulas & projection simulator",
            content = "Attendance Percentage is calculated as: (Classes Attended / Total Conducted) * 100.\n" +
                    "When your attendance drops below your target, the app projects the exact number of consecutive classes required.",
            formulaExplanation = "Projection Formula:\n(A + x) / (T + x) >= p\n" +
                    "Where A = classes attended, T = total conducted, p = target decimal (e.g. 0.75 for 75%), x = consecutive classes needed.\n" +
                    "Example: Attended 26, Total 40, Target 75% -> Need 16 consecutive attended classes with 0 absences to reach 75%."
        ),
        GuideSection(
            title = "3. Assignments & Deadlines",
            subtitle = "Priorities, statuses & countdowns",
            content = "Organize coursework by Priority (Low, Medium, High, Urgent) and Status (Not Started, In Progress, Submitted, Completed). " +
                    "Overdue tasks are flagged in red. Mark tasks as 'In Progress' when you start working, and 'Submitted' when handed in."
        ),
        GuideSection(
            title = "4. Examination Timetable & Conflict Detection",
            subtitle = "Countdown & schedule safety",
            content = "Keep track of all internal, mid-semester, end-semester, and viva dates. " +
                    "The timetable automatically calculates countdowns. If two exams share the same date and overlapping hours, a 'Conflict Detected' alert is prominently displayed."
        ),
        GuideSection(
            title = "5. Study Planner & Pomodoro Timer",
            subtitle = "Scientific 25/5 study intervals",
            content = "The Study Planner features an integrated Pomodoro timer with 25-minute focus intervals followed by 5-minute restorative breaks. " +
                    "Track completed sessions and attach study goals to individual subjects."
        ),
        GuideSection(
            title = "6. SGPA & CGPA Calculators",
            subtitle = "Credit-weighted calculations",
            content = "Calculate your Semester Grade Point Average (SGPA) and Cumulative GPA (CGPA) with support for 10-Point and 4-Point university scales.",
            formulaExplanation = "SGPA = Sum(Course Credits * Grade Points) / Sum(Course Credits)\n" +
                    "CGPA = Sum(Semester Credits * SGPA) / Sum(Semester Credits)\n" +
                    "Results are stored in your private Firestore database for cross-device tracking."
        ),
        GuideSection(
            title = "7. EduMate AI Assistant",
            subtitle = "Powered by Gemini with Search Grounding",
            content = "EduMate answers conceptual questions, outlines study timetables, and explains difficult academic topics. " +
                    "Toggle between Gemini 3.5 Flash (with Google Search grounding), Gemini 3.1 Pro (High Thinking reasoning), and Gemini 3.1 Flash Lite (ultra-fast responses)."
        )
    )

    val faqs = listOf(
        FaqItem("How is attendance calculated?", "Attendance Percentage = (Classes Attended / Total Classes) * 100. Classes marked 'Cancelled' do not penalize your record.", "ATTENDANCE"),
        FaqItem("How many consecutive classes do I need to reach 75%?", "The app calculates this dynamically. If you attended 26 out of 40 classes, you need 16 consecutive attended classes.", "ATTENDANCE"),
        FaqItem("Can I change an attendance mistake?", "Yes! Open the History tab on the Attendance screen to view and delete any erroneous records.", "ATTENDANCE"),
        FaqItem("How does the exam conflict detector work?", "If two scheduled exams share the exact same date and have overlapping time windows, both exams are highlighted with an amber conflict warning.", "EXAMS"),
        FaqItem("Is my data synced across devices?", "Yes. All profile settings, subjects, attendance, and coursework are securely synchronized to Firebase Firestore under your authenticated Google account.", "ACCOUNT"),
        FaqItem("How do I calculate CGPA across multiple semesters?", "Navigate to CGPA Calculator, compute each semester SGPA, and tap 'Save Result'. The cumulative CGPA updates automatically based on credit weights.", "GPA"),
        FaqItem("How do I contact support or report a bug?", "Tap 'Contact Support & Feedback' at the bottom of this screen to submit a ticket directly to the development team.", "SUPPORT")
    )

    val filteredSections = remember(sections, searchQuery) {
        if (searchQuery.isBlank()) sections
        else sections.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true) ||
                    (it.formulaExplanation?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    val filteredFaqs = remember(faqs, searchQuery, selectedCategory) {
        faqs.filter { faq ->
            val matchesQuery = searchQuery.isBlank() ||
                    faq.question.contains(searchQuery, ignoreCase = true) ||
                    faq.answer.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == "ALL" || faq.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("digital_guide_screen"),
        topBar = {
            TopAppBar(
                title = { Text("Digital User Guide", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { showSupportDialog = true }) {
                        Text("Support")
                    }
                }
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
            // Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = IndigoContainerLight)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Student Life Manager — Digital User Guide",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = IndigoDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Everything you need to know to manage your college life.",
                            style = MaterialTheme.typography.bodySmall,
                            color = IndigoDark
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search guide and FAQs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("guide_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Documentation Sections
            item {
                Text(
                    text = "Feature Documentation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(filteredSections) { sec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = sec.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = IndigoPrimary
                        )
                        Text(
                            text = sec.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = sec.content,
                            style = MaterialTheme.typography.bodySmall
                        )
                        sec.formulaExplanation?.let { formula ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = formula,
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // FAQ Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Frequently Asked Questions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(filteredFaqs) { faq ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = faq.question,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = IndigoPrimary
                            )
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = faq.answer,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Contact Support Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { showSupportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Support & Submit Feedback")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showSupportDialog) {
        SupportDialog(
            onDismiss = { showSupportDialog = false },
            onSubmit = { type, subject, message, rating ->
                viewModel.submitSupportTicket(type, subject, message, rating)
                showSupportDialog = false
            }
        )
    }
}

@Composable
fun SupportDialog(
    onDismiss: () -> Unit,
    onSubmit: (type: String, subject: String, message: String, rating: Int?) -> Unit
) {
    var type by remember { mutableStateOf("FEEDBACK") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var rating by remember { mutableIntStateOf(5) }
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
                Text("Support & Feedback", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

                if (errorMsg != null) {
                    Text(text = errorMsg ?: "", color = RoseDanger, style = MaterialTheme.typography.bodySmall)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("FEEDBACK", "SUPPORT", "BUG_REPORT").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.replace("_", " ")) }
                        )
                    }
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Topic / Subject *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message / Problem Description *") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rating: ", style = MaterialTheme.typography.labelMedium)
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = AmberWarning
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (subject.isBlank() || message.isBlank()) {
                                errorMsg = "Subject and Message cannot be empty."
                                return@Button
                            }
                            onSubmit(type, subject.trim(), message.trim(), rating)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                    ) {
                        Text("Submit Ticket")
                    }
                }
            }
        }
    }
}
