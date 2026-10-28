package com.amine.familyhome

import android.content.ComponentName
import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProfileMenuTest {
    @get:Rule val screen = createComposeRule()

    private fun app(index: Int) = LauncherApp("example.app$index", "App $index",
        ComponentName("example.app$index", "example.Main"),
        Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888))

    private fun key(key: Key) {
        screen.waitForIdle()
        val target = if (screen.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty()) {
            screen.onNode(isDialog())
        } else screen.onRoot()
        target.performKeyInput { pressKey(key) }
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        screen.waitForIdle()
    }

    @Test fun everyAppReachesProfileAndDownReturnsToTheSelectedApp() {
        screen.setContent {
            FamilyTheme {
                var focused by remember { mutableStateOf<String?>(null) }
                HomeScreen(Viewer.AMINE, null, remember { (0..2).map(::app) }, focused, 0,
                    { focused = it }, {}, {}, {})
            }
        }
        screen.onNodeWithTag("app:example.app0").assertIsFocused()
        screen.onNodeWithText("Switch profile").assertDoesNotExist()
        screen.onNodeWithText("Family configuration").assertDoesNotExist()
        for (index in 0..2) {
            screen.onNodeWithTag("app:example.app$index").assertIsFocused()
            key(Key.DirectionUp)
            screen.onNodeWithTag("home-profile").assertIsFocused()
            key(Key.DirectionUp); key(Key.DirectionRight); key(Key.DirectionLeft)
            screen.onNodeWithTag("home-profile").assertIsFocused()
            key(Key.DirectionDown)
            screen.onNodeWithTag("app:example.app$index").assertIsFocused()
            key(Key.DirectionDown)
            screen.onNodeWithTag("app:example.app$index").assertIsFocused()
            if (index < 2) key(Key.DirectionRight)
        }
        key(Key.DirectionRight)
        screen.onNodeWithTag("app:example.app2").assertIsFocused()
    }

    @Test fun menuBackRestoresProfileAndActionsCloseBeforeCallingTheExistingCallbacks() {
        var switches = 0
        var manages = 0
        screen.setContent {
            FamilyTheme { HomeScreen(Viewer.AMINE, null, listOf(app(0)), null, 0,
                {}, {}, { switches++ }, { manages++ }) }
        }
        key(Key.DirectionUp); key(Key.DirectionCenter)
        screen.onNodeWithText("Switch profile").assertIsFocused()
        key(Key.DirectionUp); key(Key.DirectionLeft); key(Key.DirectionRight)
        screen.onNodeWithText("Switch profile").assertIsFocused()
        back()
        screen.onNodeWithTag("home-profile").assertIsFocused()
        screen.onNodeWithText("Switch profile").assertDoesNotExist()
        key(Key.DirectionCenter); key(Key.DirectionDown)
        screen.onNodeWithText("Family configuration").assertIsFocused()
        key(Key.DirectionDown)
        screen.onNodeWithText("Family configuration").assertIsFocused()
        key(Key.DirectionCenter)
        assertEquals(1, manages)
        screen.onNodeWithText("Family configuration").assertDoesNotExist()
        screen.onNodeWithTag("home-profile").assertIsFocused()
        key(Key.DirectionCenter); key(Key.DirectionCenter)
        assertEquals(1, switches)
        screen.onNodeWithText("Switch profile").assertDoesNotExist()
    }

    @Test fun emptyChildCatalogStartsOnProfileAndKeepsBothActionsReachable() {
        var ends = 0
        screen.setContent {
            FamilyTheme { HomeScreen(Viewer.ELIAS, 1_200_000, emptyList(), null, 0,
                {}, {}, {}, { ends++ }) }
        }
        screen.onNodeWithTag("home-profile").assertIsFocused()
        key(Key.DirectionDown); key(Key.DirectionLeft); key(Key.DirectionRight)
        screen.onNodeWithTag("home-profile").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithText("Switch profile").assertIsFocused()
        key(Key.DirectionDown)
        screen.onNodeWithText("End session").assertIsFocused()
        screen.onNodeWithText("Family configuration").assertDoesNotExist()
        key(Key.DirectionCenter)
        assertEquals(1, ends)
        screen.onNodeWithTag("home-profile").assertIsFocused()
    }

    @Test fun returningFromSettingsFocusesTheProfileControl() {
        screen.setContent {
            FamilyTheme { HomeScreen(Viewer.AMINE, null, listOf(app(0)), null, 0,
                {}, {}, {}, {}, focusManageOnEntry = true) }
        }
        screen.onNodeWithTag("home-profile").assertIsFocused()
        key(Key.DirectionDown)
        screen.onNodeWithTag("app:example.app0").assertIsFocused()
    }
}
