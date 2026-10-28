package com.amine.familyhome

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilyRemoteTest {
    @get:Rule val screen = createAndroidComposeRule<MainActivity>()

    private fun capture(name: String) {
        val directory = requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null))
        directory.resolve("$name.png").outputStream().use {
            screen.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Before fun showProfiles() {
        screen.waitUntil(10_000) { screen.activity.family.ui.value.ready }
        runBlocking {
            screen.activity.family.select(Viewer.AMINE, "1111")
            screen.activity.family.setPin(null)
            screen.activity.family.end(Viewer.ELIAS)
            screen.activity.family.end(Viewer.SOFIA)
            screen.activity.family.chooseProfiles()
        }
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
        screen.onNodeWithText("Cancel").assertDoesNotExist()
        screen.onNodeWithText("Continue").assertIsFocused()
        assertContinueIsCentered()
        screen.onNodeWithText("Continue").performKeyInput { pressKey(Key.DirectionCenter) }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Parent PIN: on").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithText("Parent PIN: on").assertIsFocused()
    }

    @Test fun durationKeepsHelperRecoveryNoticeVisibleAfterStartFails() {
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionCenter) }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Time for Elias").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithText("Start session").assertIsFocused()
        screen.activity.family.helperConnected(false)
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        screen.onNodeWithText("Enable the Family Home accessibility helper before starting a child session.")
            .assertIsDisplayed()
        screen.onNodeWithText("Start session").assertIsDisplayed().assertIsFocused()
        screen.onNodeWithTag("duration-value").assertTextEquals("20 minutes")
        capture("duration-notice")
    }

    @Test fun childDurationStartsAtTwentyAndStaysWithinFiveAndSixty() {
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionCenter) }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Time for Elias").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithTag("duration-value").assertTextEquals("20 minutes")
        screen.onNodeWithText("Start session").assertIsFocused()
        screen.onNodeWithText("Cancel").assertDoesNotExist()
        val startBounds = screen.onNodeWithText("Start session").fetchSemanticsNode().boundsInRoot
        val rootBounds = screen.onRoot().fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals("Start session must be horizontally centered", rootBounds.center.x,
            startBounds.center.x, with(screen.density) { 1.dp.toPx() })
        screen.onNodeWithText("Choose up to one hour.").assertDoesNotExist()
        val valueCenter = screen.onNodeWithTag("duration-value").fetchSemanticsNode().boundsInRoot.center.y
        // Allow normal font rounding while catching the conflicting-height regression.
        val alignmentTolerance = with(screen.density) { 1.dp.toPx() }
        for (label in listOf("− 5", "+ 5")) {
            val buttonCenter = screen.onNodeWithText(label).fetchSemanticsNode().boundsInRoot.center.y
            val textCenter = screen.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.center.y
            org.junit.Assert.assertEquals("$label label is not centered in its button", buttonCenter, textCenter, alignmentTolerance)
            org.junit.Assert.assertEquals("$label label is not aligned with the duration", valueCenter, textCenter, alignmentTolerance)
        }
        capture("duration-default")
        screen.onRoot().performKeyInput { pressKey(Key.DirectionUp) }
        screen.onNodeWithText("+ 5").assertIsFocused()
        repeat(14) { screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) } }
        screen.onNodeWithTag("duration-value").assertTextEquals("60 minutes")
        screen.onRoot().performKeyInput { pressKey(Key.DirectionLeft) }
        screen.onNodeWithText("− 5").assertIsFocused()
        repeat(14) { screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) } }
        screen.onNodeWithTag("duration-value").assertTextEquals("5 minutes")
        screen.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        screen.onNodeWithText("Start session").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNodeWithText("Start session").assertIsFocused()
        pressBack()
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Who is watching?").fetchSemanticsNodes().isNotEmpty() }
        screen.onNodeWithText("Amine").assertIsFocused()
        org.junit.Assert.assertNull(screen.activity.family.ui.value.state.selected)
        org.junit.Assert.assertFalse(screen.activity.family.ui.value.state.sessions.containsKey(Viewer.ELIAS))
    }

    private fun pressBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        screen.waitForIdle()
    }

    private fun assertContinueIsCentered() {
        screen.onAllNodesWithText("Continue").assertCountEquals(1)
        screen.onNodeWithText("Cancel").assertDoesNotExist()
        val dialog = screen.onNode(isDialog()).fetchSemanticsNode().boundsInRoot
        val button = screen.onNodeWithText("Continue").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals("Continue must be horizontally centered", dialog.center.x,
            button.center.x, with(screen.density) { 1.dp.toPx() })
    }

    private fun startChildHome(pinEnabled: Boolean = false) {
        runBlocking {
            if (pinEnabled) {
                org.junit.Assert.assertTrue(screen.activity.family.select(Viewer.AMINE))
                org.junit.Assert.assertTrue(screen.activity.family.setPin("1111"))
            }
            org.junit.Assert.assertTrue(screen.activity.family.start(Viewer.ELIAS, 20, "1111"))
        }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("End session").fetchSemanticsNodes().isNotEmpty() }
        screen.onRoot().performKeyInput { pressKey(Key.DirectionDown) }
        screen.onNodeWithText("Switch profile").assertIsFocused()
    }

    @Test fun switchQuestionNeedsOneContinueAndBackKeepsTheRunningSessionWithPinOn() {
        startChildHome(pinEnabled = true)
        val session = screen.activity.family.ui.value.state.sessions[Viewer.ELIAS]
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        screen.onNodeWithText("Switch profile?").assertIsDisplayed()
        assertContinueIsCentered()
        screen.onNodeWithText("Continue").assertIsFocused().assertIsEnabled()
        screen.onNode(hasText("1") and hasAnyAncestor(isDialog())).assertDoesNotExist()
        pressBack()
        screen.onNodeWithText("Switch profile?").assertDoesNotExist()
        org.junit.Assert.assertEquals(Viewer.ELIAS, screen.activity.family.ui.value.state.selected)
        org.junit.Assert.assertEquals(session, screen.activity.family.ui.value.state.sessions[Viewer.ELIAS])
        screen.onNodeWithText("Switch profile").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        screen.onNodeWithText("Continue").assertIsFocused()
        screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionCenter) }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Who is watching?").fetchSemanticsNodes().isNotEmpty() }
        val paused = requireNotNull(screen.activity.family.ui.value.state.sessions[Viewer.ELIAS])
        org.junit.Assert.assertNull(screen.activity.family.ui.value.state.selected)
        org.junit.Assert.assertNull(paused.deadlineElapsedMs)
        org.junit.Assert.assertTrue(paused.remainingMs in 1..1_200_000L)
    }

    @Test fun endQuestionNeedsOneContinueAndBackKeepsTheRunningSession() {
        startChildHome()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionCenter) }
        screen.onNodeWithText("End Elias's session?").assertIsDisplayed()
        assertContinueIsCentered()
        screen.onNodeWithText("Continue").assertIsFocused()
        val session = screen.activity.family.ui.value.state.sessions[Viewer.ELIAS]
        pressBack()
        org.junit.Assert.assertEquals(Viewer.ELIAS, screen.activity.family.ui.value.state.selected)
        org.junit.Assert.assertEquals(session, screen.activity.family.ui.value.state.sessions[Viewer.ELIAS])
        screen.onNodeWithText("End session").assertIsFocused()
        screen.onRoot().performKeyInput { pressKey(Key.DirectionCenter) }
        screen.onNodeWithText("Continue").assertIsFocused()
        screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionCenter) }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Who is watching?").fetchSemanticsNodes().isNotEmpty() }
        org.junit.Assert.assertNull(screen.activity.family.ui.value.state.selected)
        org.junit.Assert.assertFalse(screen.activity.family.ui.value.state.sessions.containsKey(Viewer.ELIAS))
    }

    @Test fun endPinRejectsWrongDigitsThenAcceptsCorrectDigitsWithOneContinue() {
        startChildHome(pinEnabled = true)
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionCenter) }
        screen.onNode(hasText("1") and hasAnyAncestor(isDialog())).assertIsFocused()
        screen.onNodeWithText("Continue").assertIsNotEnabled()
        screen.onNodeWithText("Cancel").assertDoesNotExist()
        screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionRight) }
        screen.onNode(hasText("2") and hasAnyAncestor(isDialog())).assertIsFocused()
        repeat(4) { screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionCenter) } }
        repeat(4) { screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionDown) } }
        screen.onNodeWithText("Continue").assertIsFocused()
        screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionCenter) }
        val incorrectPin = hasText("The parent PIN is incorrect.") and hasAnyAncestor(isDialog())
        screen.waitUntil(10_000) { screen.onAllNodes(incorrectPin).fetchSemanticsNodes().isNotEmpty() }
        screen.onNode(incorrectPin).assertIsDisplayed()
        screen.onNodeWithText("End Elias's session?").assertIsDisplayed()
        org.junit.Assert.assertEquals(Viewer.ELIAS, screen.activity.family.ui.value.state.selected)
        org.junit.Assert.assertTrue(screen.activity.family.ui.value.state.sessions.containsKey(Viewer.ELIAS))
        screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionUp); pressKey(Key.DirectionLeft) }
        screen.onNodeWithText("Clear").assertIsFocused()
        screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionCenter) }
        screen.onNodeWithText("Continue").assertIsNotEnabled()
        repeat(3) { screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionUp) } }
        screen.onNode(hasText("1") and hasAnyAncestor(isDialog())).assertIsFocused()
        repeat(4) { screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionCenter) } }
        repeat(4) { screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionDown) } }
        screen.onNodeWithText("Continue").assertIsFocused()
        screen.onNode(isDialog()).performKeyInput { pressKey(Key.DirectionCenter) }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Who is watching?").fetchSemanticsNodes().isNotEmpty() }
        org.junit.Assert.assertFalse(screen.activity.family.ui.value.state.sessions.containsKey(Viewer.ELIAS))
    }

    @Test fun backFromStartPinKeepsDurationThenBackReturnsToProfilesWithoutStarting() {
        runBlocking {
            org.junit.Assert.assertTrue(screen.activity.family.select(Viewer.AMINE))
            org.junit.Assert.assertTrue(screen.activity.family.setPin("1111"))
            org.junit.Assert.assertTrue(screen.activity.family.chooseProfiles())
        }
        screen.onRoot().performKeyInput { pressKey(Key.DirectionRight); pressKey(Key.DirectionCenter) }
        screen.waitUntil(10_000) { screen.onAllNodesWithText("Time for Elias").fetchSemanticsNodes().isNotEmpty() }
        screen.onRoot().performKeyInput { pressKey(Key.DirectionUp); pressKey(Key.DirectionCenter); pressKey(Key.DirectionDown); pressKey(Key.DirectionCenter) }
        screen.onNodeWithText("Approve 25 minutes for Elias.").assertIsDisplayed()
        screen.onNode(hasText("1") and hasAnyAncestor(isDialog())).assertIsFocused()
        screen.onNodeWithText("Continue").assertIsNotEnabled()
        screen.onNodeWithText("Cancel").assertDoesNotExist()
        pressBack()
        screen.onNodeWithTag("duration-value").assertTextEquals("25 minutes")
        screen.onNodeWithText("Start session").assertIsFocused()
        org.junit.Assert.assertFalse(screen.activity.family.ui.value.state.sessions.containsKey(Viewer.ELIAS))
        pressBack()
        screen.onNodeWithText("Who is watching?").assertIsDisplayed()
        org.junit.Assert.assertNull(screen.activity.family.ui.value.state.selected)
    }
}
