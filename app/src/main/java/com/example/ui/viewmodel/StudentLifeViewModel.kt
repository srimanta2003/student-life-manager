package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.gemini.GeminiModelMode
import com.example.data.gemini.GeminiService
import com.example.data.model.*
import com.example.data.repository.StudentLifeRepository
import com.example.util.AttendanceCalculator
import com.example.util.AttendanceStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardSummary(
    val totalSubjects: Int = 0,
    val overallAttendanceStats: AttendanceStats? = null,
    val pendingAssignmentsCount: Int = 0,
    val upcomingExamsCount: Int = 0,
    val todayStudyTasksCount: Int = 0,
    val completedStudyTasksToday: Int = 0
)

data class PomodoroState(
    val isRunning: Boolean = false,
    val isBreak: Boolean = false,
    val remainingSeconds: Int = 25 * 60,
    val totalSeconds: Int = 25 * 60,
    val sessionsCompleted: Int = 0
)

class StudentLifeViewModel(
    private val repository: StudentLifeRepository,
    val currentUserId: String
) : ViewModel() {

    // Profile
    val profile: StateFlow<UserProfile?> = repository.observeProfile(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    // Subjects
    val subjects: StateFlow<List<Subject>> = repository.observeSubjects(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Attendance
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = repository.observeAttendance(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Assignments
    val assignments: StateFlow<List<Assignment>> = repository.observeAssignments(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Exams
    val exams: StateFlow<List<Exam>> = repository.observeExams(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Study Tasks
    val studyTasks: StateFlow<List<StudyTask>> = repository.observeStudyTasks(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Notes
    val notes: StateFlow<List<Note>> = repository.observeNotes(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Results (SGPA / CGPA)
    val results: StateFlow<List<SemesterResult>> = repository.observeResults(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Subject-wise attendance calculation
    val subjectAttendanceMap: StateFlow<Map<String, AttendanceStats>> = combine(
        subjects,
        attendanceRecords
    ) { subList, attList ->
        subList.associate { sub ->
            val records = attList.filter { it.subjectId == sub.id }
            val present = records.count { it.status == "PRESENT" }
            val absent = records.count { it.status == "ABSENT" }
            val cancelled = records.count { it.status == "CANCELLED" }
            sub.id to AttendanceCalculator.computeStats(present, absent, cancelled, sub.minAttendance)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    // Dashboard summary
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        subjects,
        attendanceRecords,
        assignments,
        exams,
        studyTasks
    ) { subs, atts, assns, exms, tasks ->
        val totalPresent = atts.count { it.status == "PRESENT" }
        val totalAbsent = atts.count { it.status == "ABSENT" }
        val totalCancelled = atts.count { it.status == "CANCELLED" }
        val overallStats = if (subs.isNotEmpty() || atts.isNotEmpty()) {
            AttendanceCalculator.computeStats(totalPresent, totalAbsent, totalCancelled, 75)
        } else null

        val pendingAssns = assns.count { it.status != "COMPLETED" && it.status != "SUBMITTED" }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val upcomingExams = exms.count { it.examDate >= todayStr }
        val todayTasks = tasks.filter { it.date == todayStr }

        DashboardSummary(
            totalSubjects = subs.size,
            overallAttendanceStats = overallStats,
            pendingAssignmentsCount = pendingAssns,
            upcomingExamsCount = upcomingExams,
            todayStudyTasksCount = todayTasks.size,
            completedStudyTasksToday = todayTasks.count { it.isCompleted }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DashboardSummary())

    // Pomodoro Timer State
    private val _pomodoro = MutableStateFlow(PomodoroState())
    val pomodoro: StateFlow<PomodoroState> = _pomodoro.asStateFlow()
    private var timerJob: Job? = null

    // Gemini Chat
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = ChatSender.AI,
                text = "Hello! I am EduMate, your academic tutor and college assistant. How can I help you study, plan, or prepare for exams today?",
                modelUsed = "gemini-3.5-flash"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _chatModelMode = MutableStateFlow(GeminiModelMode.GENERAL_FLASH)
    val chatModelMode: StateFlow<GeminiModelMode> = _chatModelMode.asStateFlow()

    private val _searchGroundingEnabled = MutableStateFlow(true)
    val searchGroundingEnabled: StateFlow<Boolean> = _searchGroundingEnabled.asStateFlow()

    private val _mapsGroundingEnabled = MutableStateFlow(false)
    val mapsGroundingEnabled: StateFlow<Boolean> = _mapsGroundingEnabled.asStateFlow()

    // Status snackbar / feedback
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun setChatModelMode(mode: GeminiModelMode) {
        _chatModelMode.value = mode
    }

    fun toggleSearchGrounding(enabled: Boolean) {
        _searchGroundingEnabled.value = enabled
    }

    fun toggleMapsGrounding(enabled: Boolean) {
        _mapsGroundingEnabled.value = enabled
    }

    // CRUD: Profile
    fun saveProfile(
        fullName: String,
        email: String,
        college: String?,
        course: String?,
        department: String?,
        currentSemester: Int,
        academicYear: String,
        targetAttendance: Int,
        themeMode: String = "system"
    ) {
        viewModelScope.launch {
            val p = UserProfile(
                userId = currentUserId,
                fullName = fullName,
                email = email,
                college = college,
                course = course,
                department = department,
                currentSemester = currentSemester,
                academicYear = academicYear,
                targetAttendance = targetAttendance,
                themeMode = themeMode,
                onboardingCompleted = true
            )
            repository.saveProfile(p).onSuccess {
                _actionMessage.value = "Profile updated successfully"
            }.onFailure {
                _actionMessage.value = "Failed to update profile: ${it.message}"
            }
        }
    }

    // CRUD: Subjects
    fun saveSubject(
        id: String = "",
        name: String,
        code: String,
        semester: Int,
        credits: Int,
        facultyName: String?,
        minAttendance: Int,
        description: String?,
        colorHex: String
    ) {
        viewModelScope.launch {
            val sub = Subject(
                id = id,
                userId = currentUserId,
                name = name,
                code = code,
                semester = semester,
                credits = credits,
                facultyName = facultyName,
                minAttendance = minAttendance,
                description = description,
                colorHex = colorHex
            )
            repository.saveSubject(sub).onSuccess {
                _actionMessage.value = if (id.isBlank()) "Subject added" else "Subject updated"
            }.onFailure {
                _actionMessage.value = "Error saving subject: ${it.message}"
            }
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteSubject(currentUserId, subjectId).onSuccess {
                _actionMessage.value = "Subject deleted"
            }
        }
    }

    // CRUD: Attendance
    fun markAttendance(
        subjectId: String,
        status: String,
        sessionType: String = "LECTURE",
        note: String? = null,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) {
        viewModelScope.launch {
            val record = AttendanceRecord(
                userId = currentUserId,
                subjectId = subjectId,
                date = date,
                status = status,
                sessionType = sessionType,
                note = note
            )
            repository.recordAttendance(record).onSuccess {
                _actionMessage.value = "Attendance marked as $status"
            }.onFailure {
                _actionMessage.value = "Failed to record attendance: ${it.message}"
            }
        }
    }

    fun deleteAttendance(attendanceId: String) {
        viewModelScope.launch {
            repository.deleteAttendance(currentUserId, attendanceId).onSuccess {
                _actionMessage.value = "Attendance record deleted"
            }
        }
    }

    // CRUD: Assignments
    fun saveAssignment(
        id: String = "",
        subjectId: String,
        title: String,
        description: String?,
        dueDate: String,
        priority: String,
        status: String = "NOT_STARTED"
    ) {
        viewModelScope.launch {
            val a = Assignment(
                id = id,
                userId = currentUserId,
                subjectId = subjectId,
                title = title,
                description = description,
                dueDate = dueDate,
                priority = priority,
                status = status
            )
            repository.saveAssignment(a).onSuccess {
                _actionMessage.value = if (id.isBlank()) "Assignment created" else "Assignment updated"
            }.onFailure {
                _actionMessage.value = "Error saving assignment: ${it.message}"
            }
        }
    }

    fun updateAssignmentStatus(assignment: Assignment, newStatus: String) {
        viewModelScope.launch {
            val updated = assignment.copy(status = newStatus)
            repository.saveAssignment(updated)
        }
    }

    fun deleteAssignment(assignmentId: String) {
        viewModelScope.launch {
            repository.deleteAssignment(currentUserId, assignmentId).onSuccess {
                _actionMessage.value = "Assignment deleted"
            }
        }
    }

    // CRUD: Exams
    fun saveExam(
        id: String = "",
        subjectId: String,
        title: String,
        examDate: String,
        startTime: String,
        endTime: String,
        examType: String,
        venue: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val e = Exam(
                id = id,
                userId = currentUserId,
                subjectId = subjectId,
                title = title,
                examDate = examDate,
                startTime = startTime,
                endTime = endTime,
                examType = examType,
                venue = venue,
                notes = notes
            )
            repository.saveExam(e).onSuccess {
                _actionMessage.value = if (id.isBlank()) "Exam scheduled" else "Exam updated"
            }.onFailure {
                _actionMessage.value = "Error saving exam: ${it.message}"
            }
        }
    }

    fun deleteExam(examId: String) {
        viewModelScope.launch {
            repository.deleteExam(currentUserId, examId).onSuccess {
                _actionMessage.value = "Exam deleted"
            }
        }
    }

    // CRUD: Study Tasks
    fun saveStudyTask(
        id: String = "",
        subjectId: String?,
        title: String,
        date: String,
        targetMinutes: Int,
        completedMinutes: Int = 0,
        isCompleted: Boolean = false,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val task = StudyTask(
                id = id,
                userId = currentUserId,
                subjectId = subjectId,
                title = title,
                date = date,
                targetMinutes = targetMinutes,
                completedMinutes = completedMinutes,
                isCompleted = isCompleted,
                notes = notes
            )
            repository.saveStudyTask(task).onSuccess {
                _actionMessage.value = if (id.isBlank()) "Task added" else "Task updated"
            }
        }
    }

    fun toggleStudyTaskCompletion(task: StudyTask) {
        viewModelScope.launch {
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.saveStudyTask(updated)
        }
    }

    fun deleteStudyTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteStudyTask(currentUserId, taskId).onSuccess {
                _actionMessage.value = "Task deleted"
            }
        }
    }

    // Pomodoro Actions
    fun startPomodoro() {
        if (_pomodoro.value.isRunning) return
        _pomodoro.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (_pomodoro.value.remainingSeconds > 0 && _pomodoro.value.isRunning) {
                delay(1000L)
                _pomodoro.update { it.copy(remainingSeconds = it.remainingSeconds - 1) }
            }
            if (_pomodoro.value.remainingSeconds <= 0) {
                // Switch between Focus (25m) and Break (5m)
                val wasBreak = _pomodoro.value.isBreak
                val nextIsBreak = !wasBreak
                val nextDuration = if (nextIsBreak) 5 * 60 else 25 * 60
                val sessions = if (wasBreak) _pomodoro.value.sessionsCompleted else _pomodoro.value.sessionsCompleted + 1
                _pomodoro.value = PomodoroState(
                    isRunning = false,
                    isBreak = nextIsBreak,
                    remainingSeconds = nextDuration,
                    totalSeconds = nextDuration,
                    sessionsCompleted = sessions
                )
                _actionMessage.value = if (nextIsBreak) "Focus session complete! Take a 5-minute break." else "Break finished! Ready for next focus session."
            }
        }
    }

    fun pausePomodoro() {
        _pomodoro.update { it.copy(isRunning = false) }
        timerJob?.cancel()
    }

    fun resetPomodoro() {
        timerJob?.cancel()
        val duration = if (_pomodoro.value.isBreak) 5 * 60 else 25 * 60
        _pomodoro.update {
            it.copy(
                isRunning = false,
                remainingSeconds = duration,
                totalSeconds = duration
            )
        }
    }

    // CRUD: Notes
    fun saveNote(
        id: String = "",
        subjectId: String?,
        title: String,
        content: String,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val n = Note(
                id = id,
                userId = currentUserId,
                subjectId = subjectId,
                title = title,
                content = content,
                tags = tags
            )
            repository.saveNote(n).onSuccess {
                _actionMessage.value = if (id.isBlank()) "Note created" else "Note updated"
            }.onFailure {
                _actionMessage.value = "Error saving note: ${it.message}"
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(currentUserId, noteId).onSuccess {
                _actionMessage.value = "Note deleted"
            }
        }
    }

    // CRUD: Results
    fun saveSemesterResult(
        id: String = "",
        semesterNumber: Int,
        totalCredits: Int,
        sgpa: Double,
        gradingScale: String = "10_POINT"
    ) {
        viewModelScope.launch {
            val r = SemesterResult(
                id = id,
                userId = currentUserId,
                semesterNumber = semesterNumber,
                totalCredits = totalCredits,
                sgpa = sgpa,
                gradingScale = gradingScale
            )
            repository.saveResult(r).onSuccess {
                _actionMessage.value = "Semester result saved"
            }.onFailure {
                _actionMessage.value = "Error saving result: ${it.message}"
            }
        }
    }

    fun deleteSemesterResult(resultId: String) {
        viewModelScope.launch {
            repository.deleteResult(currentUserId, resultId).onSuccess {
                _actionMessage.value = "Semester result deleted"
            }
        }
    }

    // Support Request
    fun submitSupportTicket(type: String, subject: String, message: String, rating: Int? = null) {
        viewModelScope.launch {
            val ticket = SupportRequest(
                userId = currentUserId,
                type = type,
                subject = subject,
                message = message,
                rating = rating
            )
            repository.submitSupportRequest(ticket).onSuccess {
                _actionMessage.value = "Thank you! Your ticket has been submitted."
            }.onFailure {
                _actionMessage.value = "Submission error: ${it.message}"
            }
        }
    }

    // Gemini Chat
    fun sendChatMessage(text: String) {
        if (text.isBlank() || _isAiThinking.value) return
        val userMsg = ChatMessage(sender = ChatSender.USER, text = text)
        _chatMessages.update { it + userMsg }
        _isAiThinking.value = true

        viewModelScope.launch {
            val mode = _chatModelMode.value
            val response = GeminiService.sendMessage(
                history = _chatMessages.value,
                userMessage = text,
                mode = mode,
                enableSearchGrounding = _searchGroundingEnabled.value,
                enableMapsGrounding = _mapsGroundingEnabled.value
            )
            _isAiThinking.value = false
            val aiMsg = ChatMessage(
                sender = ChatSender.AI,
                text = response.text,
                modelUsed = response.modelUsed,
                sources = response.sources
            )
            _chatMessages.update { it + aiMsg }
        }
    }
}
