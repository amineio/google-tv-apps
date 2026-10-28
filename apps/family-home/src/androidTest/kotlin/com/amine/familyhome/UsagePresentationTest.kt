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

    @Test fun todayOverviewAndChartUseDirectionalSelectionAndDistinctValues() {
        val monday = weekStart(today)
        show(mapOf(today.toString() to mapOf(Viewer.AMINE to 4_500_000L,
            Viewer.ELIAS to 1_200_000L, Viewer.SOFIA to 30_000L),
            monday.minusWeeks(1).toString() to mapOf(Viewer.AMINE to 0L)))
        openUsage()
        screen.onNodeWithTag("setting:usage:AMINE").assertIsFocused()
            .assertTextContains("Parent").assertTextContains("1 h 15 min")
        screen.onNodeWithTag("setting:usage:ELIAS").assertTextContains("20 min")
        screen.onNodeWithTag("setting:usage:SOFIA").assertTextContains("Less than 1 min")
        screen.onNodeWithTag("setting:usage:child_example").assertTextContains("No recorded use")
        capture("usage-today")
        key(Key.DirectionCenter)
        screen.onNodeWithTag("usage-day:$today").assertIsFocused()
        screen.onNodeWithTag("usage-week-total").assertTextContains("1 h 15 min")
        screen.onNodeWithTag("usage-selected-duration").assertTextContains("1 h 15 min")
        screen.onNodeWithTag("usage-day:$today").assert(hasClickAction().not())
        key(Key.DirectionCenter)
        screen.onNodeWithTag("usage-day:$today").assertIsFocused()
        if (today.dayOfWeek.value < 7) {
            key(Key.DirectionRight)
            screen.onNodeWithTag("usage-day:${today.plusDays(1)}").assertIsFocused()
            screen.onNodeWithTag("usage-selected-duration").assertTextContains("Future date")
            key(Key.DirectionLeft)
        } else {
            key(Key.DirectionLeft)
            screen.onNodeWithTag("usage-selected-duration").assertTextContains("No recorded use")
            key(Key.DirectionRight)
        }
        capture("usage-history")
        back()
        screen.onNodeWithTag("setting:usage:AMINE").assertIsFocused()
        back()
        screen.onNodeWithTag("setting:usage").assertIsFocused()
    }

    @Test fun emptyHistoryKeepsTodayFocusAndBothWeekControlsDisabled() {
        show(emptyMap())
        openUsage()
        key(Key.DirectionDown); key(Key.DirectionCenter)
        screen.onNodeWithTag("usage-day:$today").assertIsFocused()
        screen.onNodeWithTag("usage-selected-duration").assertTextContains("No recorded use")
        screen.onNodeWithTag("usage-week-total").assertTextContains("No recorded use")
        screen.onNodeWithTag("usage-scale").assertDoesNotExist()
        screen.onNodeWithTag("usage-earlier").assertIsNotEnabled()
        screen.onNodeWithTag("usage-later").assertIsNotEnabled()
        key(Key.DirectionUp)
        screen.onNodeWithTag("usage-day:$today").assertIsFocused()
        capture("usage-empty")
        back()
        screen.onNodeWithTag("setting:usage:ELIAS").assertIsFocused()
    }

    @Test fun weekPagingPreservesWeekdayControlFocusAndBackRecreationState() {
        val restore = StateRestorationTester(screen)
        val previous = today.minusWeeks(1)
        show(mapOf(previous.toString() to mapOf(Viewer.ELIAS to 0L),
            today.plusWeeks(1).toString() to mapOf(Viewer.ELIAS to 60_000L)), restore)
        openUsage()
        key(Key.DirectionDown); key(Key.DirectionCenter)
        screen.onNodeWithTag("usage-day:$today").assertIsFocused()
        key(Key.DirectionUp)
        screen.onNodeWithTag("usage-earlier").assertIsFocused()
        key(Key.DirectionCenter)
        // Earlier is now disabled at the oldest retained week, so focus moves to Later.
        screen.onNodeWithTag("usage-earlier").assertIsNotEnabled()
        screen.onNodeWithTag("usage-later").assertIsFocused()
        key(Key.DirectionDown)
        screen.onNodeWithTag("usage-day:$previous").assertIsFocused()
        screen.onNodeWithTag("usage-selected-duration").assertTextContains("0 min")
        screen.onNodeWithTag("usage-week-total").assertTextContains("0 min")
        capture("usage-zero")
        restore.emulateSavedInstanceStateRestore()
        screen.onNodeWithTag("usage-day:$previous").assertIsFocused()
        back()
        screen.onNodeWithTag("setting:usage:ELIAS").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("usage-day:$previous").assertIsFocused()
        key(Key.DirectionUp)
        screen.onNodeWithTag("usage-later").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("usage-later").assertIsFocused()
        restore.emulateSavedInstanceStateRestore()
        screen.onNodeWithTag("usage-later").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("usage-later").assertIsNotEnabled()
        screen.onNodeWithTag("usage-earlier").assertIsFocused()
        key(Key.DirectionDown)
        screen.onNodeWithTag("usage-day:${today.plusWeeks(1)}").assertIsFocused()
        screen.onNodeWithTag("usage-selected-duration").assertTextContains("1 min")
        capture("usage-future-record")
    }

    @Test fun fullRecordedWeekHasSharedScaleAndDirectionalDayDetails() {
        val monday = weekStart(today).minusWeeks(1)
        val minutes = listOf(15L, 45L, 30L, 60L, 20L, 10L, 35L)
        show(minutes.mapIndexed { index, value ->
            monday.plusDays(index.toLong()).toString() to mapOf(Viewer.ELIAS to value * 60_000L)
        }.toMap())
        openUsage()
        key(Key.DirectionDown); key(Key.DirectionCenter)
        screen.onNodeWithTag("usage-day:$today").assertIsFocused()
        key(Key.DirectionUp)
        screen.onNodeWithTag("usage-earlier").assertIsFocused()
        key(Key.DirectionCenter); key(Key.DirectionDown)
        val selected = today.dayOfWeek.value - 1
        screen.onNodeWithTag("usage-day:${monday.plusDays(selected.toLong())}").assertIsFocused()
        screen.onNodeWithTag("usage-week-total").assertTextContains("3 h 35 min")
        screen.onNodeWithTag("usage-scale").assertTextContains("Max day 1 h")
        screen.onNodeWithTag("usage-selected-duration").assertTextContains(usageTotal(minutes[selected] * 60_000L))
        val adjacent = if (selected < 6) selected + 1 else selected - 1
        key(if (selected < 6) Key.DirectionRight else Key.DirectionLeft)
        screen.onNodeWithTag("usage-day:${monday.plusDays(adjacent.toLong())}").assertIsFocused()
        screen.onNodeWithTag("usage-selected-duration").assertTextContains(usageTotal(minutes[adjacent] * 60_000L))
        screen.onNodeWithTag("usage-week-total").assertTextContains("3 h 35 min")
        screen.onNodeWithTag("usage-scale").assertTextContains("Max day 1 h")
        key(if (selected < 6) Key.DirectionLeft else Key.DirectionRight)
        screen.onNodeWithTag("usage-day:${monday.plusDays(selected.toLong())}").assertIsFocused()
        capture("usage-full-week")
    }

}
