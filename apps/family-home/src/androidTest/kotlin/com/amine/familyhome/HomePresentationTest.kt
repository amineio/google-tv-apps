package com.amine.familyhome

import android.content.ComponentName
import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomePresentationTest {
    @get:Rule val screen = createComposeRule()

    private fun app(pkg: String, label: String = pkg) = LauncherApp(pkg, label,
        ComponentName(pkg, "$pkg.Main"), Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888))

    @Test fun childHomeFiltersAppsAndSupportsVerticalAndFooterNavigation() {
        val catalog = DefaultApps.labels.map { app(it.key, it.value) } + app("com.google.android.youtube.tv", "YouTube")
        var launched = false
        screen.setContent {
            FamilyTheme {
                var focused by remember { mutableStateOf<String?>(null) }
                HomeScreen(Viewer.ELIAS, 1_200_000, orderedApps(catalog, Viewer.ELIAS, DefaultApps.labels.keys),
                    focused, 0, { focused = it }, { launched = true }, {}, {})
            }
        }
        screen.onNodeWithText("20:00 left").assertIsDisplayed()
        screen.onNodeWithTag("app:com.google.android.youtube.tv").assertDoesNotExist()
        for (pkg in DefaultApps.labels.keys) screen.onNodeWithTag("app:$pkg").assertIsDisplayed()
        screen.onNodeWithTag("app:com.amine.chessmath").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        screen.onNodeWithText("Open app").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        assertTrue(launched)
        screen.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        screen.onNodeWithTag("app:com.amine.chessmath").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        screen.onNodeWithText("Switch profile").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithText("End session").assertIsFocused()
    }

    @Test fun resumeRestoresAnAppBeyondTheFirstVisibleRow() {
        var catalog by mutableStateOf((0..9).map { app("example.app$it", if (it == 9) "An application with a long title that must fit within the TV screen" else "App $it") })
        var resume by mutableIntStateOf(0)
        screen.setContent {
            FamilyTheme {
                var focused by remember { mutableStateOf<String?>(null) }
                HomeScreen(Viewer.AMINE, null, catalog, focused, resume, { focused = it }, {}, {}, {})
            }
        }
        repeat(9) { screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }; screen.waitForIdle() }
        screen.onNodeWithTag("app:example.app9").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        screen.onNodeWithText("Open app").assertIsFocused()
        screen.runOnIdle { resume++ }
        screen.waitUntil(10_000) { screen.onAllNodes(isFocused() and hasTestTag("app:example.app9")).fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithTag("app:example.app9").assertIsDisplayed()
        screen.runOnIdle { catalog = catalog.take(1) }
        screen.waitUntil(10_000) { screen.onAllNodes(isFocused() and hasTestTag("app:example.app0")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun emptyCatalogKeepsProfileAndParentControlsUsable() {
        screen.setContent {
            FamilyTheme { HomeScreen(Viewer.AMINE, null, emptyList(), null, 0, {}, {}, {}, {}) }
        }
        screen.onNodeWithText("Switch profile").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithText("Family configuration").assertIsFocused()
        screen.onNodeWithText("No TV apps are installed.").assertIsDisplayed()
    }
}
