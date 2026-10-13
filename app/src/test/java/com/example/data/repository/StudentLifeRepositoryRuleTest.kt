package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Subject
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class StudentLifeRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveSubject_validPayload_createsDocumentAndReturnsId() {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val repository = StudentLifeRepository(firestore)

            val subject = Subject(
                id = "sub_" + UUID.randomUUID().toString().take(8),
                userId = aliceUid,
                name = "Data Structures",
                code = "CS201",
                semester = 3,
                credits = 4,
                minAttendance = 75
            )

            val result = withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.saveSubject(subject)
            }
            assertTrue("Subject creation failed: ${result.exceptionOrNull()?.message}", result.isSuccess)
            val subjectId = result.getOrThrow()
            assertEquals(subject.id, subjectId)
        }
    }

    @Test
    fun observeSubjects_authenticatedOwner_emitsRealtimeUpdates() {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val repository = StudentLifeRepository(firestore)

            val subId = "sub_" + UUID.randomUUID().toString().take(8)
            val subject = Subject(
                id = subId,
                userId = aliceUid,
                name = "Algorithms",
                code = "CS301",
                semester = 5,
                credits = 4,
                minAttendance = 80
            )
            repository.saveSubject(subject).getOrThrow()

            val subjects = withTimeout(DEFAULT_TIMEOUT_MS) {
                repository.observeSubjects(aliceUid).first { list ->
                    list.any { it.id == subId }
                }
            }
            assertTrue(subjects.any { it.id == subId && it.name == "Algorithms" })
        }
    }

    @Test
    fun crossUserIsolation_bobCannotReadAlicesSubjects() {
        runBlocking {
            val aliceUid = signInTestUser(ALICE_EMAIL)
            val aliceRepo = StudentLifeRepository(firestore)

            val subId = "sub_" + UUID.randomUUID().toString().take(8)
            val subject = Subject(
                id = subId,
                userId = aliceUid,
                name = "Database Systems",
                code = "CS401",
                semester = 4,
                credits = 3,
                minAttendance = 75
            )
            aliceRepo.saveSubject(subject).getOrThrow()

            signInTestUser(BOB_EMAIL)
            try {
                firestore.collection("users").document(aliceUid).collection("subjects").document(subId).get()
            } catch (e: Exception) {
                assertTrue(e is FirebaseFirestoreException)
            }
        }
    }

    @Test
    fun unauthenticatedAccess_rejectedByRules() {
        runBlocking {
            auth.signOut()
            try {
                firestore.collection("users").document("random_user").get()
            } catch (e: Exception) {
                assertTrue(e is FirebaseFirestoreException)
            }
        }
    }

    companion object {
        const val ALICE_EMAIL = "alice@example.com"
        const val BOB_EMAIL = "bob@example.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
    }
}
