package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.EngineeringBranch
import com.example.data.model.ResourceType
import com.example.data.model.TechnicalBoard
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PolyNotesRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun syncAndObserveUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = PolyNotesRepository(db = firestore, auth = auth)

        val testProfile = UserProfile(
            name = "Aarav Rawat",
            college = "Govt. Polytechnic Roorkee",
            board = TechnicalBoard.UBTER,
            branch = EngineeringBranch.CIVIL,
            semester = 4
        )
        val syncResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncUserProfileToCloud(
                profile = testProfile,
                bookmarks = setOf("res_01", "res_05"),
                completed = setOf("Surveying Fundamental Principles")
            )
        }
        assertTrue("Expected syncUserProfileToCloud to succeed: ${syncResult.exceptionOrNull()}", syncResult.isSuccess)

        val emittedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfileDoc(uid).first { it != null && it.name == "Aarav Rawat" }
        }
        assertNotNull(emittedProfile)
        assertEquals(4L, emittedProfile?.semester)
        assertTrue(emittedProfile?.bookmarkedIds?.contains("res_05") == true)
    }

    @Test
    fun createAndObserveCloudResource_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = PolyNotesRepository(db = firestore, auth = auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCloudResource(
                title = "Hydraulics Bernoulli Theorem Solved Numericals",
                subjectId = "ubter_ce_303",
                subjectName = "Hydraulics & Fluid Mechanics",
                unit = 2,
                type = ResourceType.NOTES,
                documentUrl = "https://www.ubter.in",
                source = "Govt Polytechnic Dehradun",
                semester = 3,
                description = "Solved numericals on Venturimeter and Orifice meter."
            )
        }
        assertTrue("Expected createCloudResource to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val resourceId = createResult.getOrThrow()

        val emittedResources = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCloudResources(uid).first { list -> list.any { it.id == resourceId } }
        }
        assertTrue(emittedResources.any { it.id == resourceId && it.title.contains("Bernoulli") })
    }

    @Test
    fun getCloudResourceById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = PolyNotesRepository(db = firestore, auth = auth)
        val resourceId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCloudResource(
                title = "Alice Private Surveying Notes",
                subjectId = "ubter_ce_301",
                subjectName = "Basic Surveying",
                unit = 1,
                type = ResourceType.NOTES,
                documentUrl = "https://www.ubter.in",
                source = "Alice",
                semester = 3,
                description = "Chain surveying field notes."
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = PolyNotesRepository(db = firestore, auth = auth)
        val bobReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getCloudResourceById(aliceUid, resourceId)
        }
        assertTrue(bobReadResult.isFailure)
        val ex = bobReadResult.exceptionOrNull() as? FirebaseFirestoreException
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun observeCloudResources_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = PolyNotesRepository(db = firestore, auth = auth)

        var caughtPermissionDenied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCloudResources(aliceUid).first()
            }
        } catch (t: Throwable) {
            var current: Throwable? = t
            while (current != null) {
                if (current is FirebaseFirestoreException &&
                    current.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                ) {
                    caughtPermissionDenied = true
                    break
                }
                current = current.cause
            }
        }
        assertTrue("Expected PERMISSION_DENIED for unauthenticated user", caughtPermissionDenied)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_poly@test.com"
        const val BOB_EMAIL = "bob_poly@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
