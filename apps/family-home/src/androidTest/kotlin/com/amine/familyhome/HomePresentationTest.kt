package com.amine.familyhome

import android.content.ComponentName
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomePresentationTest {
    @get:Rule val screen = createComposeRule()

    private fun app(pkg: String, label: String = pkg) = LauncherApp(pkg, label,
        ComponentName(pkg, "$pkg.Main"), Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(35, 80, 105))
        })

    private fun capture(name: String) {
        val directory = requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null))
        directory.resolve("$name.png").outputStream().use {
            screen.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun fixedSizeLayerArtworkScalesToFillMatchingAspectRatio() {
        val color = Color.rgb(35, 80, 105)
        val artwork = LayerDrawable(arrayOf(ColorDrawable(color))).apply { setLayerSize(0, 160, 90) }
        val image = artwork.render(384, 216)
        assertEquals(384, image.width)
        assertEquals(216, image.height)
        for ((x, y) in listOf(0 to 0, 383 to 0, 0 to 215, 383 to 215)) {
            assertEquals("Artwork must fill corner ($x, $y)", color, image.getPixel(x, y))
        }
    }

    @Test fun fixedSizeLayerArtworkFitsWhenAspectRatioDiffers() {
        val color = Color.rgb(35, 80, 105)
        val artwork = LayerDrawable(arrayOf(ColorDrawable(color))).apply { setLayerSize(0, 160, 90) }
        val image = artwork.render(128, 128)
        assertEquals(128, image.width)
        assertEquals(128, image.height)
        for ((x, y) in listOf(0 to 28, 127 to 28, 0 to 99, 127 to 99)) {
            assertEquals("Fitted artwork must cover ($x, $y)", color, image.getPixel(x, y))
        }
        for (y in listOf(0, 27, 100, 127)) {
            assertEquals("Aspect fit must keep row $y transparent", Color.TRANSPARENT, image.getPixel(64, y))
        }
    }

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
        screen.onNodeWithText("left", substring = true).assertIsDisplayed()
        screen.onNodeWithTag("app:com.google.android.youtube.tv").assertDoesNotExist()
        for (pkg in DefaultApps.labels.keys) screen.onNodeWithTag("app:$pkg").assertIsDisplayed()
        screen.onNodeWithTag("app:com.amine.chessmath").assertIsFocused()
        capture("home-chess")
        screen.onNodeWithText("Open app").assertDoesNotExist()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        screen.onNodeWithTag("app:com.amine.chessmath").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        assertTrue(launched)
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
        screen.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        screen.onNodeWithText("Switch profile").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        screen.onNodeWithTag("app:example.app9").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        screen.onNodeWithText("Switch profile").assertIsFocused()
        screen.runOnIdle { resume++ }
        screen.waitUntil(10_000) { screen.onAllNodes(isFocused() and hasTestTag("app:example.app9")).fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithTag("app:example.app9").assertIsDisplayed()
        screen.runOnIdle { catalog = catalog.take(1) }
        screen.waitUntil(10_000) { screen.onAllNodes(isFocused() and hasTestTag("app:example.app0")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun bannerAndSquareTilesLaunchDirectly() {
        val banner = Bitmap.createBitmap(384, 216, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.rgb(65, 45, 100))
        }
        val bannerApp = app("example.cinema", "Cinema").copy(banner = banner)
        val squareApp = app("example.music", "Music")
        var launched: String? = null
        screen.setContent {
            FamilyTheme {
                var focused by remember { mutableStateOf<String?>(null) }
                HomeScreen(Viewer.AMINE, null, listOf(bannerApp, squareApp), focused, 0,
                    { focused = it }, { launched = it.pkg }, {}, {})
            }
        }
        screen.onNodeWithTag("app:example.cinema").assertIsFocused()
        capture("home-banner")
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals("example.cinema", launched)
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithTag("app:example.music").assertIsFocused()
        capture("home-square")
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals("example.music", launched)
    }

    @Test fun countdownChangesKeepChipSuffixAndProfileBoundsStable() {
        var remaining by mutableLongStateOf(1_200_000L)
        screen.setContent {
            FamilyTheme {
                HomeScreen(Viewer.ELIAS, remaining, listOf(app("example.tv")), null, 0, {}, {}, {}, {})
            }
        }
        val suffix = screen.onNodeWithText("left", substring = true, useUnmergedTree = true)
        val profile = screen.onNodeWithContentDescription("Elias profile portrait", useUnmergedTree = true)
        val name = screen.onNodeWithText("Elias")
        val suffixBounds = suffix.fetchSemanticsNode().boundsInRoot
        val chipBounds = suffix.onParent().fetchSemanticsNode().boundsInRoot
        val profileBounds = profile.fetchSemanticsNode().boundsInRoot
        val nameBounds = name.fetchSemanticsNode().boundsInRoot
        for (value in listOf(1_199_000L, 600_000L, 599_000L, 60_000L, 59_000L, 1_000L, 0L)) {
            screen.runOnIdle { remaining = value }
            assertEquals("Countdown suffix moved at $value ms", suffixBounds, suffix.fetchSemanticsNode().boundsInRoot)
            assertEquals("Countdown border moved at $value ms", chipBounds, suffix.onParent().fetchSemanticsNode().boundsInRoot)
            assertEquals("Profile portrait moved at $value ms", profileBounds, profile.fetchSemanticsNode().boundsInRoot)
            assertEquals("Profile name moved at $value ms", nameBounds, name.fetchSemanticsNode().boundsInRoot)
        }
    }

    @Test fun emptyCatalogKeepsParentAndChildControlsUsable() {
        var viewer by mutableStateOf(Viewer.AMINE)
        screen.setContent {
            FamilyTheme { HomeScreen(viewer, null, emptyList(), null, 0, {}, {}, {}, {}) }
        }
        screen.onNodeWithText("Switch profile").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithText("Family configuration").assertIsFocused()
        screen.onNodeWithText("No TV apps are installed.").assertIsDisplayed()
        capture("home-empty")
        screen.runOnIdle { viewer = Viewer.ELIAS }
        screen.onNodeWithText("Switch profile").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithText("End session").assertIsFocused()
        screen.onAllNodesWithText("Your apps").assertCountEquals(1)
        screen.onNodeWithText("Your time, your choice").assertDoesNotExist()
        screen.onNodeWithText("No allowed apps are installed. Ask Amine to review the app list.").assertIsDisplayed()
    }
}
