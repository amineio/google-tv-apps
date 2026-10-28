package com.amine.familyhome

import android.app.role.RoleManager
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    var resumeGeneration by mutableIntStateOf(0)
        private set
    private val packages = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_REMOVED &&
                intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
            resumeGeneration++
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(packages, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }, RECEIVER_NOT_EXPORTED)
        setContent {
            FamilyTheme { FamilyScreen(this) }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeGeneration++
        family.helperConnected(FamilyAccessService.active && getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME))
    }

    override fun onDestroy() { unregisterReceiver(packages); super.onDestroy() }

    fun installedApps() = loadLauncherCatalog(packageManager, packageName)

    fun launch(app: LauncherApp) {
        if (!family.mayLaunch(app.pkg)) return
        try {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setComponent(app.component))
        } catch (_: android.content.ActivityNotFoundException) {
            resumeGeneration++
            android.widget.Toast.makeText(this, "This app is unavailable. Try another app.", android.widget.Toast.LENGTH_LONG).show()
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
    var configuration by rememberSaveable { mutableStateOf(false) }
    var restoreManageFocus by rememberSaveable { mutableStateOf(false) }
    var durationFor by remember { mutableStateOf<Viewer?>(null) }
    var parentAction by remember { mutableStateOf<ParentAction?>(null) }
    val selected = ui.state.selected
    val resume = activity.resumeGeneration
    var focusedPackage by rememberSaveable(selected) { mutableStateOf<String?>(null) }
    LaunchedEffect(resume) {
        apps = withContext(Dispatchers.IO) { activity.installedApps() }
    }
    LaunchedEffect(selected) {
        if (selected != Viewer.AMINE) configuration = false
        durationFor = null
    }
    fun protect(title: String, message: String, action: suspend (String) -> Boolean) {
        parentAction = ParentAction(title, message, ui.state.pinEnabled, action)
    }
    fun confirmSwitchProfile() {
        parentAction = ParentAction("Switch profile?", "Return to the profile chooser?", false) {
            controller.chooseProfiles()
        }
    }
    BackHandler(enabled = !configuration || parentAction != null) {
        when {
            parentAction != null -> parentAction = null
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
    } else if (ui.ready && !ui.storageFailed && selected != null && !configuration &&
        (!selected.isChild || (durationFor != selected && ui.state.sessions.containsKey(selected) &&
            SessionEngine.remaining(ui.state, selected, ui.now) > 0))) {
        HomeScreen(selected, if (selected.isChild) SessionEngine.remaining(ui.state, selected, ui.now) else null,
            orderedApps(apps, selected, ui.state.allowed[selected].orEmpty()), focusedPackage, resume,
            { focusedPackage = it; restoreManageFocus = false }, activity::launch, ::confirmSwitchProfile, {
                if (!selected.isChild) configuration = true
                else protect("End ${ui.state.label(selected)}'s session?", "The remaining time will be discarded.") { controller.end(selected, it) }
            }, ui.notice, displayName = ui.state.label(selected),
            portrait = requireNotNull(ui.state.profile(selected)).portrait,
            focusManageOnEntry = restoreManageFocus)
    } else if (ui.ready && !ui.storageFailed && selected == Viewer.AMINE && configuration) {
        SettingsScreen(ui, apps, FamilyAccessService.active,
            activity.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME), resume,
            restoreFocus = parentAction == null,
            onClose = { configuration = false; restoreManageFocus = true }, onPin = {
                parentAction = if (ui.state.pinEnabled) ParentAction("Turn off the parent PIN?", "New sessions will use family trust.", false) {
                    controller.setPin(null)
                } else ParentAction("Set a parent PIN", "Choose four digits. This protects Amine and new sessions.", true) {
                    controller.setPin(it)
                }
            }, onAccess = { viewer, pkg, enabled -> scope.launch { controller.setAllowed(viewer, pkg, enabled) } },
            onAddChild = controller::addChild,
            onSetup = { activity.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            onHome = { activity.startActivity(activity.getSystemService(RoleManager::class.java).createRequestRoleIntent(RoleManager.ROLE_HOME)) })
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
        } else if (selected.isChild && (durationFor == selected || !ui.state.sessions.containsKey(selected))) {
            DurationScreen(selected, modifier = Modifier.weight(1f), onStart = { minutes ->
                if (ui.state.pinEnabled) protect("Parent PIN", "Approve ${minutes} minutes for ${ui.state.label(selected)}.") {
                    controller.start(selected, minutes, it).also { ok -> if (ok) durationFor = null }
                } else scope.launch { if (controller.start(selected, minutes)) durationFor = null }
            }, displayName = ui.state.label(selected), portrait = requireNotNull(ui.state.profile(selected)).portrait)
        } else if (selected.isChild && SessionEngine.remaining(ui.state, selected, ui.now) == 0L) {
            Text("Time is up, ${ui.state.label(selected)}", fontSize = 38.sp)
            Spacer(Modifier.height(16.dp))
            Text("Your session has ended.", color = Muted, fontSize = 23.sp)
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                ActionButton("Switch profile", initial = true, onClick = ::confirmSwitchProfile)
                ActionButton("Start a new session") { durationFor = selected }
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
private fun ActionButton(text: String, initial: Boolean = false, primary: Boolean = false, onClick: () -> Unit) {
    InitialFocus(initial) { focus ->
        TvAction(text, modifier = focus, primary = primary, onClick = onClick)
    }
}

@Composable
internal fun DurationScreen(viewer: Viewer, onStart: (Int) -> Unit,
    modifier: Modifier = Modifier, displayName: String = viewer.label, portrait: Portrait = viewer.defaultPortrait) {
    var minutes by remember(viewer) { mutableIntStateOf(20) }
    val decrease = remember { FocusRequester() }
    val increase = remember { FocusRequester() }
    val start = remember { FocusRequester() }
    Column(modifier.fillMaxSize().padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        ProfilePortrait(viewer, 76.dp, displayName = displayName, portrait = portrait)
        Spacer(Modifier.height(8.dp))
        Text("Time for $displayName", fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            TvAction("− 5", Modifier.width(84.dp).focusRequester(decrease).focusProperties {
                left = FocusRequester.Cancel; right = increase
                up = FocusRequester.Cancel; down = start
            }) { minutes = (minutes - 5).coerceAtLeast(5) }
            Text("$minutes minutes", color = Ink, fontSize = 36.sp, textAlign = TextAlign.Center,
                modifier = Modifier.width(200.dp).testTag("duration-value"))
            TvAction("+ 5", Modifier.width(84.dp).focusRequester(increase).focusProperties {
                left = decrease; right = FocusRequester.Cancel
                up = FocusRequester.Cancel; down = start
            }) { minutes = (minutes + 5).coerceAtMost(60) }
        }
        Spacer(Modifier.height(24.dp))
        TvAction("Start session", Modifier.focusRequester(start).focusProperties {
            up = increase; down = FocusRequester.Cancel
            left = FocusRequester.Cancel; right = FocusRequester.Cancel
        }) { onStart(minutes) }
    }
    LaunchedEffect(viewer) { start.requestFocus() }
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
                                TvAction(key, focus.weight(1f)) { pin = when (key) {
                                    "Clear" -> ""
                                    "Delete" -> pin.dropLast(1)
                                    else -> (pin + key).take(4)
                                } }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            notice?.let { Text(it, color = Gold, fontSize = 16.sp) }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                InitialFocus(!action.needsPin) { focus ->
                    TvAction("Continue", modifier = focus, primary = true,
                        enabled = !action.needsPin || pin.length == 4) { onSubmit(pin) }
                }
            }
        }
    }
}

internal fun timeLeft(milliseconds: Long): String {
    val seconds = (milliseconds + 999) / 1_000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
