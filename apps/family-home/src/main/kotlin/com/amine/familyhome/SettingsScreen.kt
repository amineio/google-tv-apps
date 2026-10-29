package com.amine.familyhome

import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private data class SettingsRow(val key: String, val title: String, val subtitle: String = "",
    val checked: Boolean? = null, val profile: FamilyProfile? = null, val value: String? = null,
    val action: (() -> Unit)? = null)

/** All settings stay inside the authenticated parent route. Android owns device configuration. */
@Composable
internal fun SettingsScreen(ui: FamilyUi, apps: List<LauncherApp>, helperActive: Boolean,
    homeRoleHeld: Boolean, resumeGeneration: Int, restoreFocus: Boolean = true,
    onClose: () -> Unit, onPin: () -> Unit, onAccess: (Viewer, String, Boolean) -> Unit,
    onAddChild: suspend (String) -> Viewer?, onSetup: () -> Unit, onHome: () -> Unit,
    onVoiceEnabled: (Boolean) -> Unit = {}, onSpeechSetup: () -> Unit = {}) {
    var destination by rememberSaveable { mutableStateOf("root") }
    var profileFocus by rememberSaveable { mutableStateOf<String?>(null) }
    val pages = rememberSaveableStateHolder()
    fun back() {
        destination = when {
            destination.startsWith("child:") || destination == "add" -> "profiles"
            destination.startsWith("usage:") -> "usage"
            destination != "root" -> "root"
            else -> { onClose(); "root" }
        }
    }
    BackHandler(enabled = restoreFocus) { back() }
    val child = destination.removePrefix("child:").takeIf { destination.startsWith("child:") }
        ?.let { id -> ui.state.children.firstOrNull { it.viewer.id == id } }
    val usageProfile = destination.removePrefix("usage:").takeIf { destination.startsWith("usage:") }
        ?.let { id -> ui.state.profiles.firstOrNull { it.viewer.id == id } }
    val today = remember(ui.now) { LocalDate.now() }
    val title = when (destination) {
        "profiles" -> "Profiles"
        "device" -> "Device setup"
        "voice" -> "Timer voice"
        "usage" -> "Daily usage"
        "add" -> "Add child profile"
        else -> usageProfile?.name ?: child?.name ?: "Settings"
    }
    val description = when (destination) {
        "profiles" -> "Choose a child to manage allowed apps."
        "device" -> "Android controls the Home app and accessibility helper."
        "voice" -> "Spoken session updates use an installed offline voice. Visual timer warnings stay active."
        "add" -> "Create a child profile with the default allowed apps."
        "usage" -> "Today"
        else -> if (usageProfile != null) "Weekly history" else if (child != null) "Allowed apps" else "Family Home"
    }
    Row(Modifier.fillMaxSize().padding(start = 48.dp, end = 48.dp, top = 58.dp, bottom = 42.dp),
        horizontalArrangement = Arrangement.spacedBy(44.dp)) {
        Column(Modifier.weight(.38f).padding(top = 14.dp)) {
            Text(title, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 3, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(16.dp))
            Text(description, color = Muted, fontSize = 19.sp, lineHeight = 25.sp)
            if (destination == "usage" || usageProfile != null) {
                Spacer(Modifier.height(20.dp))
                Text(if (usageProfile != null) "Up to 30 recorded days" else "Selected-profile screen-on time",
                    color = Muted, fontSize = 16.sp, lineHeight = 22.sp)
            }
            if (destination == "device") {
                Spacer(Modifier.height(24.dp))
                Text(if (helperActive && homeRoleHeld) "Ready" else "Setup needed", color = Gold, fontSize = 19.sp)
            }
        }
        Column(Modifier.weight(.62f)) {
            key(destination) {
            pages.SaveableStateProvider(destination) {
                if (destination == "add") {
                    AddChildForm(ui, restoreFocus, onAddChild) { viewer ->
                        profileFocus = "profile:${viewer.id}"
                        destination = "profiles"
                    }
                } else if (usageProfile != null) {
                    UsageHistoryScreen(ui.state.usage, usageProfile.viewer, today, resumeGeneration, restoreFocus)
                } else {
                    val rows = when (destination) {
                        "root" -> listOf(
                            SettingsRow("profiles", "Profiles", "Allowed apps and child profiles") { destination = "profiles" },
                            SettingsRow("usage", "Daily usage", "Screen-on time by profile") { destination = "usage" },
                            SettingsRow("pin", "Parent PIN", "Protect parent controls and new sessions", ui.state.pinEnabled, action = onPin),
                            SettingsRow("device", "Device setup", "Home helper and default Home app") { destination = "device" },
                            SettingsRow("voice", "Timer voice", if (ui.state.voiceAnnouncementsEnabled) "On · Offline spoken timer updates" else "Muted") { destination = "voice" })
                        "usage" -> ui.state.profiles.map { profile ->
                            SettingsRow("usage:${profile.viewer.id}", profile.name, profile = profile,
                                value = usageTotal(ui.state.usage[today.toString()]?.get(profile.viewer))) {
                                destination = "usage:${profile.viewer.id}"
                            }
                        }
                        "profiles" -> ui.state.children.map { profile ->
                            SettingsRow("profile:${profile.viewer.id}", profile.name, "Allowed apps", profile = profile) {
                                destination = "child:${profile.viewer.id}"
                            }
                        } + SettingsRow("add", "Add child profile", "Name and initials portrait") { destination = "add" }
                        "voice" -> listOf(
                            SettingsRow("voice-enabled", "Voice announcements", if (ui.state.voiceAnnouncementsEnabled) ui.speechStatus.label else "Muted",
                                checked = ui.state.voiceAnnouncementsEnabled) { onVoiceEnabled(!ui.state.voiceAnnouncementsEnabled) },
                            SettingsRow("speech-setup", "Android speech setup", "Choose or install a voice in Android settings", action = onSpeechSetup))
                        "device" -> listOf(
                            SettingsRow("helper", "Home helper", if (helperActive) "Enabled · Open Android accessibility" else "Not enabled · Open Android accessibility", action = onSetup),
                            SettingsRow("home", "Default Home app", if (homeRoleHeld) "Family Home · Open Android Home selection" else "Not Family Home · Open Android Home selection", action = onHome))
                        else -> child?.let { profile -> apps.map { app ->
                            val allowed = app.pkg in ui.state.allowed[profile.viewer].orEmpty()
                            SettingsRow("app:${app.pkg}", app.label, checked = allowed) {
                                onAccess(profile.viewer, app.pkg, !allowed)
                            }
                        } }.orEmpty()
                    }
                    val list = rememberLazyListState()
                    var focused by rememberSaveable { mutableStateOf(rows.firstOrNull()?.key) }
                    if (destination == "profiles" && profileFocus != null) {
                        focused = profileFocus
                        profileFocus = null
                    }
                    SettingsList(rows, list, focused, { focused = it }, resumeGeneration, restoreFocus,
                        header = if (destination == "profiles") ({
                            ui.state.profiles.filter { it.viewer.isAdult }.forEach { adult ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    ProfilePortrait(adult.viewer, 42.dp, displayName = adult.name, portrait = adult.portrait)
                                    Column {
                                        Text(adult.name, fontSize = 21.sp)
                                        Text("Parent · All installed TV apps", color = Muted, fontSize = 16.sp)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }) else null,
                        footer = if (child != null) ({
                            val missing = DefaultApps.labels.filterKeys { pkg -> apps.none { it.pkg == pkg } }
                            if (apps.isEmpty()) Text("No TV apps are installed.", color = Muted, fontSize = 18.sp,
                                modifier = Modifier.padding(16.dp))
                            missing.forEach { (pkg, name) ->
                                Text("$name · Not installed", color = Muted, fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).testTag("unavailable:$pkg"))
                            }
                        }) else null)
                }
            }
            }
            ui.notice?.let { Text(it, color = Gold, fontSize = 16.sp, modifier = Modifier.padding(top = 12.dp)) }
        }
    }
}

@Composable
private fun SettingsList(rows: List<SettingsRow>, list: LazyListState, focused: String?,
    onFocused: (String) -> Unit, resume: Int, restoreFocus: Boolean,
    header: (@Composable () -> Unit)? = null, footer: (@Composable () -> Unit)? = null) {
    val keys = rows.map { it.key }
    val targets = remember(keys) { keys.associateWith { FocusRequester() } }
    LazyColumn(state = list, modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).testTag("settings-list"),
        contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (header != null) item(key = "header") { header() }
        items(rows, key = { it.key }) { row ->
            if (row.action == null) {
                var isFocused by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("setting:${row.key}")
                    .focusRequester(targets.getValue(row.key))
                    .focusProperties { left = FocusRequester.Cancel; right = FocusRequester.Cancel }
                    .onFocusChanged {
                        isFocused = it.isFocused
                        if (it.isFocused) onFocused(row.key)
                    }
                    .background(if (isFocused) Gold else Panel, RoundedCornerShape(8.dp))
                    .semantics(mergeDescendants = true) { }
                    .focusable().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(row.title, Modifier.weight(1f), color = if (isFocused) Background else Ink,
                        fontSize = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    row.value?.let { Text(it, color = if (isFocused) Background else Ink, fontSize = 19.sp) }
                }
            } else ListItem(selected = false, onClick = row.action,
                modifier = Modifier.fillMaxWidth().testTag("setting:${row.key}")
                    .focusRequester(targets.getValue(row.key))
                    .focusProperties { left = FocusRequester.Cancel; right = FocusRequester.Cancel }
                    .onFocusChanged { if (it.isFocused) onFocused(row.key) }
                    .semantics { row.checked?.let { stateDescription = if (it) "On" else "Off"; role = Role.Switch
                        toggleableState = if (it) ToggleableState.On else ToggleableState.Off } },
                headlineContent = { Text(row.title, fontSize = 21.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                supportingContent = row.subtitle.takeIf { it.isNotEmpty() }?.let { subtitle -> {
                    Text(subtitle, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                } },
                leadingContent = row.profile?.let { profile -> {
                    ProfilePortrait(profile.viewer, 42.dp, displayName = profile.name, portrait = profile.portrait)
                } },
                trailingContent = {
                    if (row.checked != null) Switch(checked = row.checked, onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Gold, checkedTrackColor = Panel,
                            checkedBorderColor = Muted, checkedIconColor = Background,
                            uncheckedThumbColor = Muted, uncheckedTrackColor = Panel,
                            uncheckedBorderColor = Muted, uncheckedIconColor = Background),
                        modifier = Modifier.focusProperties { canFocus = false }.clearAndSetSemantics { })
                    else Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        row.value?.let { Text(it, fontSize = 18.sp) }
                        Text("›", fontSize = 27.sp)
                    }
                },
                colors = ListItemDefaults.colors(focusedContainerColor = Gold, focusedContentColor = Background),
                scale = ListItemDefaults.scale(focusedScale = 1f))
        }
        if (footer != null) item(key = "footer") { footer() }
    }
    LaunchedEffect(keys, resume, restoreFocus) {
        if (restoreFocus && keys.isNotEmpty()) {
            withFrameNanos { }
            val key = focused?.takeIf { it in keys } ?: keys.first()
            val index = keys.indexOf(key) + if (header == null) 0 else 1
            if (list.layoutInfo.visibleItemsInfo.none { it.index == index }) list.scrollToItem(index)
            withFrameNanos { }
            targets.getValue(key).requestFocus()
        }
    }
}

@Composable
private fun AddChildForm(ui: FamilyUi, restoreFocus: Boolean, onAdd: suspend (String) -> Viewer?,
    onCreated: (Viewer) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    val save = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        ProfilePortrait(Viewer("preview"), 72.dp, displayName = name.trim().ifEmpty { "Child" }, portrait = Portrait.CHILD)
        Text("Child name", color = Muted, fontSize = 17.sp)
        BasicTextField(value = name, onValueChange = { name = it; error = null }, singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 23.sp, fontFamily = Outfit), cursorBrush = SolidColor(Gold),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { keyboard?.hide(); save.requestFocus() }),
            modifier = Modifier.fillMaxWidth().testTag("child-name").focusRequester(field).onFocusChanged { fieldFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                        keyboard?.hide(); save.requestFocus(); true
                    } else false
                }
                .border(if (fieldFocused) 2.dp else 1.dp, if (fieldFocused) Gold else Muted, RoundedCornerShape(8.dp)).background(Panel, RoundedCornerShape(8.dp)).padding(16.dp)
                .semantics { contentDescription = "Child name" })
        Text("1–24 characters. Use a different name for each profile.", color = Muted, fontSize = 15.sp)
        error?.let { Text(it, color = Gold, fontSize = 16.sp) }
        TvAction("Save", Modifier.focusRequester(save).focusProperties {
            up = field; left = FocusRequester.Cancel; right = FocusRequester.Cancel; down = FocusRequester.Cancel
        }, enabled = !saving) {
            val trimmed = name.trim()
            error = when {
                name.any { it.isISOControl() } || !validProfileName(trimmed) -> "Enter a name with 1–24 characters and no control characters."
                ui.state.profiles.any { it.name.equals(trimmed, ignoreCase = true) } -> "A profile already uses this name."
                else -> null
            }
            if (error == null) {
                keyboard?.hide()
                saving = true
                scope.launch {
                    // Explicit Save must finish even if Back leaves the form during the write.
                    val viewer = withContext(NonCancellable) { onAdd(name) }
                    if (isActive) {
                        saving = false
                        if (viewer != null) {
                            name = ""
                            error = null
                            onCreated(viewer)
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(restoreFocus) { if (restoreFocus) field.requestFocus() }
}
