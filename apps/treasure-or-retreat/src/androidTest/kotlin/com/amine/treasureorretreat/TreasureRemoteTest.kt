package com.amine.treasureorretreat

import android.view.KeyEvent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

class TreasureRemoteTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val model get() = ViewModelProvider(rule.activity)[TreasureModel::class.java]
    private fun press(code: Int) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(code)
        rule.waitForIdle()
    }
    private fun start() { press(KeyEvent.KEYCODE_DPAD_CENTER); rule.onNodeWithTag("explore").assertIsSelected() }
    private fun settle() { rule.waitUntil(4000) { model.game.phase != Phase.REVEALING } }

    @Test fun directionsBankThreeExpeditionsAndRestart() {
        start()
        repeat(3) { index ->
            assertEquals(index + 1, model.game.expedition)
            press(KeyEvent.KEYCODE_DPAD_RIGHT)
            rule.onNodeWithTag("bank").assertIsSelected()
            val expected = model.game.banked + model.game.unbanked
            press(KeyEvent.KEYCODE_DPAD_CENTER)
            settle()
            assertEquals(expected, model.game.banked)
            rule.onNodeWithTag("next").assertExists()
            press(KeyEvent.KEYCODE_DPAD_CENTER)
        }
        assertEquals(Phase.FINISHED, model.game.phase)
        rule.onNodeWithTag("finalScore").assertExists()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals(1, model.game.expedition)
        assertEquals(0, model.game.banked)
        rule.onNodeWithTag("explore").assertIsSelected()
    }

    @Test fun heldSelectCannotDrawAgainAcrossRevealCompletion() {
        start()
        rule.runOnIdle { model.game = GameRules.newGame(Random(2)).copy(deck = listOf(Card.GOLD, Card.RUBY, Card.TRAP)) }
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.sendKeySync(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER))
        rule.waitUntil(4000) { model.game.phase == Phase.READY }
        val afterFirst = model.game
        instrumentation.sendKeySync(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 1))
        instrumentation.sendKeySync(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER))
        rule.waitForIdle()
        assertEquals(afterFirst, model.game)
        assertEquals(2, model.game.deck.size)
    }

    @Test fun trapOnlyLosesUnbankedAndPausePreservesState() {
        start()
        rule.runOnIdle { model.game = GameRules.newGame(Random(2)).copy(deck = listOf(Card.TRAP), unbanked = 300, banked = 500) }
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        press(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("pause0").assertIsSelected()
        val pausedState = model.game
        Thread.sleep(1200)
        assertEquals(pausedState, model.game)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        settle()
        assertEquals(Outcome.TRAPPED, model.game.outcome)
        assertEquals(500, model.game.banked)
        assertEquals(0, model.game.unbanked)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("explore").assertIsSelected()
    }

    @Test fun recreationKeepsGameAndReturnsThroughPause() {
        start()
        val before = model.game
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
        assertEquals(before, model.game)
        rule.onNodeWithTag("pause0").assertIsSelected()
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        rule.onNodeWithTag("explore").assertIsSelected()
    }

    @Test fun recordsAndSoundPersistWithoutLoweringRecord() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test_records_" + System.nanoTime()
        try {
            val first = LocalPreferences(context, name)
            first.sound = false
            assertEquals(750, first.record(750))
            assertEquals(750, first.record(10))
            val reopened = LocalPreferences(context, name)
            assertFalse(reopened.sound)
            assertEquals(750, reopened.best)
        } finally { context.deleteSharedPreferences(name) }
    }

    @Test fun pauseDirectionsReachSoundAndRestoreGameplaySelection() {
        start()
        press(KeyEvent.KEYCODE_DPAD_RIGHT)
        press(KeyEvent.KEYCODE_BACK)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        press(KeyEvent.KEYCODE_DPAD_DOWN)
        rule.onNodeWithTag("pause2").assertIsSelected()
        val previous = model.sound
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals(!previous, model.sound)
        press(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals(previous, model.sound)
        press(KeyEvent.KEYCODE_BACK)
        rule.onNodeWithTag("bank").assertIsSelected()
    }
}
