package com.amine.familyhome

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.*

@Composable
internal fun HomeScreen(
    viewer: Viewer,
    remainingMs: Long?,
    apps: List<LauncherApp>,
    focusedPackage: String?,
    resume: Int,
    onFocused: (String) -> Unit,
    onLaunch: (LauncherApp) -> Unit,
    onSwitchProfile: () -> Unit,
    onManage: () -> Unit,
    notice: String? = null,
    displayName: String = viewer.label,
    portrait: Portrait = viewer.defaultPortrait,
    focusManageOnEntry: Boolean = false,
    onSaveOrder: suspend (List<String>) -> Boolean = { false },
) {
    var draft by remember(viewer, apps) { mutableStateOf<List<LauncherApp>?>(null) }
    var movingPackage by remember(viewer, apps) { mutableStateOf<String?>(null) }
    var saving by remember(viewer, apps) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val displayedApps = draft ?: apps
    val featured = displayedApps.firstOrNull { it.pkg == focusedPackage } ?: displayedApps.firstOrNull()
    val keys = apps.map { it.pkg }
    val targets = remember(keys) { keys.associateWith { FocusRequester() } }
    val profiles = remember { FocusRequester() }
    var profileMenuOpen by remember(viewer) { mutableStateOf(false) }
    var profileFocused by remember(viewer) { mutableStateOf(false) }
    val row = rememberLazyListState()
    val activeTarget = targets[featured?.pkg]
    suspend fun focusMovedApp(pkg: String?, ordered: List<LauncherApp>) {
        val index = ordered.indexOfFirst { it.pkg == pkg }
        if (index < 0) return
        withFrameNanos { }
        val layout = row.layoutInfo
        val item = layout.visibleItemsInfo.firstOrNull { it.key == pkg }
        if (item == null || item.offset < layout.viewportStartOffset ||
            item.offset + item.size > layout.viewportEndOffset) {
            row.scrollToItem(index)
            withFrameNanos { }
        }
        targets[pkg]?.requestFocus()
    }
    fun cancelMove() {
        val pkg = movingPackage
        draft = null
        movingPackage = null
        scope.launch { focusMovedApp(pkg, apps) }
    }
    BackHandler(enabled = movingPackage != null) { if (!saving) cancelMove() }
    LaunchedEffect(movingPackage, draft?.map { it.pkg }) {
        if (movingPackage != null) focusMovedApp(movingPackage, displayedApps)
    }
    val chess = featured?.pkg == "com.amine.chessmath"
    val hero = selectedAppHeroArtwork(featured?.pkg, if (chess) null else featured?.heroArtwork, resume)
    Box(Modifier.fillMaxSize()) {
        if (chess) Image(painterResource(R.drawable.chess_hero), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else if (hero != null) {
            Image(hero.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                listOf(Color.Black.copy(alpha = .72f), Color.Transparent))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = .82f)))))
        } else Box(Modifier.fillMaxSize().drawWithCache {
            val gradient = Brush.radialGradient(
                listOf(Color(featured?.backdropColor ?: 0xFF18202A.toInt()), Background),
                center = Offset(size.width * .85f, size.height * .16f), radius = size.width * .9f)
            onDrawBehind { drawRect(gradient) }
        })
        Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 23.dp)) {
            FamilyHeader {
                if (remainingMs != null) {
                    TimerChip(remainingMs)
                    Spacer(Modifier.width(20.dp))
                }
                val profileShape = RoundedCornerShape(12.dp)
                val profileColor = if (profileFocused) Panel else Color.Transparent
                Surface(onClick = { profileMenuOpen = true },
                    modifier = Modifier.testTag("home-profile").focusRequester(profiles)
                        .onFocusChanged { profileFocused = it.isFocused }
                        .border(2.dp, if (profileFocused) Gold else Color.Transparent, profileShape)
                        .focusProperties {
                        up = FocusRequester.Cancel
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
                        down = activeTarget ?: FocusRequester.Cancel
                    },
                    shape = ClickableSurfaceDefaults.shape(profileShape),
                    colors = ClickableSurfaceDefaults.colors(containerColor = profileColor, contentColor = Ink,
                        focusedContainerColor = profileColor, focusedContentColor = Ink),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f)) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        ProfilePortrait(viewer, 30.dp, displayName = displayName, portrait = portrait)
                        Spacer(Modifier.width(10.dp))
                        Text(displayName, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().height(208.dp), contentAlignment = Alignment.CenterStart) {
                Column(Modifier.fillMaxWidth(.53f)) {
                    Text(featured?.label ?: "Your apps",
                        fontSize = 46.sp, lineHeight = 49.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(10.dp))
            if (apps.isNotEmpty()) Text("Your apps", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            if (apps.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(112.dp).testTag("home-apps"), contentAlignment = Alignment.CenterStart) {
                    Text(if (viewer.isChild) "No allowed apps are installed. Ask Amine to review the app list."
                        else "No TV apps are installed.", color = Muted, fontSize = 18.sp)
                }
            } else LazyRow(state = row, horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp),
                modifier = Modifier.fillMaxWidth().height(112.dp).testTag("home-apps")) {
                items(displayedApps, key = { it.pkg }) { app ->
                    AppTile(app, Modifier.focusRequester(targets.getValue(app.pkg)).focusProperties {
                        up = if (movingPackage == null) profiles else FocusRequester.Cancel
                        down = FocusRequester.Cancel
                        if (app == displayedApps.first()) left = FocusRequester.Cancel
                        if (app == displayedApps.last()) right = FocusRequester.Cancel
                    }.onPreviewKeyEvent { event ->
                        if (movingPackage == null) false
                        else when (event.key) {
                            Key.DirectionUp, Key.DirectionDown -> true
                            Key.DirectionLeft, Key.DirectionRight -> {
                                if (event.type == KeyEventType.KeyDown && !saving) {
                                    val current = draft.orEmpty()
                                    val index = current.indexOfFirst { it.pkg == movingPackage }
                                    val next = index + if (event.key == Key.DirectionLeft) -1 else 1
                                    if (index >= 0 && next in current.indices) {
                                        draft = current.toMutableList().apply {
                                            val moved = removeAt(index)
                                            add(next, moved)
                                        }
                                    }
                                }
                                true
                            }
                            else -> false
                        }
                    }.onFocusChanged { if (it.isFocused) onFocused(app.pkg) },
                        moving = movingPackage == app.pkg,
                        onLongClick = {
                            if (movingPackage == null) {
                                draft = apps
                                movingPackage = app.pkg
                            }
                        },
                        onClick = {
                            if (movingPackage == null) onLaunch(app)
                            else if (!saving) {
                                saving = true
                                val packages = displayedApps.map { it.pkg }
                                scope.launch {
                                    try {
                                        val saved = withContext(NonCancellable) { onSaveOrder(packages) }
                                        if (isActive && saved) {
                                            draft = null
                                            movingPackage = null
                                        }
                                    } finally { if (isActive) saving = false }
                                }
                            }
                        })
                }
            }
            if (movingPackage != null) Text("Left/Right move · OK save · Back cancel",
                Modifier.testTag("app-move-hint"), color = Gold, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            notice?.let { Text(it, color = Gold, fontSize = 15.sp, maxLines = 2); Spacer(Modifier.height(8.dp)) }

        }
    }
    if (profileMenuOpen) {
        ProfileMenu(viewer,
            onDismiss = { profileMenuOpen = false },
            onSwitchProfile = { profileMenuOpen = false; onSwitchProfile() },
            onManage = { profileMenuOpen = false; onManage() })
    }

    LaunchedEffect(viewer, keys, resume) {
        if (focusManageOnEntry) {
            profiles.requestFocus()
        } else if (featured != null) {
            row.scrollToItem(apps.indexOfFirst { it.pkg == featured.pkg }.coerceAtLeast(0))
            withFrameNanos { }
            targets.getValue(featured.pkg).requestFocus()
        } else profiles.requestFocus()
    }
}

