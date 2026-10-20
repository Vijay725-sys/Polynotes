package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainTab
import com.example.ui.PolyNotesViewModel
import com.example.ui.screens.EditProfileBottomSheet
import com.example.ui.screens.FullScreenWebAppScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileAboutScreen
import com.example.ui.screens.ResourceDetailView
import com.example.ui.screens.ResourceLibraryScreen
import com.example.ui.screens.SavedNotesScreen
import com.example.ui.screens.SubjectDetailView
import com.example.ui.screens.SubjectsScreen
import com.example.ui.screens.SubmitResourceBottomSheet
import com.example.ui.theme.PolyNotesTheme
import kotlinx.coroutines.flow.collectLatest

private const val EXTERNAL_WEB_URL =
    "https://ais-pre-xkizlrcvgd746t4pvwdm6a-599575110484.asia-east1.run.app"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppContent()
        }
    }
}

@Composable
fun AppContent(viewModel: PolyNotesViewModel = viewModel()) {
    var isFullScreenWebMode by rememberSaveable { mutableStateOf(true) }
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    PolyNotesTheme(darkTheme = isDarkMode) {
        if (isFullScreenWebMode) {
            FullScreenWebAppScreen(
                viewModel = viewModel,
                onSwitchToNativeUi = { isFullScreenWebMode = false }
            )
        } else {
            PolyNotesApp(
                viewModel = viewModel,
                onOpenFullScreenWebMode = { isFullScreenWebMode = true }
            )
        }
    }
}

