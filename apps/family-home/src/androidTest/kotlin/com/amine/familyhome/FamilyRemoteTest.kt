package com.amine.familyhome

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilyRemoteTest {
    @get:Rule val screen = createAndroidComposeRule<MainActivity>()

    @Before fun showProfiles() {
        screen.waitUntil(10_000) { screen.activity.family.ui.value.ready }
        runBlocking { screen.activity.family.chooseProfiles() }
        screen.activity.family.helperConnected(true)
    }

    @After fun leaveProfilesWithoutATestPin() {
        runBlocking {
            screen.activity.family.select(Viewer.AMINE, "1111")
            screen.activity.family.setPin(null)
            screen.activity.family.chooseProfiles()
        }
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
        for (name in listOf("Amine", "Elias", "Sofia")) {
            screen.onNodeWithContentDescription("$name profile portrait", useUnmergedTree = true).assertIsDisplayed()
        }
        screen.onNodeWithText("Amine").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithText("Elias").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithText("Sofia").assertIsFocused()
    }

    @Test fun parentCanOpenFamilyConfigurationWithPinOff() {
        openParentConfiguration()
        screen.onNodeWithText("Parent PIN: off").assertIsDisplayed()
    }

    private fun openParentConfiguration() {
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Amine").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithText("Amine").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Family configuration").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithTag("home-apps").assertExists()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        screen.onNodeWithText("Switch profile").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithText("Family configuration").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
    }

    @Test fun parentPinKeypadSupportsRemoteApprovalAndFocusReturn() {
        openParentConfiguration()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        screen.onNodeWithText("1").assertIsFocused()
        screen.onNodeWithText("Continue").assertIsNotEnabled()
        repeat(4) { screen.onNodeWithText("1").performKeyInput { pressKey(Key.DirectionCenter) } }
        screen.onNodeWithText("Continue").assertIsEnabled()
        repeat(4) { screen.onNodeWithText("1").performKeyInput { pressKey(Key.DirectionDown) } }
        screen.onNodeWithText("Cancel").assertIsFocused()
        screen.onNodeWithText("1").performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithText("Continue").assertIsFocused()
        screen.onNodeWithText("Continue").performKeyInput { pressKey(Key.DirectionCenter) }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Parent PIN: on").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithText("Parent PIN: on").assertIsFocused()
    }

    @Test fun childDurationStartsAtTwentyAndStaysWithinFiveAndSixty() {
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionCenter) }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Time for Elias").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithTag("duration-value").assertTextEquals("20 minutes")
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        repeat(14) { screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) } }
        screen.onNodeWithTag("duration-value").assertTextEquals("60 minutes")
        screen.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        repeat(14) { screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) } }
        screen.onNodeWithTag("duration-value").assertTextEquals("5 minutes")
    }
}