@Composable
private fun ProfileMenu(viewer: Viewer, onDismiss: () -> Unit,
    onSwitchProfile: () -> Unit, onManage: () -> Unit) {
    val switchProfile = remember { FocusRequester() }
    val manage = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().padding(top = 82.dp, end = 36.dp, bottom = 120.dp),
            contentAlignment = Alignment.TopEnd) {
            Column(Modifier.width(304.dp).background(Panel, RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(16.dp))
                .padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ListItem(selected = false, onClick = onSwitchProfile,
                    modifier = Modifier.fillMaxWidth().focusRequester(switchProfile).focusProperties {
                        up = FocusRequester.Cancel; down = manage
                        left = FocusRequester.Cancel; right = FocusRequester.Cancel
                    },
                    headlineContent = { Text("Switch profile", fontSize = 20.sp) },
                    leadingContent = { Icon(painterResource(R.drawable.ic_switch_profile), null, Modifier.size(24.dp)) },
                    colors = ListItemDefaults.colors(focusedContainerColor = Gold, focusedContentColor = Background),
                    scale = ListItemDefaults.scale(focusedScale = 1f))
                ListItem(selected = false, onClick = onManage,
                    modifier = Modifier.fillMaxWidth().focusRequester(manage).focusProperties {
                        up = switchProfile; down = FocusRequester.Cancel
                        left = FocusRequester.Cancel; right = FocusRequester.Cancel
                    },
                    headlineContent = { Text(if (viewer.isChild) "End session" else "Settings", fontSize = 20.sp) },
                    leadingContent = if (viewer.isChild) null else ({
                        Icon(painterResource(R.drawable.ic_settings), null, Modifier.size(24.dp))
                    }),
                    colors = ListItemDefaults.colors(focusedContainerColor = Gold, focusedContentColor = Background),
                    scale = ListItemDefaults.scale(focusedScale = 1f))
            }
        }
        LaunchedEffect(Unit) { switchProfile.requestFocus() }
    }
}

