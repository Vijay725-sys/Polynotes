package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ResourceType
import com.example.data.model.StudyResource
import com.example.data.model.Subject
import com.example.data.model.UserProfile
import com.example.data.remote.AppSecurityAndHealthManager
import com.example.ui.PolyNotesViewModel
import org.json.JSONArray
import org.json.JSONObject

private const val SHARED_WEB_APP_URL =
    "https://ais-pre-xkizlrcvgd746t4pvwdm6a-599575110484.asia-east1.run.app"

/**
 * State bridge and JSON serializer for the Full-Screen Web App experience.
 * Implemented without instantiating system Chromium WebView so the app avoids
 * Chromium C++ variations seed errors while delivering a fast, native full-screen
 * web app experience.
 */
class PolyNotesWebBridge(
    private val context: Context,
    private val viewModel: PolyNotesViewModel,
    private val onSwitchToNativeUi: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getStateJson(): String {
        val profile: UserProfile = viewModel.userProfile.value
        val bookmarks: Set<String> = viewModel.bookmarkedIds.value
        val completed: Set<String> = viewModel.completedTopics.value
        val subjects: List<Subject> = viewModel.subjects.value
        val resources: List<StudyResource> = viewModel.allResources.value
        val isDark: Boolean = viewModel.isDarkMode.value

        val root = JSONObject()
        root.put("isDarkMode", isDark)

        val profileObj = JSONObject().apply {
            put("name", profile.name)
            put("college", profile.college)
            put("board", profile.board.code)
            put("branch", profile.branch.displayName)
            put("semester", profile.semester)
        }
        root.put("userProfile", profileObj)

        val bookmarkArr = JSONArray()
        bookmarks.forEach { bookmarkArr.put(it) }
        root.put("bookmarkedIds", bookmarkArr)

        val completedArr = JSONArray()
        completed.forEach { completedArr.put(it) }
        root.put("completedTopics", completedArr)

        val subjectsArr = JSONArray()
        subjects.forEach { s ->
            val unitsArr = JSONArray()
            s.units.forEach { u ->
                unitsArr.put(
                    JSONObject().apply {
                        put("unitNumber", u.unitNumber)
                        put("title", u.title)
                        put("summary", u.summary)
                    }
                )
            }
            subjectsArr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("code", s.code)
                    put("semester", s.semester)
                    put("unitCount", s.unitCount)
                    put("description", s.description)
                    put("units", unitsArr)
                }
            )
        }
        root.put("subjects", subjectsArr)

        val resArr = JSONArray()
        resources.forEach { r ->
            val topicsArr = JSONArray()
            r.topicsCovered.forEach { topicsArr.put(it) }
            resArr.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("subjectId", r.subjectId)
                    put("subjectName", r.subjectName)
                    put("title", r.title)
                    put("unit", r.unit)
                    put("type", r.type.name)
                    put("typeLabel", r.type.displayName)
                    put("documentUrl", r.documentUrl)
                    put("source", r.source)
                    put("reviewStatusLabel", r.reviewStatus.displayName)
                    put("uploadedAt", r.uploadedAt)
                    put("semester", r.semester)
                    put("description", r.description)
                    put("pageCountOrLength", r.pageCountOrLength)
                    put("topicsCovered", topicsArr)
                }
            )
        }
        root.put("resources", resArr)
        return root.toString()
    }

    fun openExternalBrowser(url: String = SHARED_WEB_APP_URL) {
        mainHandler.post {
            try {
                val target = if (url.startsWith("http://") || url.startsWith("https://")) {
                    url
                } else {
                    SHARED_WEB_APP_URL
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    fun shareWebLink(url: String = SHARED_WEB_APP_URL) {
        mainHandler.post {
            try {
                val shareText =
                    "PolyNotes Web App (Every Subject. Every Note. One Place.) — Open in browser: $url"
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    type = "text/plain"
                }
                val chooser = Intent.createChooser(sendIntent, "Share PolyNotes Web App").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (_: Exception) {
            }
        }
    }

    fun switchToNativeUi() {
        mainHandler.post {
            onSwitchToNativeUi()
        }
    }
}

private enum class WebPortalTab(val label: String) {
    EXPLORE("Explore"),
    SUBJECTS("Subjects"),
    SAVED("Saved"),
    SECURITY("Proxy & Status")
}

@Composable
fun FullScreenWebAppScreen(
    viewModel: PolyNotesViewModel,
    onSwitchToNativeUi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allResources by viewModel.allResources.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val completedTopics by viewModel.completedTopics.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    val bridge = remember(context, viewModel, onSwitchToNativeUi) {
        PolyNotesWebBridge(context, viewModel, onSwitchToNativeUi)
    }

    var isFullScreenImmersed by rememberSaveable { mutableStateOf(true) }
    var activeTab by rememberSaveable { mutableStateOf(WebPortalTab.EXPLORE) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedTypeFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSubjectFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedSubjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var showContributeDialog by rememberSaveable { mutableStateOf(false) }

    val filteredWebResources = remember(allResources, searchQuery, selectedTypeFilter, selectedSubjectFilter) {
        allResources.filter { res ->
            val matchesQuery = searchQuery.isBlank() ||
                res.title.contains(searchQuery, ignoreCase = true) ||
                res.subjectName.contains(searchQuery, ignoreCase = true) ||
                res.description.contains(searchQuery, ignoreCase = true) ||
                res.topicsCovered.any { it.contains(searchQuery, ignoreCase = true) }
            val matchesType = selectedTypeFilter == null || res.type.name == selectedTypeFilter
            val matchesSubject = selectedSubjectFilter == null || res.subjectId == selectedSubjectFilter
            matchesQuery && matchesType && matchesSubject
        }
    }

    BackHandler(enabled = true) {
        if (activeTab != WebPortalTab.EXPLORE) {
            activeTab = WebPortalTab.EXPLORE
        } else {
            onSwitchToNativeUi()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("fullscreen_webapp_screen")
    ) {
        // Optional External Browser Chrome Bar (when toggled out of immersed full-screen mode)
        AnimatedVisibility(visible = !isFullScreenImmersed) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { bridge.openExternalBrowser(SHARED_WEB_APP_URL) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "HTTPS Secure",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = SHARED_WEB_APP_URL,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { bridge.openExternalBrowser(SHARED_WEB_APP_URL) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_external_browser_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in External Browser",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onSwitchToNativeUi,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("switch_native_ui_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhoneAndroid,
                            contentDescription = "Switch to Native Android UI",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { isFullScreenImmersed = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("enter_fullscreen_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fullscreen,
                            contentDescription = "Enter Full Screen",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Full-Screen Web App Viewport Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("polynotes_webview")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Sticky Web App Top Header
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF1E1B4B), Color(0xFF4F46E5))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "PN",
                                    color = Color(0xFFFBBF24),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "PolyNotes Web",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${userProfile.board.code} • ${userProfile.branch.code} Sem ${userProfile.semester}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.setDarkMode(!isDarkMode) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = "Toggle Dark Mode",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            OutlinedButton(
                                onClick = { showContributeDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Main Web App Scrollable Content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Hero Banner Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4F46E5))
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "🌐 FULL-SCREEN WEB APP • ZERO CLIENT API KEYS",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Every Subject. Every Note. One Place.",
                                    color = Color.White,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Curated UBTER & State Polytechnic study notes, lab manuals, PYQs, and viva banks synced via our serverless backend proxy.",
                                    color = Color(0xFFE0E7FF),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { bridge.openExternalBrowser(SHARED_WEB_APP_URL) },
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open in External Browser", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = onSwitchToNativeUi,
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f))
                                    ) {
                                        Text("Native App View", color = Color.White, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    when (activeTab) {
                        WebPortalTab.EXPLORE -> {
                            // Search Bar
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text("Search subjects, units, surveying, SOM, PYQs...")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Search,
                                            contentDescription = "Search"
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }

                            // Filter Chips Row
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedTypeFilter == null && selectedSubjectFilter == null,
                                        onClick = {
                                            selectedTypeFilter = null
                                            selectedSubjectFilter = null
                                        },
                                        label = { Text("All (${allResources.size})") }
                                    )
                                    ResourceType.entries.forEach { type ->
                                        FilterChip(
                                            selected = selectedTypeFilter == type.name,
                                            onClick = {
                                                selectedTypeFilter =
                                                    if (selectedTypeFilter == type.name) null else type.name
                                            },
                                            label = { Text(type.displayName) }
                                        )
                                    }
                                }
                            }

                            // Resource Cards
                            items(filteredWebResources, key = { it.id }) { resource ->
                                WebResourceCard(
                                    resource = resource,
                                    isBookmarked = bookmarkedIds.contains(resource.id),
                                    isCompleted = resource.topicsCovered.isNotEmpty() &&
                                        completedTopics.containsAll(resource.topicsCovered),
                                    onToggleBookmark = { viewModel.toggleBookmark(resource.id) },
                                    onToggleCompleted = { viewModel.toggleResourceCompletion(resource) },
                                    onOpenDocument = { bridge.openExternalBrowser(resource.documentUrl) }
                                )
                            }
                        }

                        WebPortalTab.SUBJECTS -> {
                            items(subjects, key = { it.id }) { subject ->
                                val isExpanded = expandedSubjectId == subject.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedSubjectId = if (isExpanded) null else subject.id
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${subject.code} • Semester ${subject.semester}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = subject.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = subject.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "${subject.unitCount} Units",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            HorizontalDivider()
                                            Spacer(modifier = Modifier.height(8.dp))
                                            subject.units.forEach { unit ->
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = "Unit ${unit.unitNumber}: ${unit.title}",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = unit.summary,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    selectedSubjectFilter = subject.id
                                                    activeTab = WebPortalTab.EXPLORE
                                                },
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text("View Subject Notes & PYQs")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        WebPortalTab.SAVED -> {
                            val savedList = allResources.filter { bookmarkedIds.contains(it.id) }
                            if (savedList.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "No Saved Bookmarks Yet",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Bookmark study notes, practical manuals, or PYQs from the Explore tab for instant offline access.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(savedList, key = { it.id }) { resource ->
                                    WebResourceCard(
                                        resource = resource,
                                        isBookmarked = true,
                                        isCompleted = resource.topicsCovered.isNotEmpty() &&
                                            completedTopics.containsAll(resource.topicsCovered),
                                        onToggleBookmark = { viewModel.toggleBookmark(resource.id) },
                                        onToggleCompleted = { viewModel.toggleResourceCompletion(resource) },
                                        onOpenDocument = { bridge.openExternalBrowser(resource.documentUrl) }
                                    )
                                }
                            }
                        }

                        WebPortalTab.SECURITY -> {
                            item {
                                val report = remember(allResources.size) {
                                    AppSecurityAndHealthManager.runHealthCheck(
                                        context = context,
                                        totalResources = allResources.size
                                    )
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "🛡️ Zero Client-Side API Keys • Backend Proxy",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "All external API requests are handled by the serverless backend proxy (/api/v1) using server-side environment variables.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        report.checks.forEach { check ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 6.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = check.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = check.detail,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Compact Floating Control Pill in Full-Screen Mode
            if (isFullScreenImmersed) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 56.dp, end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { bridge.openExternalBrowser(SHARED_WEB_APP_URL) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("floating_external_browser_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open in External Browser",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { bridge.shareWebLink(SHARED_WEB_APP_URL) },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("floating_share_web_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Share Web Link",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { isFullScreenImmersed = false },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("exit_fullscreen_bar_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FullscreenExit,
                                contentDescription = "Show Browser Address Bar",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        TextButton(
                            onClick = onSwitchToNativeUi
                        ) {
                            Text(
                                text = "Native",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Bottom Web Navigation Bar
        NavigationBar(
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
            tonalElevation = 4.dp
        ) {
            NavigationBarItem(
                selected = activeTab == WebPortalTab.EXPLORE,
                onClick = { activeTab = WebPortalTab.EXPLORE },
                icon = { Icon(Icons.Filled.Home, contentDescription = "Explore") },
                label = { Text("Explore") }
            )
            NavigationBarItem(
                selected = activeTab == WebPortalTab.SUBJECTS,
                onClick = { activeTab = WebPortalTab.SUBJECTS },
                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Subjects") },
                label = { Text("Subjects") }
            )
            NavigationBarItem(
                selected = activeTab == WebPortalTab.SAVED,
                onClick = { activeTab = WebPortalTab.SAVED },
                icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Saved") },
                label = { Text("Saved (${bookmarkedIds.size})") }
            )
            NavigationBarItem(
                selected = activeTab == WebPortalTab.SECURITY,
                onClick = { activeTab = WebPortalTab.SECURITY },
                icon = { Icon(Icons.Filled.Security, contentDescription = "Proxy") },
                label = { Text("Proxy") }
            )
        }
    }

    if (showContributeDialog) {
        WebContributeResourceDialog(
            subjects = subjects,
            onDismiss = { showContributeDialog = false },
            onSubmit = { title, subjectId, subjectName, unit, type, url, source, desc ->
                viewModel.submitResource(
                    title = title,
                    subjectId = subjectId,
                    subjectName = subjectName,
                    unit = unit,
                    type = type,
                    documentUrl = url,
                    source = source,
                    semester = 3,
                    description = desc
                )
                showContributeDialog = false
            }
        )
    }
}

@Composable
private fun WebResourceCard(
    resource: StudyResource,
    isBookmarked: Boolean,
    isCompleted: Boolean,
    onToggleBookmark: () -> Unit,
    onToggleCompleted: () -> Unit,
    onOpenDocument: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${resource.type.displayName} • Unit ${resource.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = resource.pageCountOrLength,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = resource.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${resource.subjectName} • ${resource.source}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = resource.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onToggleBookmark) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = "Toggle Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleCompleted) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Mark Revised",
                            tint = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = onOpenDocument,
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Resource", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun WebContributeResourceDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        subjectId: String,
        subjectName: String,
        unit: Int,
        type: ResourceType,
        url: String,
        source: String,
        description: String
    ) -> Unit
) {
    val defaultSubject = subjects.firstOrNull()
    var title by remember { mutableStateOf("") }
    var documentUrl by remember { mutableStateOf("https://www.ubter.in/") }
    var source by remember { mutableStateOf("Student Contributor") }
    var description by remember { mutableStateOf("") }
    var unit by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upload Study Resource", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Resource Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = documentUrl,
                    onValueChange = { documentUrl = it },
                    label = { Text("Document HTTPS Link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Contributor / Faculty Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Summary / Topics Covered") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSubmit(
                            title.trim(),
                            defaultSubject?.id ?: "ubter_ce_301",
                            defaultSubject?.name ?: "Basic Surveying",
                            unit,
                            ResourceType.NOTES,
                            documentUrl.trim(),
                            source.trim(),
                            description.trim().ifBlank { "Community uploaded study notes" }
                        )
                    }
                }
            ) {
                Text("Submit via Proxy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
