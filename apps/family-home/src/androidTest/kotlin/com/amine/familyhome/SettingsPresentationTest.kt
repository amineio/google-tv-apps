package com.amine.familyhome

import android.content.ComponentName
import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** These fixtures never use the application's DataStore or create a real family profile. */
class SettingsPresentationTest {
    @get:Rule val screen = createComposeRule()

    private class SavedFixture : FamilyRepository {
        var text = FamilyStateCodec.encode(FamilyState())
        var failWrites = false
        var saveStarted: CompletableDeferred<Unit>? = null
        var finishSave: CompletableDeferred<Unit>? = null
        override suspend fun load() = FamilyStateCodec.decode(text)
        override suspend fun save(state: FamilyState) {
            saveStarted?.complete(Unit)
            finishSave?.await()
            check(!failWrites) { "Fixture write failure" }
            text = FamilyStateCodec.encode(state)
        }
    }

    private fun controller(repository: SavedFixture) = FamilyController(repository) { 0L }.also {
        runBlocking { assertTrue(it.load()); assertTrue(it.select(Viewer.AMINE)) }
    }

    private fun app(index: Int, label: String = "App $index") = LauncherApp("example.app$index", label,
        ComponentName("example.app$index", "example.app$index.Main"),
        Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888))

    private fun show(controller: FamilyController, apps: List<LauncherApp>, restore: StateRestorationTester? = null,
        helper: Boolean = true, home: Boolean = true, setup: () -> Unit = {}, homeAction: () -> Unit = {}) {
        val content: @Composable () -> Unit = {
            val ui by controller.ui.collectAsState()
            val scope = rememberCoroutineScope()
            FamilyTheme {
                FamilyBackdrop {
                    SettingsScreen(ui, apps, helper, home, 0, onClose = {}, onPin = {},
                        onAccess = { viewer, pkg, enabled -> scope.launch { controller.setAllowed(viewer, pkg, enabled) } },
                        onAddChild = controller::addChild, onSetup = setup, onHome = homeAction)
                }
            }
        }
        if (restore == null) screen.setContent(content) else restore.setContent(content)
    }

    private fun capture(name: String) {
        val directory = requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null))
        directory.resolve("$name.png").outputStream().use {
            screen.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun key(key: Key) {
        screen.onRoot().performKeyInput { pressKey(key) }
        screen.waitForIdle()
    }
    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        screen.waitForIdle()
    }
    private fun openFirstChild() { key(Key.DirectionCenter); key(Key.DirectionCenter) }
    private fun addForm(childCount: Int = 2) {
        key(Key.DirectionCenter)
        repeat(childCount) { key(Key.DirectionDown) }
        screen.onNodeWithText("Add child profile").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("child-name").assertIsFocused()
    }
    private fun assertSwitch(key: String, state: String) = screen.onNodeWithTag("setting:$key")
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state))

    @Test fun allowedAppTogglePersistsAndBackRestoresScrolledRowAcrossRecreation() {
        val repository = SavedFixture()
        val controller = controller(repository)
        val restore = StateRestorationTester(screen)
        show(controller, (0..11).map { app(it, if (it == 9) "A long application title that must wrap and remain readable" else "App $it") }, restore)
        openFirstChild()
        repeat(9) { key(Key.DirectionDown) }
        screen.onNodeWithTag("setting:app:example.app9").assertIsFocused().assertIsDisplayed()
        assertSwitch("app:example.app9", "Off")
        key(Key.DirectionRight)
        screen.onNodeWithTag("setting:app:example.app9").assertIsFocused()
        key(Key.DirectionCenter)
        screen.waitUntil { controller.ui.value.state.allowed[Viewer.ELIAS].orEmpty().contains("example.app9") }
        assertSwitch("app:example.app9", "On")
        runBlocking { assertTrue("The toggle must reach durable state", "example.app9" in repository.load().allowed[Viewer.ELIAS].orEmpty()) }
        back()
        screen.onNodeWithTag("setting:profile:ELIAS").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("setting:app:example.app9").assertIsDisplayed().assertIsFocused()
        restore.emulateSavedInstanceStateRestore()
        screen.onNodeWithTag("setting:app:example.app9").assertIsDisplayed().assertIsFocused()
        assertSwitch("app:example.app9", "On")
        key(Key.DirectionCenter)
        screen.waitUntil { "example.app9" !in controller.ui.value.state.allowed[Viewer.ELIAS].orEmpty() }
        assertSwitch("app:example.app9", "Off")
    }

    @Test fun cancelledCreationAndInvalidNamesNeverWriteAProfile() {
        val repository = SavedFixture()
        val controller = controller(repository)
        show(controller, emptyList())
        val before = repository.text
        addForm()
        screen.onNodeWithTag("child-name").performTextInput(" ")
        key(Key.DirectionDown); key(Key.DirectionCenter)
        screen.onNodeWithText("Enter a name with 1–24 characters and no control characters.").assertIsDisplayed()
        assertEquals(before, repository.text)
        screen.onNodeWithTag("child-name").performTextReplacement("Child One")
        screen.onNodeWithTag("child-name").performImeAction()
        screen.onNodeWithTag("child-name").assertTextEquals("Child One")
        screen.onNodeWithText("Save").assertIsFocused()
        back()
        screen.onNodeWithTag("setting:add").assertIsFocused()
        assertEquals(3, controller.ui.value.state.profiles.size)
        assertEquals(before, repository.text)
    }

    @Test fun keyboardDoneAndRecreationKeepFormTextUntilExplicitSaveOrBack() {
        val repository = SavedFixture()
        val controller = controller(repository)
        val restore = StateRestorationTester(screen)
        show(controller, emptyList(), restore)
        addForm()
        screen.onNodeWithTag("child-name").performTextInput("Child Draft")
        screen.onNodeWithTag("child-name").performImeAction()
        screen.onNodeWithText("Save").assertIsFocused()
        screen.onNodeWithTag("child-name").assertTextEquals("Child Draft")
        assertEquals(3, controller.ui.value.state.profiles.size)
        restore.emulateSavedInstanceStateRestore()
        screen.onNodeWithTag("child-name").assertTextEquals("Child Draft")
        screen.onNodeWithTag("child-name").performTextReplacement("X".repeat(25))
        key(Key.DirectionDown); key(Key.DirectionCenter)
        screen.onNodeWithText("Enter a name with 1–24 characters and no control characters.").assertIsDisplayed()
        assertEquals(3, controller.ui.value.state.profiles.size)
        back()
        screen.onNodeWithTag("setting:add").assertIsFocused()
    }

    @Test fun explicitSaveCreatesExactlyOneChildAndDuplicateSaveKeepsTheForm() {
        val repository = SavedFixture()
        val controller = controller(repository)
        val restore = StateRestorationTester(screen)
        show(controller, listOf(app(0, "Example app"), app(1, "Another app")), restore)
        addForm()
        screen.onNodeWithTag("child-name").performTextInput("  Child One  ")
        screen.onNodeWithTag("child-name").performImeAction()
        capture("settings-add-profile")
        key(Key.DirectionCenter)
        screen.waitUntil { controller.ui.value.state.profiles.size == 4 }
        val child = controller.ui.value.state.children.last()
        assertEquals("Child One", child.name)
        assertEquals(Viewer.AMINE, controller.ui.value.state.selected)
        assertFalse(controller.ui.value.state.sessions.containsKey(child.viewer))
        screen.onNodeWithTag("setting:profile:${child.viewer.id}").assertIsFocused()
        capture("settings-profiles-private")
        restore.emulateSavedInstanceStateRestore()
        screen.onNodeWithTag("setting:profile:${child.viewer.id}").assertIsFocused()
        runBlocking { assertEquals(4, repository.load().profiles.size) }
        key(Key.DirectionCenter)
        screen.onNodeWithTag("setting:app:example.app0").assertIsFocused().assertIsOff()
        capture("settings-generic-child-focused-off")
        key(Key.DirectionCenter)
        screen.waitUntil { "example.app0" in controller.ui.value.state.allowed[child.viewer].orEmpty() }
        screen.onNodeWithTag("setting:app:example.app0").assertIsOn()
        screen.onNodeWithTag("setting:app:example.app1").assertIsOff()
        capture("settings-generic-child-apps")
        key(Key.DirectionDown)
        screen.onNodeWithTag("setting:app:example.app1").assertIsFocused().assertIsOff()
        screen.onNodeWithTag("setting:app:example.app0").assertIsOn()
        capture("settings-generic-child-unfocused-on")
        key(Key.DirectionUp)
        screen.onNodeWithTag("setting:app:example.app0").assertIsFocused()
        back()
        key(Key.DirectionDown); key(Key.DirectionCenter)
        screen.onNodeWithTag("child-name").performTextInput("child one")
        key(Key.DirectionDown); key(Key.DirectionCenter)
        screen.onNodeWithText("A profile already uses this name.").assertIsDisplayed()
        assertEquals(4, controller.ui.value.state.profiles.size)
        back()
        screen.onNodeWithTag("setting:add").assertIsFocused()
    }

    @Test fun emptyAppsHaveReadableUnavailableRowsAndDeviceStatusRequiresBothConditions() {
        val controller = controller(SavedFixture())
        var openedHome = 0
        show(controller, emptyList(), helper = true, home = false, homeAction = { openedHome++ })
        capture("settings-root")
        openFirstChild()
        screen.onNodeWithText("No TV apps are installed.").assertIsDisplayed()
        screen.onNodeWithTag("unavailable:com.amine.chessmath").assertIsDisplayed()
        screen.onAllNodes(hasClickAction()).assertCountEquals(0)
        back()
        screen.onNodeWithTag("setting:profile:ELIAS").assertIsFocused()
        back()
        screen.onNodeWithTag("setting:profiles").assertIsFocused()
        key(Key.DirectionDown); key(Key.DirectionDown); key(Key.DirectionDown); key(Key.DirectionCenter)
        screen.onNodeWithText("Setup needed").assertIsDisplayed()
        capture("settings-device")
        screen.onNodeWithText("Ready").assertDoesNotExist()
        screen.onNodeWithTag("setting:helper").assertIsFocused()
        key(Key.DirectionDown); key(Key.DirectionCenter)
        assertEquals(1, openedHome)
        screen.onNodeWithTag("setting:home").assertIsFocused()
        back()
        screen.onNodeWithTag("setting:device").assertIsFocused()
    }

    @Test fun backDuringExplicitSaveDoesNotCancelTheDurableCommand() {
        val repository = SavedFixture()
        val controller = controller(repository)
        show(controller, emptyList())
        addForm()
        screen.onNodeWithTag("child-name").performTextInput("Child Save")
        repository.saveStarted = CompletableDeferred()
        repository.finishSave = CompletableDeferred()
        key(Key.DirectionDown); key(Key.DirectionCenter)
        screen.waitUntil { repository.saveStarted!!.isCompleted }
        back()
        screen.onNodeWithTag("setting:add").assertIsFocused()
        repository.finishSave!!.complete(Unit)
        screen.waitUntil { controller.ui.value.state.profiles.size == 4 || controller.ui.value.storageFailed }
        assertFalse("Back must not report a cancelled durable Save as storage failure", controller.ui.value.storageFailed)
        runBlocking { assertEquals(4, repository.load().profiles.size) }
        assertEquals("Child Save", controller.ui.value.state.children.last().name)
        screen.onNodeWithTag("setting:add").assertIsFocused()
    }

    @Test fun failedAllowedAppWriteKeepsTheSavedSwitchState() {
        val repository = SavedFixture()
        val controller = controller(repository)
        show(controller, listOf(app(0)))
        openFirstChild()
        repository.failWrites = true
        key(Key.DirectionCenter)
        screen.waitUntil { controller.ui.value.storageFailed }
        assertSwitch("app:example.app0", "Off")
        runBlocking { assertFalse("example.app0" in repository.load().allowed[Viewer.ELIAS].orEmpty()) }
    }
}
