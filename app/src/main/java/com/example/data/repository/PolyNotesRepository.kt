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
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.example.data.sample.SampleData
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PolyNotesRepository(
    private val db: FirebaseFirestore,
    private val prefs: SharedPreferences? = null,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = initFirestore(context.applicationContext),
        prefs = context.applicationContext.getSharedPreferences("polynotes_prefs", Context.MODE_PRIVATE),
        auth = initAuth(context.applicationContext)
    )

    private val _subjects = MutableStateFlow<List<Subject>>(SampleData.sampleSubjects)
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _localResources = MutableStateFlow<List<StudyResource>>(loadLocalResources())
    private val _cloudResources = MutableStateFlow<List<StudyResource>>(emptyList())

    private val _allResources = MutableStateFlow<List<StudyResource>>(_localResources.value)
    val allResources: StateFlow<List<StudyResource>> = _allResources.asStateFlow()

    private val _bookmarkedIds = MutableStateFlow<Set<String>>(loadBookmarkedIds())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    private val _completedTopics = MutableStateFlow<Set<String>>(loadCompletedTopics())
    val completedTopics: StateFlow<Set<String>> = _completedTopics.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile>(loadUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean>(prefs?.getBoolean("dark_mode_enabled", false) ?: false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun refreshCombinedResources() {
        val cloudList = _cloudResources.value
        val cloudIds = cloudList.map { it.id }.toSet()
        val combined = cloudList + _localResources.value.filterNot { cloudIds.contains(it.id) }
        _allResources.value = combined
    }

    fun applyCloudProfileSnapshot(doc: UserProfileDoc) {
        val profile = doc.toUserProfile()
        _userProfile.value = profile
        val bookmarks = doc.bookmarkedIds.take(50).toSet()
        val completed = doc.completedTopics.take(100).toSet()
        _bookmarkedIds.value = bookmarks
        _completedTopics.value = completed

        prefs?.edit()
            ?.putString("user_name", profile.name)
            ?.putString("user_college", profile.college)
            ?.putString("user_board", profile.board.name)
            ?.putString("user_branch", profile.branch.name)
            ?.putInt("user_semester", profile.semester)
            ?.putStringSet("bookmarked_resource_ids", bookmarks)
            ?.putStringSet("completed_study_topics", completed)
            ?.apply()
    }

    fun applyCloudResourcesSnapshot(cloudDocs: List<CloudStudyResource>) {
        _cloudResources.value = cloudDocs
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
            .map { it.toStudyResource() }
        refreshCombinedResources()
    }

    // --- FIRESTORE CLOUD OPERATIONS ---

    fun observeUserProfileDoc(userId: String = requireUserId()): Flow<UserProfileDoc?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfileDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
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
        )
    }

    suspend fun syncUserProfileToCloud(
        profile: UserProfile = _userProfile.value,
        bookmarks: Set<String> = _bookmarkedIds.value,
        completed: Set<String> = _completedTopics.value
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        return try {
            val existingSnap = docRef.get().await()
            val boundedBookmarks = bookmarks.take(50).toList()
            val boundedCompleted = completed.take(100).toList()
            if (existingSnap.exists()) {
                val updates = mapOf(
                    "name" to profile.name.trim().take(100).ifEmpty { "Student" },
                    "college" to profile.college.trim().take(150).ifEmpty { "Govt. Polytechnic" },
                    "board" to profile.board.name,
                    "branch" to profile.branch.name,
                    "semester" to profile.semester.coerceIn(1, 6),
                    "bookmarkedIds" to boundedBookmarks,
                    "completedTopics" to boundedCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val newDoc = UserProfileDoc(
                    id = uid,
                    userId = uid,
                    name = profile.name.trim().take(100).ifEmpty { "Student" },
                    college = profile.college.trim().take(150).ifEmpty { "Govt. Polytechnic" },
                    board = profile.board.name,
                    branch = profile.branch.name,
                    semester = profile.semester.coerceIn(1, 6).toLong(),
                    bookmarkedIds = boundedBookmarks,
                    completedTopics = boundedCompleted,
                    createdAt = null,
                    updatedAt = null
                )
                docRef.set(newDoc).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserProfileById(targetUserId: String): Result<UserProfileDoc?> {
        val docRef = db.collection("users").document(targetUserId)
        return try {
            val snap = docRef.get().await()
            val doc = if (snap.exists()) {
                snap.toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            Result.success(doc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeCloudResources(userId: String = requireUserId()): Flow<List<CloudStudyResource>> = flow {
        val path = "users/$userId/resources"
        emitAll(
            db.collection("users").document(userId).collection("resources")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudStudyResource::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
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
        val resourceId = "res_" + UUID.randomUUID().toString().replace("-", "").take(12)
        val docRef = db.collection("users").document(uid).collection("resources").document(resourceId)
        val dateStr = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())

        val cloudResource = CloudStudyResource(
            id = resourceId,
            userId = uid,
            subjectId = subjectId.trim().take(64).ifEmpty { "ubter_ce_301" },
            subjectName = subjectName.trim().take(120).ifEmpty { "Civil Engineering" },
            title = title.trim().take(200),
            unit = unit.coerceIn(1, 10).toLong(),
            type = type.name,
            documentUrl = documentUrl.trim().take(500),
            source = source.trim().take(150).ifEmpty { "Student Contributor" },
            reviewStatus = ReviewStatus.UNDER_REVIEW.name,
            uploadedAt = dateStr,
            semester = semester.coerceIn(1, 6).toLong(),
            description = description.trim().take(500).ifEmpty {
                "Contributed student revision material for Unit $unit."
            },
            topicsCovered = listOf("Student Contribution", "Unit $unit Focus", subjectName.take(80)),
            pageCountOrLength = "Community Submission",
            fileFormat = "PDF / Web Link",
            createdAt = null,
            updatedAt = null
        )

        return try {
            docRef.set(cloudResource).await()
            Result.success(resourceId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getCloudResourceById(ownerUserId: String, resourceId: String): Result<CloudStudyResource?> {
        val docRef = db.collection("users").document(ownerUserId).collection("resources").document(resourceId)
        return try {
            val snap = docRef.get().await()
            val item = if (snap.exists()) {
                snap.toObject(CloudStudyResource::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            Result.success(item)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudResource(resourceId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("resources").document(resourceId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- LOCAL & HYBRID OPERATIONS ---

    private fun loadBookmarkedIds(): Set<String> {
        val rawSet = prefs?.getStringSet("bookmarked_resource_ids", null)
        return rawSet?.toSet() ?: setOf("res_01", "res_07", "res_15")
    }

    private fun saveBookmarkedIds(ids: Set<String>) {
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
        saveBookmarkedIds(current)
        return isNowBookmarked
    }

    fun isBookmarked(resourceId: String): Boolean {
        return _bookmarkedIds.value.contains(resourceId)
    }

    private fun loadCompletedTopics(): Set<String> {
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

    private fun loadUserProfile(): UserProfile {
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

    private fun loadLocalResources(): List<StudyResource> {
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

        _localResources.value = listOf(newResource) + _localResources.value
        refreshCombinedResources()
        return newResource
    }

    fun getSubjectById(subjectId: String): Subject? {
        return _subjects.value.find { it.id == subjectId }
    }

    fun getResourceById(resourceId: String): StudyResource? {
        return _allResources.value.find { it.id == resourceId }
    }

    companion object {
        private fun ensureFirebaseApp(context: Context) {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
        }

        private fun initFirestore(context: Context): FirebaseFirestore {
            ensureFirebaseApp(context)
            return FirebaseFirestore.getInstance(
                context.getString(R.string.firestore_database_id)
            )
        }

        private fun initAuth(context: Context): FirebaseAuth {
            ensureFirebaseApp(context)
            return Firebase.auth
        }
    }
}
