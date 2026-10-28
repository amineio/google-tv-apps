package com.amine.familyhome

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/** Generic presentation fixtures never use the application's saved family data. */
class UsagePresentationTest {
    @get:Rule val screen = createComposeRule()
    private val today = LocalDate.now()
    private val profiles = listOf(
        FamilyProfile(Viewer.AMINE, "Parent", Portrait.AMINE),
        FamilyProfile(Viewer.ELIAS, "Child One", Portrait.ELIAS),
        FamilyProfile(Viewer.SOFIA, "Child Two", Portrait.SOFIA),
        FamilyProfile(Viewer("child_example"), "Child Three"),
    )

    private fun show(usage: Map<String, Map<Viewer, Long>>, restore: StateRestorationTester? = null) {
        val content: @Composable () -> Unit = {
            FamilyTheme {
                FamilyBackdrop {
                    SettingsScreen(FamilyUi(state = FamilyState(profiles = profiles, usage = usage), ready = true),
                        emptyList(), true, true, 0, onClose = {}, onPin = {}, onAccess = { _, _, _ -> },
                        onAddChild = { null }, onSetup = {}, onHome = {})
                }
            }
        }
        if (restore == null) screen.setContent(content) else restore.setContent(content)
    }

    private fun key(key: Key) {
        screen.onRoot().performKeyInput { pressKey(key) }
        screen.waitForIdle()
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        screen.waitForIdle()
    }

    private fun openUsage() {
        screen.onNodeWithTag("setting:profiles").assertIsFocused()
        key(Key.DirectionDown)
        screen.onNodeWithTag("setting:usage").assertIsFocused()
        key(Key.DirectionCenter)
    }

    private fun capture(name: String) {
        val directory = requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null))
        directory.resolve("$name.png").outputStream().use {
            screen.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test fun todayShowsAllProfilesIncludingParentAndReadableTotals() {
        show(mapOf(today.toString() to mapOf(Viewer.AMINE to 4_500_000L,
            Viewer.ELIAS to 1_200_000L, Viewer.SOFIA to 30_000L),
            today.plusDays(1).toString() to mapOf(Viewer.AMINE to 300_000L)))
        openUsage()
        screen.onNodeWithTag("setting:usage:AMINE").assertIsFocused()
            .assertTextContains("Parent").assertTextContains("1 h 15 min")
        screen.onNodeWithTag("setting:usage:ELIAS").assertTextContains("20 min")
        screen.onNodeWithTag("setting:usage:SOFIA").assertTextContains("Less than 1 min")
        screen.onNodeWithTag("setting:usage:child_example").assertTextContains("No recorded use")
        screen.onNodeWithContentDescription("Parent profile portrait", useUnmergedTree = true).assertIsDisplayed()
        capture("usage-today")
        key(Key.DirectionCenter)
        screen.onNodeWithTag("setting:usage-date:$today").assertIsFocused()
            .assertTextContains("Today").assertTextContains("1 h 15 min")
        screen.onAllNodes(hasClickAction()).assertCountEquals(0)
        key(Key.DirectionCenter)
        screen.onNodeWithTag("setting:usage-date:$today").assertIsFocused()
        key(Key.DirectionDown)
        screen.onNodeWithTag("setting:usage-date:${today.plusDays(1)}").assertIsFocused().assertTextContains("5 min")
        back()
        screen.onNodeWithTag("setting:usage:AMINE").assertIsFocused()
        back()
        screen.onNodeWithTag("setting:usage").assertIsFocused()
    }

    @Test fun emptyHistoryKeepsTodayAndRemoteBackAvailable() {
        show(emptyMap())
        openUsage()
        key(Key.DirectionDown)
        screen.onNodeWithTag("setting:usage:ELIAS").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("setting:usage-date:$today").assertIsFocused()
            .assertTextContains("No recorded use")
        screen.onNodeWithText("No earlier recorded use.").assertIsDisplayed()
        screen.onAllNodes(hasClickAction()).assertCountEquals(0)
        capture("usage-empty")
        back()
        screen.onNodeWithTag("setting:usage:ELIAS").assertIsFocused()
    }

    @Test fun thirtyRecordedDatesScrollAndRestoreHistoryAndOverviewAfterRecreation() {
        val restore = StateRestorationTester(screen)
        val usage = (0L..29L).associate { offset ->
            today.minusDays(offset).toString() to mapOf(Viewer.ELIAS to 60_000L * (offset + 1))
        } + (today.minusDays(30).toString() to mapOf(Viewer.SOFIA to 60_000L))
        show(usage, restore)
        openUsage()
        key(Key.DirectionDown); key(Key.DirectionCenter)
        screen.onNodeWithTag("setting:usage-date:$today").assertIsFocused()
        capture("usage-history")
        repeat(29) { key(Key.DirectionDown) }
        val last = "setting:usage-date:${today.minusDays(29)}"
        screen.onNodeWithTag(last).assertIsFocused().assertIsDisplayed().assertTextContains("30 min")
        key(Key.DirectionDown)
        screen.onNodeWithTag(last).assertIsFocused()
        screen.onAllNodes(hasClickAction()).assertCountEquals(0)
        capture("usage-history-scrolled")
        restore.emulateSavedInstanceStateRestore()
        screen.onNodeWithTag(last).assertIsFocused().assertIsDisplayed()
        back()
        screen.onNodeWithTag("setting:usage:ELIAS").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithTag(last).assertIsFocused().assertIsDisplayed()
        back(); back()
        screen.onNodeWithTag("setting:usage").assertIsFocused()
        restore.emulateSavedInstanceStateRestore()
        screen.onNodeWithTag("setting:usage").assertIsFocused()
    }
}
