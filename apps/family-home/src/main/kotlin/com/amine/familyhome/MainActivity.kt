package com.amine.familyhome

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LauncherApp(val pkg: String, val label: String, val component: android.content.ComponentName, val icon: Bitmap)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FamilyTheme { FamilyScreen(this) }
        }
    }

    override fun onResume() {
        super.onResume()
        family.helperConnected(FamilyAccessService.active && getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME))
    }

    fun installedApps(): List<LauncherApp> {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return packageManager.queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(0)).mapNotNull { info ->
            if (info.activityInfo.packageName == packageName) return@mapNotNull null
            val drawable = info.loadIcon(packageManager)
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, 64, 64); drawable.draw(Canvas(bitmap))
            LauncherApp(info.activityInfo.packageName,
                info.loadLabel(packageManager).toString().filter { !it.isISOControl() }.take(64),
                android.content.ComponentName(info.activityInfo.packageName, info.activityInfo.name), bitmap)
        }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    }

    fun launch(app: LauncherApp) {
        if (!family.mayLaunch(app.pkg)) return
        try {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setComponent(app.component))
        } catch (_: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(this, "This app is unavailable. Refresh the app list.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

private data class ParentAction(val title: String, val message: String, val needsPin: Boolean,
    val action: suspend (String) -> Boolean)

@Composable
fun FamilyScreen(activity: MainActivity) {
    val controller = activity.family
    val ui by controller.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<LauncherApp>>(emptyList()) }
    var configuration by remember { mutableStateOf(false) }
    var durationFor by remember { mutableStateOf<Viewer?>(null) }
    var parentAction by remember { mutableStateOf<ParentAction?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val selected = ui.state.selected
    LaunchedEffect(refresh, selected) {
        apps = withContext(Dispatchers.IO) { activity.installedApps() }
        if (selected != Viewer.AMINE) configuration = false
        durationFor = null
    }
    fun protect(title: String, message: String, action: suspend (String) -> Boolean) {
        parentAction = ParentAction(title, message, ui.state.pinEnabled, action)
    }
    BackHandler {
        when {
            parentAction != null -> parentAction = null
            configuration -> configuration = false
            durationFor != null -> durationFor = null
            selected?.isChild == true && !ui.state.sessions.containsKey(selected) -> scope.launch { controller.chooseProfiles() }
            selected == Viewer.AMINE -> scope.launch { controller.chooseProfiles() }
        }
    }
    FamilyBackdrop {
    if (ui.ready && !ui.storageFailed && selected == null) {
        ProfileScreen(ui) { viewer ->
            if (!viewer.isChild && ui.state.pinEnabled) {
                protect("Parent PIN", "Enter the PIN to open Amine.") { controller.select(viewer, it) }
            } else scope.launch { controller.select(viewer) }
        }
    } else Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 26.dp)) {
        FamilyHeader()
        Spacer(Modifier.height(16.dp))
        if (!ui.ready) {
            Text("Opening your family profiles…", fontSize = 28.sp)
        } else if (ui.storageFailed) {
            Text("Saved state needs attention", fontSize = 30.sp)
            Spacer(Modifier.height(16.dp))
            Text(ui.notice.orEmpty(), color = Muted, fontSize = 20.sp)
            Spacer(Modifier.height(24.dp))
            ActionButton("Retry saved state") { scope.launch { controller.load() } }
        } else if (selected == null) {
            // The chooser is rendered by ProfileScreen above.
        } else if (configuration) {
            Configuration(ui, apps, onBack = { configuration = false }, onPin = {
                parentAction = if (ui.state.pinEnabled) ParentAction("Turn off the parent PIN?", "New sessions will use family trust.", false) {
                    controller.setPin(null)
                } else ParentAction("Set a parent PIN", "Choose four digits. This protects Amine and new sessions.", true) {
                    controller.setPin(it)
                }
            }, onAccess = { viewer, pkg, enabled -> scope.launch { controller.setAllowed(viewer, pkg, enabled) } },
                onSetup = { activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                onHome = { activity.startActivity(activity.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME)) })
        } else if (selected.isChild && (durationFor == selected || !ui.state.sessions.containsKey(selected))) {
            DurationScreen(selected, onCancel = {
                durationFor = null
                if (!ui.state.sessions.containsKey(selected)) scope.launch { controller.chooseProfiles() }
            }, onStart = { minutes ->
                if (ui.state.pinEnabled) protect("Parent PIN", "Approve ${minutes} minutes for ${selected.label}.") {
                    controller.start(selected, minutes, it).also { ok -> if (ok) durationFor = null }
                } else scope.launch { if (controller.start(selected, minutes)) durationFor = null }
            })
        } else if (selected.isChild && SessionEngine.remaining(ui.state, selected, ui.now) == 0L) {
            Text("Time is up, ${selected.label}", fontSize = 38.sp)
            Spacer(Modifier.height(16.dp))
            Text("Your session has ended.", color = Muted, fontSize = 23.sp)
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ActionButton("Switch profile", initial = true) { scope.launch { controller.chooseProfiles() } }
                ActionButton("Start a new session") { durationFor = selected }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(selected.label, fontSize = 34.sp)
                    Text(if (selected.isChild) "${timeLeft(SessionEngine.remaining(ui.state, selected, ui.now))} left" else "Your apps",
                        color = if (selected.isChild) Gold else Muted, fontSize = 23.sp)
                }
                ActionButton("Switch profile") { scope.launch { controller.chooseProfiles() } }
                Spacer(Modifier.width(16.dp))
                if (!selected.isChild) ActionButton("Family configuration") { configuration = true }
                else ActionButton("End session") {
                    protect("End ${selected.label}'s session?", "The remaining time will be discarded.") { controller.end(selected, it) }
                }
            }
            Spacer(Modifier.height(24.dp))
            val allowedApps = if (!selected.isChild) apps else apps.filter { it.pkg in ui.state.allowed[selected].orEmpty() }
            if (allowedApps.isEmpty()) Text("No allowed apps are installed. Ask Amine to review the app list.", color = Muted, fontSize = 22.sp)
            LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.weight(1f)) {
                items(allowedApps, key = { it.pkg }) { app ->
                    Button(onClick = { activity.launch(app) }, modifier = Modifier.fillMaxWidth().height(100.dp).testTag("app:${app.pkg}")) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(app.icon.asImageBitmap(), contentDescription = null, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.width(16.dp))
                            Text(app.label, fontSize = 22.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        ui.notice?.let { Spacer(Modifier.height(14.dp)); Text(it, color = Gold, fontSize = 18.sp) }
    }
    }
    parentAction?.let { action ->
        ParentDialog(action, onDismiss = { parentAction = null }, onSubmit = { pin ->
            scope.launch { if (action.action(pin)) parentAction = null }
        }, notice = ui.notice)
    }
}

@Composable
private fun InitialFocus(initial: Boolean, content: @Composable (Modifier) -> Unit) {
    val focus = remember { FocusRequester() }
    content(Modifier.focusRequester(focus))
    LaunchedEffect(initial) { if (initial) focus.requestFocus() }
}

@Composable
private fun ActionButton(text: String, initial: Boolean = false, onClick: () -> Unit) {
    InitialFocus(initial) { focus ->
        TvAction(text, modifier = focus, onClick = onClick)
    }
}

@Composable
private fun DurationScreen(viewer: Viewer, onCancel: () -> Unit, onStart: (Int) -> Unit) {
    var minutes by remember(viewer) { mutableIntStateOf(20) }
    Text("Time for ${viewer.label}", fontSize = 38.sp)
    Text("Choose up to one hour.", color = Muted, fontSize = 22.sp)
    Spacer(Modifier.height(30.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        ActionButton("− 5", initial = true) { minutes = (minutes - 5).coerceAtLeast(5) }
        Text("$minutes minutes", color = Gold, fontSize = 36.sp, modifier = Modifier.width(225.dp).testTag("duration-value"))
        ActionButton("+ 5") { minutes = (minutes + 5).coerceAtMost(60) }
    }
    Spacer(Modifier.height(32.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        ActionButton("Start session") { onStart(minutes) }
        ActionButton("Cancel") { onCancel() }
    }
}

@Composable
private fun Configuration(ui: FamilyUi, apps: List<LauncherApp>, onBack: () -> Unit, onPin: () -> Unit,
    onAccess: (Viewer, String, Boolean) -> Unit, onSetup: () -> Unit, onHome: () -> Unit) {
    var child by remember { mutableStateOf(Viewer.ELIAS) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Family configuration", fontSize = 30.sp, modifier = Modifier.weight(1f))
        ActionButton("Back", onClick = onBack)
    }
    Spacer(Modifier.height(18.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ActionButton("Parent PIN: ${if (ui.state.pinEnabled) "on" else "off"}", initial = true, onClick = onPin)
        ActionButton("Enable helper", onClick = onSetup)
        ActionButton("Select Home", onClick = onHome)
    }
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Allowed apps for", color = Muted, fontSize = 20.sp)
        ActionButton(if (child == Viewer.ELIAS) "Elias · selected" else "Elias") { child = Viewer.ELIAS }
        ActionButton(if (child == Viewer.SOFIA) "Sofia · selected" else "Sofia") { child = Viewer.SOFIA }
    }
    Spacer(Modifier.height(14.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val installed = apps.map { it.pkg }.toSet()
        val missing = DefaultApps.labels.filterKeys { it !in installed }
        items(missing.entries.toList(), key = { "missing:${it.key}" }) { entry ->
            Text("${entry.value}: not installed", color = Muted, fontSize = 18.sp)
        }
        items(apps, key = { it.pkg }) { app ->
            val allowed = app.pkg in ui.state.allowed[child].orEmpty()
            Button(onClick = { onAccess(child, app.pkg, !allowed) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("${if (allowed) "Allowed" else "Hidden"}    ${app.label}", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun ParentDialog(action: ParentAction, onDismiss: () -> Unit, onSubmit: (String) -> Unit, notice: String?) {
    var pin by remember(action) { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.width(500.dp).background(Panel, RoundedCornerShape(20.dp)).padding(24.dp)) {
            Text(action.title, fontSize = 26.sp)
            Text(action.message, color = Muted, fontSize = 17.sp)
            if (action.needsPin) {
                Text("●".repeat(pin.length).padEnd(4, '○'), color = Gold, fontSize = 26.sp,
                    modifier = Modifier.padding(vertical = 12.dp))
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("Clear", "0", "Delete")).forEachIndexed { row, keys ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        keys.forEachIndexed { column, key ->
                            InitialFocus(row == 0 && column == 0) { focus ->
                                Button(onClick = { pin = when (key) {
                                    "Clear" -> ""
                                    "Delete" -> pin.dropLast(1)
                                    else -> (pin + key).take(4)
                                } }, modifier = focus.weight(1f).height(40.dp)) { Text(key, fontSize = 17.sp) }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            notice?.let { Text(it, color = Gold, fontSize = 16.sp) }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionButton("Cancel", initial = !action.needsPin, onClick = onDismiss)
                Button(onClick = { onSubmit(pin) }, enabled = !action.needsPin || pin.length == 4) { Text("Continue", fontSize = 18.sp) }
            }
        }
    }
}

private fun timeLeft(milliseconds: Long): String {
    val seconds = (milliseconds + 999) / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
