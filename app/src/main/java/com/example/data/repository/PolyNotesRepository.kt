package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.EngineeringBranch
import com.example.data.model.ResourceType
import com.example.data.model.ReviewStatus
import com.example.data.model.StudyResource
import com.example.data.model.Subject
import com.example.data.model.TechnicalBoard
import com.example.data.model.UserProfile
import com.example.data.sample.SampleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PolyNotesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("polynotes_prefs", Context.MODE_PRIVATE)

    private val _subjects = MutableStateFlow<List<Subject>>(SampleData.sampleSubjects)
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _allResources = MutableStateFlow<List<StudyResource>>(loadAllResources())
    val allResources: StateFlow<List<StudyResource>> = _allResources.asStateFlow()

    private val _bookmarkedIds = MutableStateFlow<Set<String>>(loadBookmarkedIds())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    private val _completedTopics = MutableStateFlow<Set<String>>(loadCompletedTopics())
    val completedTopics: StateFlow<Set<String>> = _completedTopics.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile>(loadUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean>(prefs.getBoolean("dark_mode_enabled", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private fun loadBookmarkedIds(): Set<String> {
        val rawSet = prefs.getStringSet("bookmarked_resource_ids", null)
        return rawSet?.toSet() ?: setOf("res_01", "res_07", "res_15")
    }

    private fun saveBookmarkedIds(ids: Set<String>) {
        prefs.edit().putStringSet("bookmarked_resource_ids", ids).apply()
        _bookmarkedIds.value = ids
    }

    fun toggleBookmark(resourceId: String): Boolean {
        val current = _bookmarkedIds.value.toMutableSet()
        val isNowBookmarked = if (current.contains(resourceId)) {
            current.remove(resourceId)
            false
        } else {
            current.add(resourceId)
            true
        }
        saveBookmarkedIds(current)
        return isNowBookmarked
    }

    fun isBookmarked(resourceId: String): Boolean {
        return _bookmarkedIds.value.contains(resourceId)
    }

    private fun loadCompletedTopics(): Set<String> {
        val rawSet = prefs.getStringSet("completed_study_topics", null)
        return rawSet?.toSet() ?: setOf(
            "Surveying Fundamental Principles",
            "Pull, Temperature & Sag Corrections",
            "IS 10262 Step-by-Step Flowchart"
        )
    }

    fun toggleTopicCompleted(topicKey: String): Boolean {
        val current = _completedTopics.value.toMutableSet()
        val isCompleted = if (current.contains(topicKey)) {
            current.remove(topicKey)
            false
        } else {
            current.add(topicKey)
            true
        }
        prefs.edit().putStringSet("completed_study_topics", current).apply()
        _completedTopics.value = current
        return isCompleted
    }

    fun toggleResourceCompletion(resource: StudyResource): Boolean {
        val current = _completedTopics.value.toMutableSet()
        val allTopics = resource.topicsCovered
        if (allTopics.isEmpty()) return false
        val allAlreadyCompleted = allTopics.all { current.contains(it) }
        if (allAlreadyCompleted) {
            current.removeAll(allTopics.toSet())
        } else {
            current.addAll(allTopics)
        }
        prefs.edit().putStringSet("completed_study_topics", current).apply()
        _completedTopics.value = current
        return !allAlreadyCompleted
    }

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
        _isDarkMode.value = enabled
    }

    private fun loadUserProfile(): UserProfile {
        val name = prefs.getString("user_name", "Rahul Sharma") ?: "Rahul Sharma"
        val college = prefs.getString("user_college", "Govt. Polytechnic Dehradun") ?: "Govt. Polytechnic Dehradun"
        val boardStr = prefs.getString("user_board", TechnicalBoard.UBTER.name) ?: TechnicalBoard.UBTER.name
        val branchStr = prefs.getString("user_branch", EngineeringBranch.CIVIL.name) ?: EngineeringBranch.CIVIL.name
        val semester = prefs.getInt("user_semester", 3)

        val board = try { TechnicalBoard.valueOf(boardStr) } catch (_: Exception) { TechnicalBoard.UBTER }
        val branch = try { EngineeringBranch.valueOf(branchStr) } catch (_: Exception) { EngineeringBranch.CIVIL }

        return UserProfile(name, college, board, branch, semester)
    }

    fun updateUserProfile(
        name: String,
        college: String,
        board: TechnicalBoard,
        branch: EngineeringBranch,
        semester: Int
    ) {
        prefs.edit()
            .putString("user_name", name)
            .putString("user_college", college)
            .putString("user_board", board.name)
            .putString("user_branch", branch.name)
            .putInt("user_semester", semester)
            .apply()

        _userProfile.value = UserProfile(name, college, board, branch, semester)
    }

    private fun loadAllResources(): List<StudyResource> {
        val list = SampleData.sampleResources.toMutableList()
        val customJson = prefs.getString("custom_submitted_resources", null)
        if (!customJson.isNullOrBlank()) {
            try {
                val array = JSONArray(customJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val resource = StudyResource(
                        id = obj.getString("id"),
                        subjectId = obj.getString("subjectId"),
                        subjectName = obj.getString("subjectName"),
                        title = obj.getString("title"),
                        unit = obj.getInt("unit"),
                        type = ResourceType.valueOf(obj.getString("type")),
                        documentUrl = obj.getString("documentUrl"),
                        source = obj.getString("source"),
                        reviewStatus = ReviewStatus.valueOf(obj.getString("reviewStatus")),
                        uploadedAt = obj.getString("uploadedAt"),
                        semester = obj.getInt("semester"),
                        description = obj.getString("description"),
                        topicsCovered = listOf("Student Contribution", "Unit ${obj.getInt("unit")} Syllabus"),
                        pageCountOrLength = obj.optString("pageCountOrLength", "Community Doc"),
                        fileFormat = "PDF / Document"
                    )
                    list.add(0, resource)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return list
    }

    fun submitResource(
        title: String,
        subjectId: String,
        subjectName: String,
        unit: Int,
        type: ResourceType,
        documentUrl: String,
        source: String,
        semester: Int,
        description: String
    ): StudyResource {
        val dateStr = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())
        val newResource = StudyResource(
            id = "custom_" + UUID.randomUUID().toString().substring(0, 8),
            subjectId = subjectId,
            subjectName = subjectName,
            title = title.trim(),
            unit = unit,
            type = type,
            documentUrl = documentUrl.trim(),
            source = source.trim().ifEmpty { "Student Contributor" },
            reviewStatus = ReviewStatus.UNDER_REVIEW,
            uploadedAt = dateStr,
            semester = semester,
            description = description.trim().ifEmpty { "Contributed student revision material for Unit $unit." },
            topicsCovered = listOf("Student Contribution", "Unit $unit Focus", subjectName),
            pageCountOrLength = "Community Submission",
            fileFormat = "PDF / Web Link"
        )

        try {
            val existingJson = prefs.getString("custom_submitted_resources", "[]") ?: "[]"
            val array = JSONArray(existingJson)
            val obj = JSONObject().apply {
                put("id", newResource.id)
                put("subjectId", newResource.subjectId)
                put("subjectName", newResource.subjectName)
                put("title", newResource.title)
                put("unit", newResource.unit)
                put("type", newResource.type.name)
                put("documentUrl", newResource.documentUrl)
                put("source", newResource.source)
                put("reviewStatus", newResource.reviewStatus.name)
                put("uploadedAt", newResource.uploadedAt)
                put("semester", newResource.semester)
                put("description", newResource.description)
                put("pageCountOrLength", newResource.pageCountOrLength)
            }
            array.put(obj)
            prefs.edit().putString("custom_submitted_resources", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val updatedList = listOf(newResource) + _allResources.value
        _allResources.value = updatedList
        return newResource
    }

    fun getSubjectById(subjectId: String): Subject? {
        return _subjects.value.find { it.id == subjectId }
    }

    fun getResourceById(resourceId: String): StudyResource? {
        return _allResources.value.find { it.id == resourceId }
    }
}
