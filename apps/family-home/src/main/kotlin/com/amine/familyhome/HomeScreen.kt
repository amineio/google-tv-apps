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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
) {
    val featured = apps.firstOrNull { it.pkg == focusedPackage } ?: apps.firstOrNull()
    val keys = apps.map { it.pkg }
    val targets = remember(keys) { keys.associateWith { FocusRequester() } }
    val open = remember { FocusRequester() }
    val profiles = remember { FocusRequester() }
    val manage = remember { FocusRequester() }
    val row = rememberLazyListState()
    val activeTarget = targets[featured?.pkg]
    val chess = featured?.pkg == "com.amine.chessmath"
    Box(Modifier.fillMaxSize()) {
        if (chess) Image(painterResource(R.drawable.chess_hero), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else featured?.banner?.let { banner ->
            Image(banner.asImageBitmap(), null, Modifier.align(Alignment.TopEnd).fillMaxWidth(.6f).height(300.dp),
                contentScale = ContentScale.Fit, alpha = .6f)
            Box(Modifier.fillMaxWidth().height(330.dp).background(Brush.horizontalGradient(
                listOf(Background, Background.copy(alpha = .95f), Color.Transparent))))
            Box(Modifier.fillMaxWidth().height(340.dp).background(Brush.verticalGradient(
                listOf(Color.Transparent, Background))))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 23.dp)) {
            FamilyHeader {
                ProfilePortrait(viewer, 30.dp)
                Spacer(Modifier.width(10.dp))
                Text(viewer.label, fontSize = 18.sp)
                if (remainingMs != null) {
                    Spacer(Modifier.width(20.dp))
                    TimerChip(remainingMs)
                }
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth().height(208.dp), contentAlignment = Alignment.CenterStart) {
                Column(Modifier.fillMaxWidth(.53f)) {
                    Text(featured?.label ?: if (viewer.isChild) "Your time, your choice" else "Welcome, ${viewer.label}",
                        fontSize = 46.sp, lineHeight = 49.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(9.dp))
                    Text(if (chess) "Ready for your next move?" else "Choose something you enjoy.", color = Muted, fontSize = 20.sp)
                    if (featured != null) {
                        Spacer(Modifier.height(19.dp))
                        TvAction("Open app", Modifier.focusRequester(open).focusProperties {
                            down = activeTarget ?: FocusRequester.Default
                        }, primary = true) { onLaunch(featured) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Your apps", fontSize = 18.sp, fontWeight = FontWeight.Medium)
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
                        up = open; down = profiles
                    }.onFocusChanged { if (it.isFocused) onFocused(app.pkg) }) { onLaunch(app) }
                }
            }
            Spacer(Modifier.weight(1f))
            notice?.let { Text(it, color = Gold, fontSize = 15.sp, maxLines = 2); Spacer(Modifier.height(8.dp)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TvAction("Switch profile", Modifier.focusRequester(profiles).focusProperties {
                    up = activeTarget ?: FocusRequester.Default; right = manage
                }, onClick = onSwitchProfile)
                TvAction(if (viewer.isChild) "End session" else "Family configuration",
                    Modifier.focusRequester(manage).focusProperties {
                        up = activeTarget ?: FocusRequester.Default; left = profiles
                    }, onClick = onManage)
            }
        }
    }
    LaunchedEffect(viewer, keys, resume) {
        if (featured != null) {
            row.scrollToItem(apps.indexOfFirst { it.pkg == featured.pkg }.coerceAtLeast(0))
            withFrameNanos { }
            targets.getValue(featured.pkg).requestFocus()
        } else profiles.requestFocus()
    }
}

@Composable
private fun TimerChip(milliseconds: Long) {
    Row(Modifier.background(Color.White.copy(alpha = .065f), RoundedCornerShape(14.dp))
        .border(1.dp, Color.White.copy(alpha = .1f), RoundedCornerShape(14.dp))
        .padding(horizontal = 13.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(19.dp)) {
            val stroke = 1.5.dp.toPx()
            drawCircle(Ink, radius = size.minDimension / 2 - stroke, style = Stroke(stroke))
            drawLine(Ink, center, Offset(center.x, size.height * .23f), strokeWidth = stroke)
            drawLine(Ink, center, Offset(size.width * .72f, size.height * .64f), strokeWidth = stroke)
        }
        Spacer(Modifier.width(9.dp))
        Text("${timeLeft(milliseconds)} left", fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AppTile(app: LauncherApp, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    Surface(onClick, modifier.width(134.dp).height(86.dp).testTag("app:${app.pkg}"),
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
            Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Image(app.icon.asImageBitmap(), null, Modifier.size(38.dp))
                Spacer(Modifier.height(5.dp))
                Text(app.label, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
