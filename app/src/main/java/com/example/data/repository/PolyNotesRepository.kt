package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.R
import com.example.data.model.CloudStudyResource
import com.example.data.model.EngineeringBranch
import com.example.data.model.ResourceType
import com.example.data.model.ReviewStatus
import com.example.data.model.StudyResource
import com.example.data.model.Subject
import com.example.data.model.TechnicalBoard
import com.example.data.model.UserProfile
import com.example.data.model.UserProfileDoc
import com.example.data.remote.AppSecurityAndHealthManager
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.sample.SampleData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PolyNotesRepository(
    private val db: FirebaseFirestore? = null,
    private val auth: FirebaseAuth? = null,
    private val prefs: SharedPreferences? = null
) {

    constructor(context: Context) : this(
        db = try {
            FirebaseFirestore.getInstance(
                context.applicationContext.getString(R.string.firestore_database_id)
            )
        } catch (_: Exception) {
            null
        },
        auth = try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        },
        prefs = context.applicationContext.getSharedPreferences("polynotes_prefs", Context.MODE_PRIVATE)
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

    private fun requireFirestore(): FirebaseFirestore {
        return checkNotNull(db) { "FirebaseFirestore instance is not configured." }
    }

    private fun requireUserId(): String {
        val currentAuth = auth ?: FirebaseAuth.getInstance()
        return currentAuth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // =========================================================================
    // FIRESTORE CLOUD SYNC OPERATIONS (Scoped to /users/{userId})
    // =========================================================================

    fun observeUserProfileDoc(userId: String): Flow<UserProfileDoc?> {
        val firestore = requireFirestore()
        val path = "users/$userId"
        return firestore.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    val doc = snapshot.toObject(
                        UserProfileDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                    if (doc != null) {
                        applyCloudProfileLocally(doc)
                    }
                    doc
                } else {
                    null
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, path)
                }
                throw error
            }
    }

    suspend fun ensureUserProfileExists(userId: String): Result<UserProfileDoc> {
        val firestore = requireFirestore()
        val docRef = firestore.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    UserProfileDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfileDoc(id = userId, userId = userId)
                applyCloudProfileLocally(existing)
                Result.success(existing)
            } else {
                val localProfile = _userProfile.value
                val initialDoc = UserProfileDoc(
                    id = userId,
                    userId = userId,
                    name = localProfile.name.take(80).ifBlank { "Rahul Sharma" },
                    college = localProfile.college.take(120).ifBlank { "Govt. Polytechnic Dehradun" },
                    board = localProfile.board.name,
                    branch = localProfile.branch.name,
                    semester = localProfile.semester.toLong().coerceIn(1L, 6L),
                    bookmarkedIds = _bookmarkedIds.value.take(200),
                    completedTopics = _completedTopics.value.take(200),
                    darkMode = _isDarkMode.value,
                    createdAt = null,
                    updatedAt = null
                )
                require(AppSecurityAndHealthManager.validateProfileContract(initialDoc, userId)) {
                    "Invalid UserProfileDoc contract"
                }
                docRef.set(initialDoc).await()
                Result.success(initialDoc)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfileToCloud(userId: String): Result<Unit> {
        val firestore = requireFirestore()
        val docRef = firestore.collection("users").document(userId)
        return try {
            val snap = docRef.get().await()
            val localProfile = _userProfile.value
            val bookmarksList = _bookmarkedIds.value.take(200)
            val completedList = _completedTopics.value.take(200)
            val dark = _isDarkMode.value

            if (snap.exists()) {
                val updatePayload = mapOf(
                    "name" to localProfile.name.take(80).ifBlank { "Rahul Sharma" },
                    "college" to localProfile.college.take(120).ifBlank { "Govt. Polytechnic Dehradun" },
                    "board" to localProfile.board.name,
                    "branch" to localProfile.branch.name,
                    "semester" to localProfile.semester.toLong().coerceIn(1L, 6L),
                    "bookmarkedIds" to bookmarksList,
                    "completedTopics" to completedList,
                    "darkMode" to dark,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            } else {
                val newDoc = UserProfileDoc(
                    id = userId,
                    userId = userId,
                    name = localProfile.name.take(80).ifBlank { "Rahul Sharma" },
                    college = localProfile.college.take(120).ifBlank { "Govt. Polytechnic Dehradun" },
                    board = localProfile.board.name,
                    branch = localProfile.branch.name,
                    semester = localProfile.semester.toLong().coerceIn(1L, 6L),
                    bookmarkedIds = bookmarksList,
                    completedTopics = completedList,
                    darkMode = dark,
                    createdAt = null,
                    updatedAt = null
                )
                docRef.set(newDoc).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCloudResources(userId: String): Flow<List<CloudStudyResource>> {
        val firestore = requireFirestore()
        val path = "users/$userId/resources"
        return firestore.collection("users").document(userId).collection("resources")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { querySnapshot ->
                val cloudList = querySnapshot.toObjects(
                    CloudStudyResource::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                mergeCloudResourcesIntoAll(cloudList)
                cloudList
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun createCloudResource(
        title: String,
        subjectId: String,
        subjectName: String,
        unit: Int,
        type: ResourceType,
        documentUrl: String,
        source: String,
        semester: Int,
        description: String
    ): Result<String> {
        val uid = requireUserId()
        val firestore = requireFirestore()
        val resourceId = "res_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val docRef = firestore.collection("users").document(uid)
            .collection("resources").document(resourceId)
        val dateStr = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())

        val cloudRes = CloudStudyResource(
            id = resourceId,
            userId = uid,
            subjectId = subjectId.take(64).ifBlank { "ubter_ce_301" },
            subjectName = subjectName.take(100).ifBlank { "Basic Surveying" },
            title = title.trim().take(140),
            unit = unit.toLong().coerceIn(1L, 6L),
            type = type.name,
            documentUrl = documentUrl.trim().take(500),
            source = source.trim().ifEmpty { "Student Contributor" }.take(100),
            reviewStatus = ReviewStatus.UNDER_REVIEW.name,
            uploadedAt = dateStr.take(40),
            semester = semester.toLong().coerceIn(1L, 6L),
            description = description.trim()
                .ifEmpty { "Contributed student revision material for Unit $unit." }
                .take(500),
            topicsCovered = listOf("Student Contribution", "Unit $unit Focus", subjectName.take(50)),
            pageCountOrLength = "Community Submission",
            fileFormat = "PDF / Web Link",
            createdAt = null,
            updatedAt = null
        )

        return try {
            require(AppSecurityAndHealthManager.validateResourceContract(cloudRes, uid)) {
                "Invalid CloudStudyResource contract"
            }
            docRef.set(cloudRes).await()
            Result.success(resourceId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getCloudResourceById(ownerUserId: String, resourceId: String): Result<CloudStudyResource?> {
        val firestore = requireFirestore()
        val docRef = firestore.collection("users").document(ownerUserId)
            .collection("resources").document(resourceId)
        return try {
            val snap = docRef.get().await()
            val item = snap.toObject(
                CloudStudyResource::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    private fun applyCloudProfileLocally(doc: UserProfileDoc) {
        val profile = doc.toUserProfile()
        _userProfile.value = profile
        _bookmarkedIds.value = doc.bookmarkedIds.toSet()
        _completedTopics.value = doc.completedTopics.toSet()
        _isDarkMode.value = doc.darkMode

        prefs?.edit()
            ?.putString("user_name", profile.name)
            ?.putString("user_college", profile.college)
            ?.putString("user_board", profile.board.name)
            ?.putString("user_branch", profile.branch.name)
            ?.putInt("user_semester", profile.semester)
            ?.putStringSet("bookmarked_resource_ids", _bookmarkedIds.value)
            ?.putStringSet("completed_study_topics", _completedTopics.value)
            ?.putBoolean("dark_mode_enabled", doc.darkMode)
            ?.apply()
    }

    private fun mergeCloudResourcesIntoAll(cloudList: List<CloudStudyResource>) {
        val mappedCloud = cloudList.map { it.toStudyResource() }
        val localBase = loadLocalAllResources()
        val cloudIds = mappedCloud.map { it.id }.toSet()
        val merged = mappedCloud + localBase.filter { !cloudIds.contains(it.id) }
        _allResources.value = merged
    }

    // =========================================================================
    // LOCAL HYBRID FALLBACK & SYNCHRONOUS STATE UPDATES
    // =========================================================================

    private fun loadLocalBookmarkedIds(): Set<String> {
        val rawSet = prefs?.getStringSet("bookmarked_resource_ids", null)
        return rawSet?.toSet() ?: setOf("res_01", "res_07", "res_15")
    }

    private fun saveLocalBookmarkedIds(ids: Set<String>) {
        prefs?.edit()?.putStringSet("bookmarked_resource_ids", ids)?.apply()
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
        saveLocalBookmarkedIds(current)
        return isNowBookmarked
    }

    fun isBookmarked(resourceId: String): Boolean {
        return _bookmarkedIds.value.contains(resourceId)
    }

    private fun loadLocalCompletedTopics(): Set<String> {
        val rawSet = prefs?.getStringSet("completed_study_topics", null)
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
        prefs?.edit()?.putStringSet("completed_study_topics", current)?.apply()
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
        prefs?.edit()?.putStringSet("completed_study_topics", current)?.apply()
        _completedTopics.value = current
        return !allAlreadyCompleted
    }

    fun setDarkMode(enabled: Boolean) {
        prefs?.edit()?.putBoolean("dark_mode_enabled", enabled)?.apply()
        _isDarkMode.value = enabled
    }

    private fun loadLocalUserProfile(): UserProfile {
        val name = prefs?.getString("user_name", "Rahul Sharma") ?: "Rahul Sharma"
        val college = prefs?.getString("user_college", "Govt. Polytechnic Dehradun") ?: "Govt. Polytechnic Dehradun"
        val boardStr = prefs?.getString("user_board", TechnicalBoard.UBTER.name) ?: TechnicalBoard.UBTER.name
        val branchStr = prefs?.getString("user_branch", EngineeringBranch.CIVIL.name) ?: EngineeringBranch.CIVIL.name
        val semester = prefs?.getInt("user_semester", 3) ?: 3

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
        prefs?.edit()
            ?.putString("user_name", name)
            ?.putString("user_college", college)
            ?.putString("user_board", board.name)
            ?.putString("user_branch", branch.name)
            ?.putInt("user_semester", semester)
            ?.apply()

        _userProfile.value = UserProfile(name, college, board, branch, semester)
    }

    private fun loadLocalAllResources(): List<StudyResource> {
        val list = SampleData.sampleResources.toMutableList()
        val customJson = prefs?.getString("custom_submitted_resources", null)
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
            val existingJson = prefs?.getString("custom_submitted_resources", "[]") ?: "[]"
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
            prefs?.edit()?.putString("custom_submitted_resources", array.toString())?.apply()
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
