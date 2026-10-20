package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.CloudStudyResource
import com.example.data.model.EngineeringBranch
import com.example.data.model.ResourceType
import com.example.data.model.ReviewStatus
import com.example.data.model.StudyResource
import com.example.data.model.Subject
import com.example.data.model.TechnicalBoard
import com.example.data.model.UserProfile
import com.example.data.model.UserProfileDoc
import com.example.data.remote.PolyNotesBackendProxyClient
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

class PolyNotesRepository(
    private val prefs: SharedPreferences? = null,
    val proxyClient: PolyNotesBackendProxyClient = PolyNotesBackendProxyClient()
) {

    constructor(
        context: Context,
        proxyClient: PolyNotesBackendProxyClient = PolyNotesBackendProxyClient()
    ) : this(
        prefs = context.applicationContext.getSharedPreferences("polynotes_prefs", Context.MODE_PRIVATE),
        proxyClient = proxyClient
    )

    private val _subjects = MutableStateFlow<List<Subject>>(SampleData.sampleSubjects)
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _allResources = MutableStateFlow<List<StudyResource>>(loadLocalAllResources())
    val allResources: StateFlow<List<StudyResource>> = _allResources.asStateFlow()

    private val _bookmarkedIds = MutableStateFlow<Set<String>>(loadLocalBookmarkedIds())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    private val _completedTopics = MutableStateFlow<Set<String>>(loadLocalCompletedTopics())
    val completedTopics: StateFlow<Set<String>> = _completedTopics.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile>(loadLocalUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean>(prefs?.getBoolean("dark_mode_enabled", false) ?: false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // =========================================================================
    // BACKEND PROXY SYNC OPERATIONS (Zero Client-Side API Keys)
    // =========================================================================

    suspend fun refreshFromBackendProxy(): Result<List<StudyResource>> {
        val result = proxyClient.fetchResourcesFromProxy()
        result.onSuccess { remoteResources ->
            if (remoteResources.isNotEmpty()) {
                val localList = loadLocalAllResources()
                val existingIds = localList.map { it.id }.toSet()
                val newItems = remoteResources.filter { it.id !in existingIds }
                if (newItems.isNotEmpty()) {
                    _allResources.value = newItems + localList
                }
            }
        }
        return result
    }

    suspend fun syncProfileWithBackendProxy(studentId: String = "default_student"): Result<UserProfileDoc> {
        val profile = _userProfile.value
        val doc = UserProfileDoc(
            id = studentId,
            userId = studentId,
            name = profile.name.trim().take(80).ifBlank { "Rahul Sharma" },
            college = profile.college.trim().take(120).ifBlank { "Govt. Polytechnic Dehradun" },
            board = profile.board.name,
            branch = profile.branch.name,
            semester = profile.semester.toLong().coerceIn(1L, 6L),
            bookmarkedIds = _bookmarkedIds.value.take(200),
            completedTopics = _completedTopics.value.take(200),
            darkMode = _isDarkMode.value
        )
        return proxyClient.syncProfileViaProxy(doc)
    }

    suspend fun submitResourceViaBackendProxy(
        resource: StudyResource,
        studentId: String = "default_student"
    ): Result<StudyResource> {
        val cloudDto = CloudStudyResource(
            id = resource.id,
            userId = studentId,
            subjectId = resource.subjectId,
            subjectName = resource.subjectName,
            title = resource.title,
            unit = resource.unit.toLong(),
            type = resource.type.name,
            documentUrl = resource.documentUrl,
            source = resource.source,
            reviewStatus = resource.reviewStatus.name,
            uploadedAt = resource.uploadedAt,
            semester = resource.semester.toLong(),
            description = resource.description,
            topicsCovered = resource.topicsCovered,
            pageCountOrLength = resource.pageCountOrLength
        )
        return proxyClient.submitResourceViaProxy(cloudDto)
    }

    suspend fun reportResourceIssueViaProxy(resourceTitle: String, reason: String): Result<Boolean> {
        return proxyClient.reportIssueViaProxy(resourceTitle, reason)
    }

    // =========================================================================
    // LOCAL STATE & CACHE OPERATIONS
    // =========================================================================

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs?.edit()?.putBoolean("dark_mode_enabled", enabled)?.apply()
    }

    fun toggleBookmark(resourceId: String): Boolean {
        val current = _bookmarkedIds.value.toMutableSet()
        val added = if (current.contains(resourceId)) {
            current.remove(resourceId)
            false
        } else {
            current.add(resourceId)
            true
        }
        _bookmarkedIds.value = current
        prefs?.edit()?.putStringSet("bookmarked_ids", current)?.apply()
        return added
    }

    fun isBookmarked(resourceId: String): Boolean {
        return _bookmarkedIds.value.contains(resourceId)
    }

    fun toggleTopicCompleted(topicKey: String): Boolean {
        val current = _completedTopics.value.toMutableSet()
        val completed = if (current.contains(topicKey)) {
            current.remove(topicKey)
            false
        } else {
            current.add(topicKey)
            true
        }
        _completedTopics.value = current
        prefs?.edit()?.putStringSet("completed_topics", current)?.apply()
        return completed
    }

    fun toggleResourceCompletion(resource: StudyResource): Boolean {
        val topics = resource.topicsCovered
        if (topics.isEmpty()) return false
        val current = _completedTopics.value.toMutableSet()
        val allCurrentlyCompleted = topics.all { current.contains(it) }
        val nowCompleted = if (allCurrentlyCompleted) {
            current.removeAll(topics.toSet())
            false
        } else {
            current.addAll(topics)
            true
        }
        _completedTopics.value = current
        prefs?.edit()?.putStringSet("completed_topics", current)?.apply()
        return nowCompleted
    }

    fun updateUserProfile(
        name: String,
        college: String,
        board: TechnicalBoard,
        branch: EngineeringBranch,
        semester: Int
    ) {
        val updated = UserProfile(
            name = name,
            college = college,
            board = board,
            branch = branch,
            semester = semester
        )
        _userProfile.value = updated
        prefs?.edit()?.apply {
            putString("profile_name", name)
            putString("profile_college", college)
            putString("profile_board", board.name)
            putString("profile_branch", branch.name)
            putInt("profile_semester", semester)
            apply()
        }
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
        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.US)
        val currentMonthYear = dateFormat.format(Date())

        val newResource = StudyResource(
            id = "res_user_${UUID.randomUUID().toString().take(6)}",
            subjectId = subjectId,
            subjectName = subjectName,
            title = title.trim(),
            unit = unit,
            type = type,
            documentUrl = documentUrl.trim(),
            source = source.trim().ifEmpty { "Student Contributor" },
            reviewStatus = ReviewStatus.UNDER_REVIEW,
            uploadedAt = currentMonthYear,
            semester = semester,
            description = description.trim().ifEmpty {
                "Community-submitted ${type.displayName} for $subjectName (Unit $unit)."
            },
            topicsCovered = listOf(
                "Unit $unit Core Concepts",
                "${title.trim()} Summary Notes",
                "Semester $semester Board Exam Focus"
            ),
            pageCountOrLength = "Community Submission"
        )

        val updatedList = listOf(newResource) + _allResources.value
        _allResources.value = updatedList
        saveCustomResourceToPrefs(newResource)
        return newResource
    }

    private fun loadLocalBookmarkedIds(): Set<String> {
        val saved = prefs?.getStringSet("bookmarked_ids", null)
        return saved ?: setOf("res_01", "res_07", "res_15")
    }

    private fun loadLocalCompletedTopics(): Set<String> {
        val saved = prefs?.getStringSet("completed_topics", null)
        return saved ?: setOf(
            "Surveying Fundamental Principles",
            "Pull, Temperature & Sag Corrections",
            "IS 10262 Step-by-Step Flowchart"
        )
    }

    private fun loadLocalUserProfile(): UserProfile {
        if (prefs == null) return UserProfile()
        val name = prefs.getString("profile_name", "Rahul Sharma") ?: "Rahul Sharma"
        val college = prefs.getString("profile_college", "Govt. Polytechnic Dehradun") ?: "Govt. Polytechnic Dehradun"
        val boardName = prefs.getString("profile_board", TechnicalBoard.UBTER.name) ?: TechnicalBoard.UBTER.name
        val branchName = prefs.getString("profile_branch", EngineeringBranch.CIVIL.name) ?: EngineeringBranch.CIVIL.name
        val semester = prefs.getInt("profile_semester", 3)

        val board = try {
            TechnicalBoard.valueOf(boardName)
        } catch (_: Exception) {
            TechnicalBoard.UBTER
        }

        val branch = try {
            EngineeringBranch.valueOf(branchName)
        } catch (_: Exception) {
            EngineeringBranch.CIVIL
        }

        return UserProfile(
            name = name,
            college = college,
            board = board,
            branch = branch,
            semester = semester
        )
    }

    private fun loadLocalAllResources(): List<StudyResource> {
        val customResources = loadCustomResourcesFromPrefs()
        return customResources + SampleData.sampleResources
    }

    private fun saveCustomResourceToPrefs(resource: StudyResource) {
        if (prefs == null) return
        val existingJson = prefs.getString("custom_submitted_resources", "[]") ?: "[]"
        try {
            val jsonArray = JSONArray(existingJson)
            val obj = JSONObject().apply {
                put("id", resource.id)
                put("subjectId", resource.subjectId)
                put("subjectName", resource.subjectName)
                put("title", resource.title)
                put("unit", resource.unit)
                put("type", resource.type.name)
                put("documentUrl", resource.documentUrl)
                put("source", resource.source)
                put("reviewStatus", resource.reviewStatus.name)
                put("uploadedAt", resource.uploadedAt)
                put("semester", resource.semester)
                put("description", resource.description)
                put("pageCountOrLength", resource.pageCountOrLength)
            }
            val updatedArray = JSONArray()
            updatedArray.put(obj)
            for (i in 0 until jsonArray.length()) {
                updatedArray.put(jsonArray.getJSONObject(i))
            }
            prefs.edit().putString("custom_submitted_resources", updatedArray.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun loadCustomResourcesFromPrefs(): List<StudyResource> {
        if (prefs == null) return emptyList()
        val existingJson = prefs.getString("custom_submitted_resources", null) ?: return emptyList()
        val list = mutableListOf<StudyResource>()
        try {
            val jsonArray = JSONArray(existingJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.optString("title", "Submitted Resource")
                val unit = obj.optInt("unit", 1)
                val semester = obj.optInt("semester", 3)
                list.add(
                    StudyResource(
                        id = obj.optString("id", "res_custom_$i"),
                        subjectId = obj.optString("subjectId", "ubter_ce_301"),
                        subjectName = obj.optString("subjectName", "Basic Surveying"),
                        title = title,
                        unit = unit,
                        type = try {
                            ResourceType.valueOf(obj.optString("type", "NOTES"))
                        } catch (_: Exception) {
                            ResourceType.NOTES
                        },
                        documentUrl = obj.optString("documentUrl", "https://www.ubter.in"),
                        source = obj.optString("source", "Student Contributor"),
                        reviewStatus = try {
                            ReviewStatus.valueOf(obj.optString("reviewStatus", "UNDER_REVIEW"))
                        } catch (_: Exception) {
                            ReviewStatus.UNDER_REVIEW
                        },
                        uploadedAt = obj.optString("uploadedAt", "Oct 2026"),
                        semester = semester,
                        description = obj.optString("description", ""),
                        topicsCovered = listOf(
                            "Unit $unit Core Concepts",
                            "$title Summary Notes",
                            "Semester $semester Board Exam Focus"
                        ),
                        pageCountOrLength = obj.optString("pageCountOrLength", "Community Submission")
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }
}
