package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.subjects.SubjectsScreen
import com.example.data.repository.StudentLifeRepository
import com.example.ui.assignments.AssignmentsScreen
import com.example.ui.attendance.AttendanceScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.chat.GeminiChatScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.exams.ExamsScreen
import com.example.ui.gpa.GpaCalculatorScreen
import com.example.ui.guide.DigitalGuideScreen
import com.example.ui.notes.NotesScreen
import com.example.ui.planner.StudyPlannerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.viewmodel.StudentLifeViewModel
import com.example.ui.viewmodel.ViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

enum class Screen(val title: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard),
    SUBJECTS("Subjects", Icons.Default.MenuBook),
    ATTENDANCE("Attendance", Icons.Default.FactCheck),
    ASSIGNMENTS("Assignments", Icons.Default.Assignment),
    EXAMS("Exams", Icons.Default.Event),
    PLANNER("Planner", Icons.Default.Timer),
    GPA("CGPA", Icons.Default.Calculate),
    NOTES("Notes", Icons.Default.Description),
    CHAT("EduMate AI", Icons.Default.AutoAwesome),
    GUIDE("Guide", Icons.Default.HelpOutline),
    SETTINGS("Settings", Icons.Default.Settings)
}

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation() {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            onAuthSuccess = { /* authStateFlow will reactively update currentUser */ }
        )
    } else {
        MainAppShell(user = user)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { StudentLifeRepository(context) }
    val viewModel: StudentLifeViewModel = viewModel(
        factory = ViewModelFactory(repository, user.uid)
    )

    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }
    val actionMessage by viewModel.actionMessage.collectAsState()

    var showMoreSheet by remember { mutableStateOf(false) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionMessage()
        }
    }

    // Back handling: if not on DASHBOARD, back navigates to DASHBOARD
    BackHandler(enabled = currentScreen != Screen.DASHBOARD) {
        currentScreen = Screen.DASHBOARD
    }

    val primaryNavScreens = listOf(
        Screen.DASHBOARD,
        Screen.ATTENDANCE,
        Screen.ASSIGNMENTS,
        Screen.CHAT
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_app_shell"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                primaryNavScreens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }

                // "More" item
                val isMoreSelected = currentScreen !in primaryNavScreens
                NavigationBarItem(
                    selected = isMoreSelected,
                    onClick = { showMoreSheet = true },
                    icon = {
                        Icon(
                            imageVector = if (isMoreSelected) currentScreen.icon else Icons.Default.MoreHoriz,
                            contentDescription = "More Features"
                        )
                    },
                    label = { Text(if (isMoreSelected) currentScreen.title else "More") },
                    modifier = Modifier.testTag("nav_item_more")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSubjects = { currentScreen = Screen.SUBJECTS },
                    onNavigateToAttendance = { currentScreen = Screen.ATTENDANCE },
                    onNavigateToAssignments = { currentScreen = Screen.ASSIGNMENTS },
                    onNavigateToExams = { currentScreen = Screen.EXAMS },
                    onNavigateToPlanner = { currentScreen = Screen.PLANNER },
                    onNavigateToChat = { currentScreen = Screen.CHAT },
                    onNavigateToGpa = { currentScreen = Screen.GPA }
                )
                Screen.SUBJECTS -> SubjectsScreen(viewModel = viewModel)
                Screen.ATTENDANCE -> AttendanceScreen(viewModel = viewModel)
                Screen.ASSIGNMENTS -> AssignmentsScreen(viewModel = viewModel)
                Screen.EXAMS -> ExamsScreen(viewModel = viewModel)
                Screen.PLANNER -> StudyPlannerScreen(viewModel = viewModel)
                Screen.GPA -> GpaCalculatorScreen(viewModel = viewModel)
                Screen.NOTES -> NotesScreen(viewModel = viewModel)
                Screen.CHAT -> GeminiChatScreen(viewModel = viewModel)
                Screen.GUIDE -> DigitalGuideScreen(viewModel = viewModel)
                Screen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onSignOut = {
                        scope.launch {
                            FirebaseAuth.getInstance().signOut()
                            try {
                                CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    }
                )
            }
        }

        if (showMoreSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMoreSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "More Features",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val moreScreens = listOf(
                        Screen.SUBJECTS to "Academic Subjects & Courses",
                        Screen.EXAMS to "Examination Timetable & Conflicts",
                        Screen.PLANNER to "Study Planner & Pomodoro Timer",
                        Screen.GPA to "SGPA & CGPA Calculators",
                        Screen.NOTES to "Subject Notes & Study Docs",
                        Screen.GUIDE to "Digital User Guide & Help Center",
                        Screen.SETTINGS to "Profile, Targets & Settings"
                    )

                    moreScreens.forEach { (screen, desc) ->
                        Surface(
                            onClick = {
                                currentScreen = screen
                                showMoreSheet = false
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.IndigoPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// Helper alias
private val Screen.ATTANCE_SAFE: Screen get() = Screen.ATTENDANCE
