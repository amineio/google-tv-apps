package com.amine.familyhome

import android.content.ComponentName
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AppOrderPresentationTest {
    @get:Rule val screen = createComposeRule()
    private fun app(index: Int) = LauncherApp("com.example.app$index", "App $index",
        ComponentName("com.example.app$index", "Main"), Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888))
    private fun press(key: Key) {
        screen.onRoot().performKeyInput { pressKey(key) }
        screen.waitForIdle()
    }
    private fun holdOk() {
        // Real held remote events exercise Surface's native long-click and release handling.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val down = SystemClock.uptimeMillis()
        instrumentation.sendKeySync(KeyEvent(down, down, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        SystemClock.sleep(650)
        instrumentation.sendKeySync(KeyEvent(down, SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_DPAD_CENTER, 1, 0, -1, 0, KeyEvent.FLAG_LONG_PRESS))
        instrumentation.sendKeySync(KeyEvent(down, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        screen.waitForIdle()
    }
    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        screen.waitForIdle()
    }
    private fun tile(index: Int) = screen.onNodeWithTag("app:com.example.app$index")
    private fun assertBefore(first: Int, second: Int) {
        assertTrue(tile(first).fetchSemanticsNode().boundsInRoot.left < tile(second).fetchSemanticsNode().boundsInRoot.left)
    }

    @Test fun heldOkMovesWithoutLaunchAndShortOkSavesExactlyOnce() {
        val apps = (0..2).map(::app)
        var focused by mutableStateOf(apps.first().pkg)
        val launches = mutableListOf<String>()
        val saves = mutableListOf<List<String>>()
        screen.setContent { FamilyTheme {
            HomeScreen(Viewer.AMINE, null, apps, focused, 0, { focused = it }, { launches += it.pkg }, {}, {},
                onSaveOrder = { saves += it; true })
        } }
        tile(0).assertIsFocused()
        holdOk()
        screen.onNodeWithTag("app-move-hint").assertIsDisplayed()
        assertTrue(launches.isEmpty()); assertTrue(saves.isEmpty())
        press(Key.DirectionUp); tile(0).assertIsFocused()
        press(Key.DirectionDown); tile(0).assertIsFocused()
        press(Key.DirectionLeft); tile(0).assertIsFocused()
        press(Key.DirectionRight); tile(0).assertIsFocused().assertIsDisplayed()
        assertBefore(1, 0)
        press(Key.DirectionCenter)
        assertEquals(listOf(listOf(apps[1].pkg, apps[0].pkg, apps[2].pkg)), saves)
        assertTrue(launches.isEmpty())
        screen.onNodeWithTag("app-move-hint").assertDoesNotExist()
        press(Key.DirectionCenter)
        assertEquals(listOf(apps[0].pkg), launches)
        assertEquals(1, saves.size)
    }

    @Test fun draftScrollsBeyondViewportStopsAtBoundaryAndBackRestoresOrder() {
        val apps = (0..15).map(::app)
        var focused by mutableStateOf(apps.first().pkg)
        var writes = 0
        screen.setContent { FamilyTheme {
            HomeScreen(Viewer.AMINE, null, apps, focused, 0, { focused = it }, {}, {}, {},
                onSaveOrder = { writes++; true })
        } }
        holdOk()
        repeat(18) { press(Key.DirectionRight); tile(0).assertIsFocused().assertIsDisplayed() }
        assertBefore(15, 0)
        assertEquals(0, writes)
        back()
        tile(0).assertIsFocused().assertIsDisplayed()
        screen.onNodeWithTag("app-move-hint").assertDoesNotExist()
        assertBefore(0, 1)
        assertEquals(0, writes)
    }

    @Test fun profileAndCatalogChangesCancelDraftAndFailedSaveKeepsOriginalOrder() {
        val original = (0..3).map(::app)
        var apps by mutableStateOf(original)
        var viewer by mutableStateOf(Viewer.AMINE)
        var focused by mutableStateOf(original.first().pkg)
        var writes = 0
        screen.setContent { FamilyTheme {
            HomeScreen(viewer, null, apps, focused, 0, { focused = it }, {}, {}, {},
                onSaveOrder = { writes++; false })
        } }
        holdOk(); press(Key.DirectionRight); press(Key.DirectionCenter)
        assertEquals(1, writes)
        screen.onNodeWithTag("app-move-hint").assertIsDisplayed()
        back(); assertBefore(0, 1)
        holdOk(); press(Key.DirectionRight)
        screen.runOnIdle { viewer = Viewer.ELIAS }
        screen.onNodeWithTag("app-move-hint").assertDoesNotExist()
        assertBefore(0, 1)
        holdOk(); press(Key.DirectionRight)
        screen.runOnIdle { apps = original.drop(1) }
        screen.onNodeWithTag("app-move-hint").assertDoesNotExist()
        tile(0).assertDoesNotExist()
        tile(1).assertIsFocused()
        assertEquals(1, writes)
    }

    @Test fun explicitSaveCompletesAfterHomeLeavesComposition() {
        val apps = (0..2).map(::app)
        var focused by mutableStateOf(apps.first().pkg)
        var showHome by mutableStateOf(true)
        val started = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()
        val committed = AtomicBoolean(false)
        screen.setContent { FamilyTheme {
            if (showHome) HomeScreen(Viewer.AMINE, null, apps, focused, 0,
                { focused = it }, {}, {}, {}, onSaveOrder = {
                    started.complete(Unit)
                    finishWrite.await()
                    committed.set(true)
                    true
                })
        } }
        holdOk(); press(Key.DirectionRight); press(Key.DirectionCenter)
        screen.waitUntil(5_000) { started.isCompleted }
        screen.runOnIdle { showHome = false }
        assertFalse(committed.get())
        finishWrite.complete(Unit)
        screen.waitUntil(5_000) { committed.get() }
    }

    @Test fun catalogFiltersBeforeSavedOrderAndAppendsNewAppsUsingExistingFallback() {
        val spotify = app(0).copy(pkg = "com.spotify.tv.android")
        val vrt = app(1).copy(pkg = "be.vrt.vrtnu")
        val extra = app(2)
        val missing = "com.example.missing"
        val catalog = listOf(extra, spotify, vrt, spotify)
        assertEquals(listOf(spotify.pkg, vrt.pkg, extra.pkg),
            orderedApps(catalog, Viewer.AMINE, emptySet(), listOf(missing, spotify.pkg, spotify.pkg)).map { it.pkg })
        assertEquals(listOf(vrt.pkg, extra.pkg),
            orderedApps(catalog, Viewer.ELIAS, setOf(vrt.pkg, extra.pkg), listOf(spotify.pkg, missing, vrt.pkg)).map { it.pkg })
        assertEquals(listOf(vrt.pkg, spotify.pkg, extra.pkg),
            orderedApps(catalog, Viewer.ELIAS, catalog.map { it.pkg }.toSet()).map { it.pkg })
    }
}
