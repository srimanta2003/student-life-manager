package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.*
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StudentLifeRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    // User Profile
    fun observeProfile(userId: String): Flow<UserProfile?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val docRef = db.collection("users").document(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                handleFirestoreError(error, OperationType.GET, docRef.path)
                trySend(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                trySend(snapshot.toObject(UserProfile::class.java))
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        val docRef = db.collection("users").document(profile.userId)
        return try {
            val payload = mutableMapOf<String, Any?>(
                "userId" to profile.userId,
                "fullName" to profile.fullName,
                "email" to profile.email,
                "college" to profile.college,
                "course" to profile.course,
                "department" to profile.department,
                "currentSemester" to profile.currentSemester,
                "academicYear" to profile.academicYear,
                "targetAttendance" to profile.targetAttendance,
                "themeMode" to profile.themeMode,
                "onboardingCompleted" to profile.onboardingCompleted,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val existing = docRef.get().await()
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload.filterValues { it != null }).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    // Subjects
    fun observeSubjects(userId: String): Flow<List<Subject>> {
        val path = "users/$userId/subjects"
        val collRef = db.collection("users").document(userId).collection("subjects")
        return collRef.snapshots()
            .map { snap -> snap.toObjects(Subject::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun saveSubject(subject: Subject): Result<String> {
        val id = if (subject.id.isNotBlank()) subject.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(subject.userId).collection("subjects").document(id)
        return try {
            val payload = mutableMapOf<String, Any?>(
                "id" to id,
                "userId" to subject.userId,
                "name" to subject.name,
                "code" to subject.code,
                "semester" to subject.semester,
                "credits" to subject.credits,
                "facultyName" to subject.facultyName,
                "minAttendance" to subject.minAttendance,
                "description" to subject.description,
                "colorHex" to subject.colorHex,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val existing = docRef.get().await()
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload.filterValues { it != null }).await()
            Result.success(id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteSubject(userId: String, subjectId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("subjects").document(subjectId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Attendance
    fun observeAttendance(userId: String): Flow<List<AttendanceRecord>> {
        val path = "users/$userId/attendance"
        val collRef = db.collection("users").document(userId).collection("attendance")
        return collRef.snapshots()
            .map { snap -> snap.toObjects(AttendanceRecord::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun recordAttendance(attendance: AttendanceRecord): Result<String> {
        val id = if (attendance.id.isNotBlank()) attendance.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(attendance.userId).collection("attendance").document(id)
        return try {
            val payload = mutableMapOf<String, Any?>(
                "id" to id,
                "userId" to attendance.userId,
                "subjectId" to attendance.subjectId,
                "date" to attendance.date,
                "status" to attendance.status,
                "sessionType" to attendance.sessionType,
                "note" to attendance.note,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val existing = docRef.get().await()
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload.filterValues { it != null }).await()
            Result.success(id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAttendance(userId: String, attendanceId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("attendance").document(attendanceId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Assignments
    fun observeAssignments(userId: String): Flow<List<Assignment>> {
        val path = "users/$userId/assignments"
        val collRef = db.collection("users").document(userId).collection("assignments")
        return collRef.snapshots()
            .map { snap -> snap.toObjects(Assignment::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun saveAssignment(assignment: Assignment): Result<String> {
        val id = if (assignment.id.isNotBlank()) assignment.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(assignment.userId).collection("assignments").document(id)
        return try {
            val payload = mutableMapOf<String, Any?>(
                "id" to id,
                "userId" to assignment.userId,
                "subjectId" to assignment.subjectId,
                "title" to assignment.title,
                "description" to assignment.description,
                "assignedDate" to assignment.assignedDate,
                "dueDate" to assignment.dueDate,
                "priority" to assignment.priority,
                "status" to assignment.status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val existing = docRef.get().await()
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload.filterValues { it != null }).await()
            Result.success(id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAssignment(userId: String, assignmentId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("assignments").document(assignmentId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Exams
    fun observeExams(userId: String): Flow<List<Exam>> {
        val path = "users/$userId/exams"
        val collRef = db.collection("users").document(userId).collection("exams")
        return collRef.snapshots()
            .map { snap -> snap.toObjects(Exam::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun saveExam(exam: Exam): Result<String> {
        val id = if (exam.id.isNotBlank()) exam.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(exam.userId).collection("exams").document(id)
        return try {
            val payload = mutableMapOf<String, Any?>(
                "id" to id,
                "userId" to exam.userId,
                "subjectId" to exam.subjectId,
                "title" to exam.title,
                "examDate" to exam.examDate,
                "startTime" to exam.startTime,
                "endTime" to exam.endTime,
                "examType" to exam.examType,
                "venue" to exam.venue,
                "notes" to exam.notes,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val existing = docRef.get().await()
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload.filterValues { it != null }).await()
            Result.success(id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteExam(userId: String, examId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("exams").document(examId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Study Tasks
    fun observeStudyTasks(userId: String): Flow<List<StudyTask>> {
        val path = "users/$userId/study_tasks"
        val collRef = db.collection("users").document(userId).collection("study_tasks")
        return collRef.snapshots()
            .map { snap -> snap.toObjects(StudyTask::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun saveStudyTask(task: StudyTask): Result<String> {
        val id = if (task.id.isNotBlank()) task.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(task.userId).collection("study_tasks").document(id)
        return try {
            val payload = mutableMapOf<String, Any?>(
                "id" to id,
                "userId" to task.userId,
                "subjectId" to task.subjectId,
                "title" to task.title,
                "date" to task.date,
                "targetMinutes" to task.targetMinutes,
                "completedMinutes" to task.completedMinutes,
                "isCompleted" to task.isCompleted,
                "notes" to task.notes,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val existing = docRef.get().await()
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload.filterValues { it != null }).await()
            Result.success(id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudyTask(userId: String, taskId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("study_tasks").document(taskId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Notes
    fun observeNotes(userId: String): Flow<List<Note>> {
        val path = "users/$userId/notes"
        val collRef = db.collection("users").document(userId).collection("notes")
        return collRef.snapshots()
            .map { snap -> snap.toObjects(Note::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun saveNote(note: Note): Result<String> {
        val id = if (note.id.isNotBlank()) note.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(note.userId).collection("notes").document(id)
        return try {
            val payload = mutableMapOf<String, Any?>(
                "id" to id,
                "userId" to note.userId,
                "subjectId" to note.subjectId,
                "title" to note.title,
                "content" to note.content,
                "tags" to note.tags,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val existing = docRef.get().await()
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload.filterValues { it != null }).await()
            Result.success(id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteNote(userId: String, noteId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("notes").document(noteId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Results (SGPA / CGPA)
    fun observeResults(userId: String): Flow<List<SemesterResult>> {
        val path = "users/$userId/results"
        val collRef = db.collection("users").document(userId).collection("results")
        return collRef.snapshots()
            .map { snap -> snap.toObjects(SemesterResult::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun saveResult(result: SemesterResult): Result<String> {
        val id = if (result.id.isNotBlank()) result.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(result.userId).collection("results").document(id)
        return try {
            val payload = mutableMapOf<String, Any?>(
                "id" to id,
                "userId" to result.userId,
                "semesterNumber" to result.semesterNumber,
                "totalCredits" to result.totalCredits,
                "sgpa" to result.sgpa,
                "gradingScale" to result.gradingScale,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val existing = docRef.get().await()
            if (!existing.exists()) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            }
            docRef.set(payload.filterValues { it != null }).await()
            Result.success(id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteResult(userId: String, resultId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("results").document(resultId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // Support Request
    suspend fun submitSupportRequest(request: SupportRequest): Result<String> {
        val id = if (request.id.isNotBlank()) request.id else UUID.randomUUID().toString()
        val docRef = db.collection("users").document(request.userId).collection("support_requests").document(id)
        return try {
            val payload = mutableMapOf<String, Any?>(
                "id" to id,
                "userId" to request.userId,
                "type" to request.type,
                "subject" to request.subject,
                "message" to request.message,
                "rating" to request.rating,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload.filterValues { it != null }).await()
            Result.success(id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }
}
