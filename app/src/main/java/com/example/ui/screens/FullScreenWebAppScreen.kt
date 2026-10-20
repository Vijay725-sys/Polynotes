package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.ResourceType
import com.example.data.model.StudyResource
import com.example.data.model.Subject
import com.example.data.model.UserProfile
import com.example.ui.PolyNotesViewModel
import org.json.JSONArray
import org.json.JSONObject

private const val SHARED_WEB_APP_URL =
    "https://ais-pre-xkizlrcvgd746t4pvwdm6a-599575110484.asia-east1.run.app"

class PolyNotesWebBridge(
    private val context: Context,
    private val viewModel: PolyNotesViewModel,
    private val onSwitchToNativeUi: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
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

    @JavascriptInterface
    fun toggleBookmark(resourceId: String) {
        mainHandler.post {
            viewModel.toggleBookmark(resourceId)
        }
    }

    @JavascriptInterface
    fun toggleResourceCompletion(resourceId: String) {
        mainHandler.post {
            val res = viewModel.allResources.value.find { it.id == resourceId }
            if (res != null) {
                viewModel.toggleResourceCompletion(res)
            }
        }
    }

    @JavascriptInterface
    fun toggleTopicCompleted(topic: String) {
        mainHandler.post {
            viewModel.toggleTopicCompleted(topic)
        }
    }

    @JavascriptInterface
    fun setDarkMode(enabled: Boolean) {
        mainHandler.post {
            viewModel.setDarkMode(enabled)
        }
    }

    @JavascriptInterface
    fun submitResource(
        title: String,
        subjectId: String,
        subjectName: String,
        unit: Int,
        typeStr: String,
        url: String,
        source: String,
        semester: Int,
        description: String
    ) {
        mainHandler.post {
            val parsedType = try {
                ResourceType.valueOf(typeStr)
            } catch (_: Exception) {
                ResourceType.NOTES
            }
            viewModel.submitResource(
                title = title,
                subjectId = subjectId,
                subjectName = subjectName,
                unit = unit,
                type = parsedType,
                documentUrl = url,
                source = source,
                semester = semester,
                description = description
            )
        }
    }

    @JavascriptInterface
    fun openExternalBrowser(url: String) {
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

    @JavascriptInterface
    fun shareWebLink(url: String) {
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

    @JavascriptInterface
    fun switchToNativeUi() {
        mainHandler.post {
            onSwitchToNativeUi()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FullScreenWebAppScreen(
    viewModel: PolyNotesViewModel,
    onSwitchToNativeUi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allResources by viewModel.allResources.collectAsState()
    val bookmarkedIds by viewModel.bookmarkedIds.collectAsState()
    val completedTopics by viewModel.completedTopics.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isFullScreenImmersed by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(100) }

    // Push live Firestore/ViewModel updates into the WebView whenever state changes
    LaunchedEffect(allResources, bookmarkedIds, completedTopics, userProfile, isDarkMode) {
        webViewRef?.evaluateJavascript("if(window.onAndroidStateUpdated) window.onAndroidStateUpdated();", null)
    }

    BackHandler(enabled = true) {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) {
            wv.goBack()
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
        // Optional Browser Bar (when not in 100% immersed full-screen mode)
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
                    // Browser URL Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHARED_WEB_APP_URL))
                                context.startActivity(intent)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
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
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHARED_WEB_APP_URL))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .size(36.dp)
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
                            .size(36.dp)
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
                            .size(36.dp)
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

        if (loadProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadProgress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Full-Screen WebView Container
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("polynotes_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false

                        addJavascriptInterface(
                            PolyNotesWebBridge(
                                context = ctx,
                                viewModel = viewModel,
                                onSwitchToNativeUi = onSwitchToNativeUi
                            ),
                            "PolyNotesBridge"
                        )

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                if (reqUrl.startsWith("file:///android_asset/")) {
                                    return false
                                }
                                // Open external links directly in the phone's external browser
                                return try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(reqUrl))
                                    ctx.startActivity(intent)
                                    true
                                } catch (_: Exception) {
                                    false
                                }
                            }
                        }

                        loadUrl("file:///android_asset/polynotes_webapp.html")
                        webViewRef = this
                    }
                },
                update = { wv ->
                    webViewRef = wv
                }
            )

            // Compact Floating Control Pill in Full-Screen Mode
            if (isFullScreenImmersed) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 54.dp, end = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHARED_WEB_APP_URL))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .size(34.dp)
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
                            onClick = {
                                val shareText = "Open PolyNotes Web App: $SHARED_WEB_APP_URL"
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share PolyNotes Web Link"))
                            },
                            modifier = Modifier
                                .size(34.dp)
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
                                .size(34.dp)
                                .testTag("exit_fullscreen_bar_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FullscreenExit,
                                contentDescription = "Show Browser Address Bar",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "Native",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onSwitchToNativeUi() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
