package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CloudStudyResource
import com.example.data.model.ResourceType
import com.example.data.model.UserProfileDoc
import com.example.data.remote.AppSecurityAndHealthManager
import com.example.data.repository.PolyNotesRepository
import com.example.data.sample.SampleData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches PolyNotes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PolyNotes", appName)
    }

    @Test
    fun `verify 16 sample resources loaded with valid data`() {
        assertEquals(16, SampleData.sampleResources.size)
        SampleData.sampleResources.forEach { resource ->
            assertTrue(
                "Resource ${resource.id} documentUrl should be valid: ${resource.documentUrl}",
                resource.documentUrl.startsWith("http://") || resource.documentUrl.startsWith("https://")
            )
            assertNotNull(resource.title)
            assertNotNull(resource.subjectName)
        }
    }

    @Test
    fun `verify bookmarks persist across repository instances`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo1 = PolyNotesRepository(context)

        val testResId = "res_02"
        val initialState = repo1.isBookmarked(testResId)
        val newState = repo1.toggleBookmark(testResId)
        assertEquals(!initialState, newState)

        val repo2 = PolyNotesRepository(context)
        assertEquals(newState, repo2.isBookmarked(testResId))
    }

    @Test
    fun `verify custom resource submission and persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PolyNotesRepository(context)

        val newRes = repo.submitResource(
            title = "Test Civil Concrete Compressive Strength Notes",
            subjectId = "ubter_ce_302",
            subjectName = "Concrete Technology",
            unit = 3,
            type = ResourceType.NOTES,
            documentUrl = "https://www.ubter.in",
            source = "Govt Polytechnic Roorkee Test",
            semester = 3,
            description = "Detailed test summary for testing"
        )

        assertNotNull(newRes.id)
        assertTrue(repo.allResources.value.any { it.id == newRes.id })
    }

    @Test
    fun `verify schema contract validation and health check report`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val validProfile = UserProfileDoc(userId = "uid_123", name = "Rahul Sharma", semester = 3L)
        val invalidProfile = UserProfileDoc(userId = "uid_999", name = "Rahul Sharma", semester = 99L)
        assertTrue(AppSecurityAndHealthManager.validateProfileContract(validProfile, "uid_123"))
        assertFalse(AppSecurityAndHealthManager.validateProfileContract(invalidProfile, "uid_123"))

        val validRes = CloudStudyResource(
            userId = "uid_123",
            title = "Valid Surveying Notes",
            documentUrl = "https://www.ubter.in"
        )
        val invalidRes = CloudStudyResource(
            userId = "uid_123",
            title = "Bad URL",
            documentUrl = "javascript:alert(1)"
        )
        assertTrue(AppSecurityAndHealthManager.validateResourceContract(validRes, "uid_123"))
        assertFalse(AppSecurityAndHealthManager.validateResourceContract(invalidRes, "uid_123"))

        val report = AppSecurityAndHealthManager.runHealthCheck(context, 16, "student@test.com")
        assertTrue(report.checks.isNotEmpty())
        assertTrue(report.databaseId.isNotBlank())
    }
}
