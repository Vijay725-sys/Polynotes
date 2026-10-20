package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.EngineeringBranch
import com.example.data.model.ResourceType
import com.example.data.model.StudyResource
import com.example.data.model.Subject
import com.example.data.model.TechnicalBoard
import com.example.data.model.UserProfile
import com.example.data.repository.PolyNotesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab(val title: String, val testTag: String) {
    HOME("Home", "nav_home"),
    SUBJECTS("Subjects", "nav_subjects"),
    LIBRARY("Library", "nav_library"),
    SAVED("Saved", "nav_saved"),
    PROFILE("Profile", "nav_profile")
}

data class UiState(
    val currentTab: MainTab = MainTab.HOME,
    val selectedBoard: TechnicalBoard = TechnicalBoard.UBTER,
    val selectedBranch: EngineeringBranch = EngineeringBranch.CIVIL,
    val selectedSemester: Int = 3,
    val searchQuery: String = "",
    val selectedTypeFilter: ResourceType? = null,
    val selectedSubjectFilter: String? = null,
    val selectedResourceForDetail: StudyResource? = null,
    val selectedSubjectForUnits: Subject? = null,
    val isSubmitDialogOpen: Boolean = false,
    val isFilterSheetOpen: Boolean = false,
    val isProfileEditOpen: Boolean = false
)

class PolyNotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PolyNotesRepository(application.applicationContext)

    val subjects: StateFlow<List<Subject>> = repository.subjects
    val allResources: StateFlow<List<StudyResource>> = repository.allResources
    val bookmarkedIds: StateFlow<Set<String>> = repository.bookmarkedIds
    val completedTopics: StateFlow<Set<String>> = repository.completedTopics
    val userProfile: StateFlow<UserProfile> = repository.userProfile
    val isDarkMode: StateFlow<Boolean> = repository.isDarkMode

    private val _uiState = MutableStateFlow(
        UiState(
            selectedBoard = repository.userProfile.value.board,
            selectedBranch = repository.userProfile.value.branch,
            selectedSemester = repository.userProfile.value.semester
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    val filteredResources: StateFlow<List<StudyResource>> = combine(
        allResources,
        _uiState
    ) { resources, state ->
        resources.filter { res ->
            val matchesQuery = if (state.searchQuery.isBlank()) true else {
                val q = state.searchQuery.trim().lowercase()
                res.title.lowercase().contains(q) ||
                res.subjectName.lowercase().contains(q) ||
                res.source.lowercase().contains(q) ||
                "unit ${res.unit}".contains(q) ||
                res.topicsCovered.any { it.lowercase().contains(q) } ||
                res.type.displayName.lowercase().contains(q)
            }

            val matchesType = state.selectedTypeFilter == null || res.type == state.selectedTypeFilter
            val matchesSemester = state.selectedSemester == 0 || res.semester == state.selectedSemester
            val matchesSubject = state.selectedSubjectFilter == null || res.subjectId == state.selectedSubjectFilter

            matchesQuery && matchesType && matchesSemester && matchesSubject
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedResources: StateFlow<List<StudyResource>> = combine(
        allResources,
        bookmarkedIds
    ) { resources, ids ->
        resources.filter { ids.contains(it.id) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: MainTab) {
        _uiState.value = _uiState.value.copy(
            currentTab = tab,
            selectedResourceForDetail = null,
            selectedSubjectForUnits = null
        )
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setResourceTypeFilter(type: ResourceType?) {
        _uiState.value = _uiState.value.copy(selectedTypeFilter = type)
    }

    fun setSemesterFilter(semester: Int) {
        _uiState.value = _uiState.value.copy(selectedSemester = semester)
    }

    fun setSubjectFilter(subjectId: String?) {
        _uiState.value = _uiState.value.copy(selectedSubjectFilter = subjectId)
    }

    fun setBoard(board: TechnicalBoard) {
        _uiState.value = _uiState.value.copy(selectedBoard = board)
    }

    fun setBranch(branch: EngineeringBranch) {
        _uiState.value = _uiState.value.copy(selectedBranch = branch)
    }

    fun openResourceDetail(resource: StudyResource) {
        _uiState.value = _uiState.value.copy(selectedResourceForDetail = resource)
    }

    fun closeResourceDetail() {
        _uiState.value = _uiState.value.copy(selectedResourceForDetail = null)
    }

    fun openSubjectUnits(subject: Subject) {
        _uiState.value = _uiState.value.copy(selectedSubjectForUnits = subject)
    }

    fun closeSubjectUnits() {
        _uiState.value = _uiState.value.copy(selectedSubjectForUnits = null)
    }

    fun openSubmitDialog() {
        _uiState.value = _uiState.value.copy(isSubmitDialogOpen = true)
    }

    fun closeSubmitDialog() {
        _uiState.value = _uiState.value.copy(isSubmitDialogOpen = false)
    }

    fun toggleFilterSheet(open: Boolean) {
        _uiState.value = _uiState.value.copy(isFilterSheetOpen = open)
    }

    fun toggleProfileEdit(open: Boolean) {
        _uiState.value = _uiState.value.copy(isProfileEditOpen = open)
    }

    fun toggleBookmark(resourceId: String) {
        val isNowBookmarked = repository.toggleBookmark(resourceId)
        viewModelScope.launch {
            if (isNowBookmarked) {
                _userMessage.emit("Saved to your study bookmarks")
            } else {
                _userMessage.emit("Removed from Saved Notes")
            }
        }
    }

    fun isBookmarked(resourceId: String): Boolean {
        return repository.isBookmarked(resourceId)
    }

    fun toggleTopicCompleted(topicKey: String) {
        val completed = repository.toggleTopicCompleted(topicKey)
        viewModelScope.launch {
            if (completed) {
                _userMessage.emit("Topic marked as revised!")
            }
        }
    }

    fun toggleResourceCompletion(resource: StudyResource) {
        val nowCompleted = repository.toggleResourceCompletion(resource)
        viewModelScope.launch {
            if (nowCompleted) {
                _userMessage.emit("Great job! '${resource.title}' marked 100% reviewed.")
            } else {
                _userMessage.emit("Revision status reset for '${resource.title}'")
            }
        }
    }

    fun setDarkMode(enabled: Boolean) {
        repository.setDarkMode(enabled)
    }

    fun reportResourceIssue(resourceTitle: String, reason: String) {
        viewModelScope.launch {
            _userMessage.emit("Report received for '$resourceTitle' ($reason). Our editorial team will review it within 24h.")
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
    ): Boolean {
        if (title.trim().length < 4) {
            viewModelScope.launch { _userMessage.emit("Title must be at least 4 characters") }
            return false
        }
        if (documentUrl.isBlank() || (!documentUrl.startsWith("http://") && !documentUrl.startsWith("https://"))) {
            viewModelScope.launch { _userMessage.emit("Please enter a valid document URL (https://...)") }
            return false
        }

        val newRes = repository.submitResource(
            title = title,
            subjectId = subjectId,
            subjectName = subjectName,
            unit = unit,
            type = type,
            documentUrl = documentUrl,
            source = source,
            semester = semester,
            description = description
        )

        closeSubmitDialog()
        viewModelScope.launch {
            _userMessage.emit("Resource '${newRes.title}' added to library!")
        }
        return true
    }

    fun updateProfile(
        name: String,
        college: String,
        board: TechnicalBoard,
        branch: EngineeringBranch,
        semester: Int
    ) {
        repository.updateUserProfile(name, college, board, branch, semester)
        _uiState.value = _uiState.value.copy(
            selectedBoard = board,
            selectedBranch = branch,
            selectedSemester = semester,
            isProfileEditOpen = false
        )
        viewModelScope.launch {
            _userMessage.emit("Academic profile updated")
        }
    }

    fun handleBackPress(): Boolean {
        val state = _uiState.value
        return when {
            state.selectedResourceForDetail != null -> {
                closeResourceDetail()
                true
            }
            state.selectedSubjectForUnits != null -> {
                closeSubjectUnits()
                true
            }
            state.isSubmitDialogOpen -> {
                closeSubmitDialog()
                true
            }
            state.currentTab != MainTab.HOME -> {
                selectTab(MainTab.HOME)
                true
            }
            else -> false
        }
    }
}