@Composable
private fun TimerChip(milliseconds: Long) {
    Row(Modifier.width(154.dp).height(38.dp).testTag("countdown-chip")
        .semantics(mergeDescendants = true) { contentDescription = "${timeLeft(milliseconds)} left" }
        .background(Color.White.copy(alpha = .065f), RoundedCornerShape(14.dp))
        .border(1.dp, Color.White.copy(alpha = .1f), RoundedCornerShape(14.dp))
        .padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(19.dp)) {
            val stroke = 1.5.dp.toPx()
            drawCircle(Ink, radius = size.minDimension / 2 - stroke, style = Stroke(stroke))
            drawLine(Ink, center, Offset(center.x, size.height * .23f), strokeWidth = stroke)
            drawLine(Ink, center, Offset(size.width * .72f, size.height * .64f), strokeWidth = stroke)
        }
        Spacer(Modifier.width(9.dp))
        val time = timeLeft(milliseconds).padStart(5, ' ')
        Row(Modifier.width(57.dp), verticalAlignment = Alignment.CenterVertically) {
            time.forEachIndexed { index, character ->
                Text(character.toString(), Modifier.width(if (index == 2) 9.dp else 12.dp)
                    .testTag("countdown-digit:$index"),
                    textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            }
        }
        Spacer(Modifier.width(5.dp))
        Text("left", fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AppTile(app: LauncherApp, modifier: Modifier, moving: Boolean,
    onLongClick: () -> Unit, onClick: () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    Surface(onClick = onClick, onLongClick = onLongClick,
        modifier = modifier.width(if (app.banner != null) 144.dp else 81.dp).height(81.dp).testTag("app:${app.pkg}"),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Panel, contentColor = Ink,
            focusedContainerColor = Panel, focusedContentColor = Ink),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.045f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = .12f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, Gold), shape = shape)),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (app.banner != null) {
                Image(app.banner.asImageBitmap(), app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                Image(app.icon.asImageBitmap(), app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
            if (moving) Text("‹  ›", Modifier.align(Alignment.BottomCenter)
                .background(Background.copy(alpha = .9f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp),
                color = Gold, fontSize = 18.sp)
        }
    }
}
