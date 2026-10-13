package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val college: String? = null,
    val course: String? = null,
    val department: String? = null,
    val currentSemester: Int = 1,
    val academicYear: String = "2026-2027",
    val targetAttendance: Int = 75,
    val themeMode: String = "system",
    val onboardingCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Subject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val code: String = "",
    val semester: Int = 1,
    val credits: Int = 3,
    val facultyName: String? = null,
    val minAttendance: Int = 75,
    val description: String? = null,
    val colorHex: String = "#4F46E5",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val date: String = "", // YYYY-MM-DD
    val status: String = "PRESENT", // PRESENT, ABSENT, CANCELLED
    val sessionType: String = "LECTURE", // LECTURE, LAB, TUTORIAL
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Assignment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val title: String = "",
    val description: String? = null,
    val assignedDate: String? = null,
    val dueDate: String = "", // YYYY-MM-DD
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, SUBMITTED, COMPLETED
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Exam(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String = "",
    val title: String = "",
    val examDate: String = "", // YYYY-MM-DD
    val startTime: String = "", // HH:mm
    val endTime: String = "", // HH:mm
    val examType: String = "MID_SEM", // INTERNAL, MID_SEM, END_SEM, PRACTICAL, VIVA, OTHER
    val venue: String? = null,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String? = null,
    val title: String = "",
    val date: String = "", // YYYY-MM-DD
    val targetMinutes: Int = 30,
    val completedMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Note(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subjectId: String? = null,
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SemesterResult(
    @DocumentId val id: String = "",
    val userId: String = "",
    val semesterNumber: Int = 1,
    val totalCredits: Int = 20,
    val sgpa: Double = 8.5,
    val gradingScale: String = "10_POINT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupportRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "FEEDBACK", // SUPPORT, FEEDBACK, BUG_REPORT
    val subject: String = "",
    val message: String = "",
    val rating: Int? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class ChatSender {
    USER,
    AI,
    SYSTEM
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String? = null,
    val sources: List<String> = emptyList(),
    val isThinking: Boolean = false
)
