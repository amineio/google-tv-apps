package com.amine.familyhome

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileChooserPresentationTest {
    @get:Rule val screen = createComposeRule()

    private val parent = Viewer.AMINE
    private val child = Viewer.ELIAS
    private val other = Viewer.SOFIA
    private fun profiles() = listOf(
        FamilyProfile(parent, "Parent", Portrait.CHILD),
        FamilyProfile(child, "Child One", Portrait.CHILD),
        FamilyProfile(other, "Child Two", Portrait.CHILD),
    )

    private fun show(state: FamilyState, helper: Boolean = true, notice: String? = null, now: Long = 0) {
        screen.setContent {
            FamilyTheme { FamilyBackdrop {
                ProfileScreen(FamilyUi(state = state, ready = true, helper = helper, notice = notice, now = now)) {}
            } }
        }
    }

    private fun press(key: Key) {
        screen.onRoot().performKeyInput { pressKey(key) }
        screen.waitForIdle()
    }

    private fun assertFocused(viewer: Viewer) {
        screen.waitUntil(5_000) {
            screen.onAllNodes(hasTestTag("profile:${viewer.id}") and isFocused()).fetchSemanticsNodes().isNotEmpty()
        }
        screen.onNodeWithTag("profile:${viewer.id}").assertIsFocused()
    }

    private fun capture(name: String) {
        val directory = requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null))
        directory.resolve("$name.png").outputStream().use {
            screen.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun positiveChildBalanceShowsOneMergedResumeStateEvenWithoutRecovery() {
        show(FamilyState(profiles = profiles(), sessions = mapOf(child to ChildSession(1_200_000, 679_001))))
        screen.onNodeWithTag("profile-chooser").assertIsDisplayed()
        assertFocused(parent)
        screen.onNodeWithTag("resume:ELIAS", useUnmergedTree = true).assertIsDisplayed()
            .assert(hasClickAction().not()).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
        screen.onNodeWithTag("profile:ELIAS").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Resume, 11:20 remaining"))
        screen.onAllNodes(hasStateDescription("Resume, 11:20 remaining")).assertCountEquals(1)
        press(Key.DirectionRight)
        assertFocused(child)
        press(Key.DirectionRight)
        assertFocused(other)
        capture("profile-chooser-resume-generic")
    }

    @Test fun parentExpiredAndAbsentSessionsHaveNoBadge() {
        // The parent fixture deliberately has a session to verify the presentation guard.
        val absent = Viewer("absent-fixture")
        show(FamilyState(profiles = profiles() + FamilyProfile(absent, "Child Three"), sessions = mapOf(
            parent to ChildSession(1_200_000, 600_000),
            child to ChildSession(1_200_000, 0),
            other to ChildSession(1_200_000, 600_000, deadlineElapsedMs = 500),
        )), now = 500)
        for (viewer in listOf(parent, child, other, absent)) {
            screen.onNodeWithTag("resume:${viewer.id}", useUnmergedTree = true).assertDoesNotExist()
        }
        screen.onNodeWithTag("profile:ELIAS").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test fun recoveryKeepsResumeBadgeAndHelperAndStorageNotices() {
        val recovered = SessionEngine.suspend(FamilyState(profiles = profiles(), selected = child,
            sessions = mapOf(child to ChildSession(1_200_000, 600_000, deadlineElapsedMs = 900_000))), 300_000).copy(recovered = true)
        show(recovered, helper = false, notice = "Saved state needs attention")
        screen.onNodeWithTag("resume:ELIAS", useUnmergedTree = true).assertIsDisplayed()
        screen.onNodeWithText("Unfinished sessions are paused. Each child keeps the last saved balance.").assertDoesNotExist()
        screen.onNodeWithText("Who is watching?").assertDoesNotExist()
        screen.onNodeWithText("Choose your profile to begin").assertDoesNotExist()
        screen.onNodeWithText("Parent setup: open Amine to enable the Home helper.").assertIsDisplayed()
        screen.onNodeWithText("Saved state needs attention").assertIsDisplayed()
        screen.onNodeWithText("Select to continue").assertIsDisplayed()
        capture("profile-chooser-notices-generic")
    }

    @Test fun dialogReturnKeepsTheLastScrolledProfileWithoutRequestingTheDisposedParent() {
        val extra = (1..5).map { FamilyProfile(Viewer("fixture-$it"), "Child profile $it", Portrait.CHILD) }
        val state = FamilyState(profiles = profiles() + extra)
        var dialogOpen by mutableStateOf(false)
        var chooserWindowFocused by mutableStateOf(false)
        screen.setContent {
            val focused = LocalWindowInfo.current.isWindowFocused
            SideEffect { chooserWindowFocused = focused }
            FamilyTheme {
                FamilyBackdrop {
                    ProfileScreen(FamilyUi(state = state, ready = true, helper = true)) { dialogOpen = true }
                }
                if (dialogOpen) Dialog(onDismissRequest = { dialogOpen = false }) {
                    Text("Return to profiles with Back")
                }
            }
        }
        assertFocused(parent)
        for (viewer in listOf(child, other) + extra.map { it.viewer }) {
            press(Key.DirectionRight)
            assertFocused(viewer)
        }
        val last = extra.last().viewer
        screen.onNodeWithTag("profile:AMINE").assertDoesNotExist()
        press(Key.DirectionCenter)
        screen.onNode(isDialog()).assertExists()
        screen.waitUntil(5_000) { !chooserWindowFocused }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        screen.waitUntil(5_000) { !dialogOpen && chooserWindowFocused }
        screen.onNode(isDialog()).assertDoesNotExist()
        assertFocused(last)
        screen.onNodeWithTag("profile:AMINE").assertDoesNotExist()
    }

    @Test fun longNamesAndExtraProfilesScrollWithAlignedPortraitsAndUnclippedBadges() {
        val extra = (1..5).map { FamilyProfile(Viewer("fixture-$it"), "Long child profile name $it", Portrait.CHILD) }
        val state = FamilyState(profiles = profiles() + extra,
            sessions = (listOf(child) + extra.map { it.viewer }).associateWith { ChildSession(3_600_000, 3_600_000) })
        show(state)
        assertFocused(parent)
        val root = screen.onRoot().fetchSemanticsNode().boundsInRoot
        val parentBounds = screen.onNodeWithTag("profile:AMINE").fetchSemanticsNode().boundsInRoot
        press(Key.DirectionRight)
        assertFocused(child)
        val childBounds = screen.onNodeWithTag("profile:ELIAS").fetchSemanticsNode().boundsInRoot
        assertEquals(parentBounds.center.y, childBounds.center.y, with(screen.density) { 2.dp.toPx() })
        assertEquals(root.center.y, childBounds.center.y, with(screen.density) { 18.dp.toPx() })
        for (viewer in listOf(other) + extra.map { it.viewer }) {
            press(Key.DirectionRight)
            assertFocused(viewer)
        }
        val last = extra.last().viewer
        screen.onNodeWithTag("profile:${last.id}").assertIsDisplayed()
        val card = screen.onNodeWithTag("profile:${last.id}").fetchSemanticsNode().boundsInRoot
        val badge = screen.onNodeWithTag("resume:${last.id}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        capture("profile-chooser-scroll-generic")
        // Surface reports its layout bounds; descendants include the focused graphics scale.
        val visualCard = androidx.compose.ui.geometry.Rect(
            card.center.x - card.width * 1.07f / 2, card.center.y - card.height * 1.07f / 2,
            card.center.x + card.width * 1.07f / 2, card.center.y + card.height * 1.07f / 2)
        assertTrue("Badge must stay inside the focused portrait card: badge=$badge, visualCard=$visualCard",
            badge.left >= visualCard.left && badge.right <= visualCard.right &&
                badge.top >= visualCard.top && badge.bottom <= visualCard.bottom)
        assertTrue("Focus outline must stay inside the screen: visualCard=$visualCard, root=$root",
            visualCard.left > root.left && visualCard.right < root.right &&
                visualCard.top > root.top && visualCard.bottom < root.bottom)
    }
}