@Composable
fun PolyNotesApp(
    viewModel: PolyNotesViewModel,
    onOpenFullScreenWebMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val allResources by viewModel.allResources.collectAsState()
    val filteredResources by viewModel.filteredResources.collectAsState()
    val savedResources by viewModel.savedResources.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val completedTopics by viewModel.completedTopics.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    BackHandler(enabled = true) {
        viewModel.handleBackPress()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (uiState.selectedResourceForDetail == null && uiState.selectedSubjectForUnits == null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 6.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        TextButton(
                            onClick = onOpenFullScreenWebMode,
                            modifier = Modifier.testTag("open_fullscreen_web_mode_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = "Full-Screen Web App View",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Full-Screen Web View",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(EXTERNAL_WEB_URL))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.testTag("launch_external_browser_fab")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in External Browser",
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (uiState.selectedResourceForDetail == null && uiState.selectedSubjectForUnits == null) {
                NavigationBar(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .testTag("bottom_nav_bar"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    val tabs = listOf(
                        Triple(MainTab.HOME, Icons.Filled.Home, Icons.Outlined.Home),
                        Triple(MainTab.SUBJECTS, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
                        Triple(MainTab.LIBRARY, Icons.Filled.Search, Icons.Outlined.Search),
                        Triple(MainTab.SAVED, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
                        Triple(MainTab.PROFILE, Icons.Filled.Person, Icons.Outlined.Person)
                    )

                    tabs.forEach { (tab, selectedIcon, unselectedIcon) ->
                        val isSelected = uiState.currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Screen 4: Resource Details View
                uiState.selectedResourceForDetail != null -> {
                    val resource = uiState.selectedResourceForDetail!!
                    ResourceDetailView(
                        resource = resource,
                        isBookmarked = bookmarkedIds.contains(resource.id),
                        completedTopics = completedTopics,
                        onBookmarkToggle = { viewModel.toggleBookmark(resource.id) },
                        onToggleTopicCompleted = { topic -> viewModel.toggleTopicCompleted(topic) },
                        onReportIssue = { reason -> viewModel.reportResourceIssue(resource.title, reason) },
                        onBack = { viewModel.closeResourceDetail() }
                    )
                }

                // Screen 2 Subview: Subject Units & Syllabus Detail
                uiState.selectedSubjectForUnits != null -> {
                    val subject = uiState.selectedSubjectForUnits!!
                    SubjectDetailView(
                        subject = subject,
                        onBack = { viewModel.closeSubjectUnits() },
                        onExploreUnitResources = { unitNum ->
                            viewModel.closeSubjectUnits()
                            viewModel.setSubjectFilter(subject.id)
                            viewModel.onSearchQueryChanged("Unit $unitNum")
                            viewModel.selectTab(MainTab.LIBRARY)
                        }
                    )
                }

                // Primary Tabs
                else -> {
                    AnimatedContent(
                        targetState = uiState.currentTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "tab_transition"
                    ) { tab ->
                        when (tab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    userProfile = userProfile,
                                    selectedSemester = uiState.selectedSemester,
                                    subjects = subjects,
                                    recentResources = allResources,
                                    bookmarkedIds = bookmarkedIds,
                                    completedTopics = completedTopics,
                                    searchQuery = uiState.searchQuery,
                                    onSearchChange = { query ->
                                        viewModel.onSearchQueryChanged(query)
                                        if (query.isNotEmpty() && uiState.currentTab != MainTab.LIBRARY) {
                                            viewModel.selectTab(MainTab.LIBRARY)
                                        }
                                    },
                                    onSemesterSelect = { sem -> viewModel.setSemesterFilter(sem) },
                                    onTypeSelect = { type ->
                                        viewModel.setResourceTypeFilter(type)
                                        viewModel.selectTab(MainTab.LIBRARY)
                                    },
                                    onSubjectClick = { subject ->
                                        viewModel.openSubjectUnits(subject)
                                    },
                                    onResourceClick = { resource ->
                                        viewModel.openResourceDetail(resource)
                                    },
                                    onBookmarkToggle = { id -> viewModel.toggleBookmark(id) },
                                    onToggleResourceCompletion = { res -> viewModel.toggleResourceCompletion(res) },
                                    onSubmitClick = { viewModel.openSubmitDialog() },
                                    onViewAllResourcesClick = { viewModel.selectTab(MainTab.LIBRARY) }
                                )
                            }

                            MainTab.SUBJECTS -> {
                                SubjectsScreen(
                                    subjects = subjects,
                                    allResources = allResources,
                                    selectedSemester = uiState.selectedSemester,
                                    onSemesterSelect = { sem -> viewModel.setSemesterFilter(sem) },
                                    onSubjectClick = { subject -> viewModel.openSubjectUnits(subject) }
                                )
                            }

                            MainTab.LIBRARY -> {
                                ResourceLibraryScreen(
                                    resources = filteredResources,
                                    subjects = subjects,
                                    bookmarkedIds = bookmarkedIds,
                                    searchQuery = uiState.searchQuery,
                                    selectedTypeFilter = uiState.selectedTypeFilter,
                                    selectedSemester = uiState.selectedSemester,
                                    selectedSubjectFilter = uiState.selectedSubjectFilter,
                                    onSearchChange = { q -> viewModel.onSearchQueryChanged(q) },
                                    onTypeFilterChange = { t -> viewModel.setResourceTypeFilter(t) },
                                    onSemesterChange = { s -> viewModel.setSemesterFilter(s) },
                                    onSubjectFilterChange = { subId -> viewModel.setSubjectFilter(subId) },
                                    onResourceClick = { res -> viewModel.openResourceDetail(res) },
                                    onBookmarkToggle = { id -> viewModel.toggleBookmark(id) },
                                    completedTopics = completedTopics,
                                    onToggleResourceCompletion = { res -> viewModel.toggleResourceCompletion(res) },
                                    onResetFilters = {
                                        viewModel.onSearchQueryChanged("")
                                        viewModel.setResourceTypeFilter(null)
                                        viewModel.setSemesterFilter(0)
                                        viewModel.setSubjectFilter(null)
                                    }
                                )
                            }

                            MainTab.SAVED -> {
                                SavedNotesScreen(
                                    savedResources = savedResources,
                                    onResourceClick = { res -> viewModel.openResourceDetail(res) },
                                    onBookmarkToggle = { id -> viewModel.toggleBookmark(id) },
                                    completedTopics = completedTopics,
                                    onToggleResourceCompletion = { res -> viewModel.toggleResourceCompletion(res) },
                                    onExploreLibraryClick = { viewModel.selectTab(MainTab.LIBRARY) }
                                )
                            }

                            MainTab.PROFILE -> {
                                ProfileAboutScreen(
                                    userProfile = userProfile,
                                    totalResourcesCount = allResources.size,
                                    savedNotesCount = savedResources.size,
                                    isDarkMode = isDarkMode,
                                    onDarkModeToggle = { enabled -> viewModel.setDarkMode(enabled) },
                                    onEditProfileClick = { viewModel.toggleProfileEdit(true) },
                                    onSubmitNewResourceClick = { viewModel.openSubmitDialog() }
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isSubmitDialogOpen) {
                SubmitResourceBottomSheet(
                    subjects = subjects,
                    defaultSemester = uiState.selectedSemester,
                    onDismiss = { viewModel.closeSubmitDialog() },
                    onSubmit = { title, subId, subName, unit, type, docUrl, source, sem, desc ->
                        viewModel.submitResource(
                            title = title,
                            subjectId = subId,
                            subjectName = subName,
                            unit = unit,
                            type = type,
                            documentUrl = docUrl,
                            source = source,
                            semester = sem,
                            description = desc
                        )
                    }
                )
            }

            if (uiState.isProfileEditOpen) {
                EditProfileBottomSheet(
                    currentProfile = userProfile,
                    onDismiss = { viewModel.toggleProfileEdit(false) },
                    onSave = { name, college, board, branch, sem ->
                        viewModel.updateProfile(name, college, board, branch, sem)
                    }
                )
            }
        }
    }
}
