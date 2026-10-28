package com.amine.familyhome

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilyRemoteTest {
    @get:Rule val screen = createAndroidComposeRule<MainActivity>()

    @Before fun showProfiles() {
        screen.waitUntil(10_000) { screen.activity.family.ui.value.ready }
        runBlocking { screen.activity.family.chooseProfiles() }
    }

    @Test fun profileHeadingIsReadableOnTheDarkBackground() {
        screen.onNodeWithText("Who is watching?").assertIsDisplayed()
        val pixels = screen.onNodeWithText("Who is watching?").captureToImage().toPixelMap()
        var lightPixels = 0
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            if (pixels[x, y].luminance() > 0.7f) lightPixels++
        }
        org.junit.Assert.assertTrue("The profile heading needs light text on its dark background",
            lightPixels > pixels.width * pixels.height * 0.02)
    }

    @Test fun initialChooserShowsThreeProfiles() {
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Amine").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithText("Amine").assertIsDisplayed()
        screen.onNodeWithText("Elias").assertIsDisplayed()
        screen.onNodeWithText("Sofia").assertIsDisplayed()
        screen.onNodeWithText("Amine").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithText("Elias").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithText("Sofia").assertIsFocused()
    }

    @Test fun parentCanOpenFamilyConfigurationWithPinOff() {
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Amine").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithText("Amine").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Family configuration").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithText("Family configuration").performSemanticsAction(SemanticsActions.RequestFocus)
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        screen.onNodeWithText("Parent PIN: off").assertIsDisplayed()
    }
}
