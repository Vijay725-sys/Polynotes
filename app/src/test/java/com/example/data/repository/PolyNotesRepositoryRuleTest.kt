package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.EngineeringBranch
import com.example.data.model.ResourceType
import com.example.data.model.TechnicalBoard
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PolyNotesRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveCloudResource_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = PolyNotesRepository(db = firestore, auth = auth)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createCloudResource(
                title = "Unit 4 Theodolite Traversing Notes",
                subjectId = "ubter_ce_301",
                subjectName = "Basic Surveying",
                unit = 4,
                type = ResourceType.NOTES,
                documentUrl = "https://www.ubter.in",
                source = "Govt Polytechnic Dehradun",
                semester = 3,
                description = "Latitude, departure and closing error adjustments."
            )
        }
        assertTrue("Expected resource creation to succeed", createResult.isSuccess)
        val resId = createResult.getOrThrow()

        val emittedList = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCloudResources(aliceUid).first { list -> list.any { it.id == resId } }
        }
        assertTrue(emittedList.any { it.id == resId && it.userId == aliceUid })
    }

    @Test
    fun syncAndObserveUserProfile_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = PolyNotesRepository(db = firestore, auth = auth)

        val ensureResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfileExists(aliceUid)
        }
        assertTrue(ensureResult.isSuccess)

        repository.updateUserProfile(
            name = "Priya Rawat",
            college = "Govt. Polytechnic Roorkee",
            board = TechnicalBoard.UBTER,
            branch = EngineeringBranch.CIVIL,
            semester = 5
        )
        repository.toggleBookmark("res_03")

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserProfileToCloud(aliceUid)
        }
        assertTrue(saveResult.isSuccess)

        val observedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfileDoc(aliceUid).first { it != null && it.name == "Priya Rawat" }
        }
        assertNotNull(observedProfile)
        assertEquals(5L, observedProfile?.semester)
    }

    @Test
    fun getCloudResourceById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = PolyNotesRepository(db = firestore, auth = auth)
        val resourceId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createCloudResource(
                title = "Alice Private Viva Prep Sheet",
                subjectId = "ubter_ce_302",
                subjectName = "Concrete Technology",
                unit = 2,
                type = ResourceType.VIVA,
                documentUrl = "https://www.ubter.in",
                source = "Alice Notes",
                semester = 3,
                description = "Workability and slump cone test viva questions."
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = PolyNotesRepository(db = firestore, auth = auth)
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                bobRepo.getCloudResourceById(aliceUid, resourceId)
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED for cross-user access")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeCloudResources_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = PolyNotesRepository(db = firestore, auth = auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCloudResources(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated user")
        } catch (e: Throwable) {
            var current: Throwable? = e
            var foundPermissionDenied = false
            while (current != null) {
                if (current is FirebaseFirestoreException &&
                    current.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
                ) {
                    foundPermissionDenied = true
                    break
                }
                current = current.cause
            }
            assertTrue("Expected PERMISSION_DENIED in exception chain, got: $e", foundPermissionDenied)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_poly@test.com"
        const val BOB_EMAIL = "bob_poly@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
