package com.amine.familyhome

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
) {
    val featured = apps.firstOrNull { it.pkg == focusedPackage } ?: apps.firstOrNull()
    val keys = apps.map { it.pkg }
    val targets = remember(keys) { keys.associateWith { FocusRequester() } }
    val profiles = remember { FocusRequester() }
    var profileMenuOpen by remember(viewer) { mutableStateOf(false) }
    val row = rememberLazyListState()
    val activeTarget = targets[featured?.pkg]
    val chess = featured?.pkg == "com.amine.chessmath"
    Box(Modifier.fillMaxSize()) {
        if (chess) Image(painterResource(R.drawable.chess_hero), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize().drawWithCache {
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
                Surface(onClick = { profileMenuOpen = true },
                    modifier = Modifier.testTag("home-profile").focusRequester(profiles).focusProperties {
                        up = FocusRequester.Cancel
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
                        down = activeTarget ?: FocusRequester.Cancel
                    },
                    shape = ClickableSurfaceDefaults.shape(profileShape),
                    colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent, contentColor = Ink,
                        focusedContainerColor = Panel, focusedContentColor = Ink),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(BorderStroke(2.dp, Gold), shape = profileShape))) {
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
                items(apps, key = { it.pkg }) { app ->
                    AppTile(app, Modifier.focusRequester(targets.getValue(app.pkg)).focusProperties {
                        up = profiles; down = FocusRequester.Cancel
                        if (app == apps.first()) left = FocusRequester.Cancel
                        if (app == apps.last()) right = FocusRequester.Cancel
                    }.onFocusChanged { if (it.isFocused) onFocused(app.pkg) }) { onLaunch(app) }
                }
            }
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
                    colors = ListItemDefaults.colors(focusedContainerColor = Gold, focusedContentColor = Background),
                    scale = ListItemDefaults.scale(focusedScale = 1f))
                ListItem(selected = false, onClick = onManage,
                    modifier = Modifier.fillMaxWidth().focusRequester(manage).focusProperties {
                        up = switchProfile; down = FocusRequester.Cancel
                        left = FocusRequester.Cancel; right = FocusRequester.Cancel
                    },
                    headlineContent = { Text(if (viewer.isChild) "End session" else "Family configuration", fontSize = 20.sp) },
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
private fun AppTile(app: LauncherApp, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    Surface(onClick, modifier.width(if (app.banner != null) 144.dp else 81.dp).height(81.dp).testTag("app:${app.pkg}"),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(containerColor = Panel, contentColor = Ink,
            focusedContainerColor = Panel, focusedContentColor = Ink),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.045f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = .12f)), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, Gold), shape = shape)),
    ) {
        if (app.banner != null) {
            Image(app.banner.asImageBitmap(), app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        } else {
            Image(app.icon.asImageBitmap(), app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
    }
}
