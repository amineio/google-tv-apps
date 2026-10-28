package com.amine.familyhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

@Composable
internal fun ProfileScreen(ui: FamilyUi, onSelect: (Viewer) -> Unit) {
    val first = remember { FocusRequester() }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    var initialFocusApplied by remember { mutableStateOf(false) }
    var firstPlaced by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().padding(horizontal = 38.dp, vertical = 25.dp).testTag("profile-chooser")) {
        FamilyHeader()
        LazyRow(Modifier.fillMaxWidth().align(Alignment.Center),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(44.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically) {
            itemsIndexed(ui.state.profiles, key = { _, profile -> profile.viewer.id }) { index, profile ->
                val viewer = profile.viewer
                val balance = if (viewer.isChild) SessionEngine.remaining(ui.state, viewer, ui.now) else 0L
                var focused by remember { mutableStateOf(false) }
                Surface(onClick = { onSelect(viewer) },
                    modifier = Modifier.width(172.dp).testTag("profile:${viewer.id}")
                        .semantics { if (balance > 0) stateDescription = "Resume, ${timeLeft(balance)} remaining" }
                        .then(if (index == 0) Modifier.focusRequester(first) else Modifier)
                        .onGloballyPositioned { if (index == 0) firstPlaced = it.isAttached }
                        .onFocusChanged { focused = it.isFocused },
                    colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent,
                        contentColor = Ink, focusedContainerColor = Color.Transparent, focusedContentColor = Ink),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.07f),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(5.dp)) {
                        Box(Modifier.fillMaxWidth().height(30.dp), contentAlignment = Alignment.TopEnd) {
                            if (balance > 0) ResumeBadge(viewer, balance)
                        }
                        Box(Modifier.border(BorderStroke(2.dp, if (focused) Gold else Color.Transparent), CircleShape).padding(5.dp)) {
                            ProfilePortrait(viewer, 152.dp, displayName = profile.name, portrait = profile.portrait)
                        }
                        Spacer(Modifier.height(13.dp))
                        Text(profile.name, color = if (focused) Ink else Muted, fontSize = 25.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontWeight = if (focused) FontWeight.Medium else FontWeight.Normal)
                    }
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (!ui.helper) Text("Parent setup: open Amine to enable the Home helper.", color = Muted, fontSize = 15.sp)
            ui.notice?.let { Text(it, color = Gold, fontSize = 16.sp, textAlign = TextAlign.Center) }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(25.dp).border(1.dp, Muted, CircleShape), contentAlignment = Alignment.Center) {
                    Text("OK", color = Muted, fontSize = 11.sp)
                }
                Text("Select to continue", color = Muted, fontSize = 15.sp)
            }
        }
    }
    LaunchedEffect(windowFocused, firstPlaced) {
        if (windowFocused && firstPlaced && !initialFocusApplied) {
            initialFocusApplied = first.requestFocus(FocusDirection.Enter)
        }
    }
}

@Composable
private fun ResumeBadge(viewer: Viewer, balance: Long) {
    Row(Modifier.testTag("resume:${viewer.id}").clearAndSetSemantics { }
        .background(Panel, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Canvas(Modifier.size(14.dp)) {
            val stroke = 1.4.dp.toPx()
            drawCircle(Gold, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
            drawLine(Gold, center, Offset(center.x, size.height * 0.25f), stroke, StrokeCap.Round)
            drawLine(Gold, center, Offset(size.width * 0.7f, center.y), stroke, StrokeCap.Round)
        }
        Text(timeLeft(balance), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}
