package com.amine.familyhome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

@Composable
internal fun ProfileScreen(ui: FamilyUi, onSelect: (Viewer) -> Unit) {
    val first = remember { FocusRequester() }
    Box(Modifier.fillMaxSize().padding(horizontal = 38.dp, vertical = 25.dp)) {
        FamilyHeader()
        Column(Modifier.fillMaxWidth().padding(top = 62.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Who is watching?", fontSize = 42.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(7.dp))
            Text("Choose your profile to begin", color = Muted, fontSize = 20.sp)
            Spacer(Modifier.height(39.dp))
            LazyRow(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(44.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically) {
                itemsIndexed(ui.state.profiles, key = { _, profile -> profile.viewer.id }) { index, profile ->
                    val viewer = profile.viewer
                    var focused by remember { mutableStateOf(false) }
                    Surface(onClick = { onSelect(viewer) },
                        modifier = Modifier.width(172.dp)
                            .then(if (index == 0) Modifier.focusRequester(first) else Modifier)
                            .onFocusChanged { focused = it.isFocused },
                        colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent,
                            contentColor = Ink, focusedContainerColor = Color.Transparent, focusedContentColor = Ink),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.07f),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(5.dp)) {
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
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (ui.state.recovered) Text("Unfinished sessions are paused. Each child keeps the last saved balance.",
                color = Muted, fontSize = 15.sp, textAlign = TextAlign.Center)
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
    LaunchedEffect(Unit) { first.requestFocus() }
}
