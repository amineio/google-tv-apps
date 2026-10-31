package com.amine.memoryparade

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoryRemoteTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val model get() = ui.activity.gameModel
    private fun key(key: Key) { ui.onNodeWithTag("game_root").performKeyInput { pressKey(key) }; ui.waitForIdle() }
    private fun select() { key(Key.DirectionCenter) }
    private fun back() { InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); ui.waitForIdle() }
    private fun to(index: Int) {
        while (model.selected / 4 > index / 4) key(Key.DirectionUp)
        while (model.selected / 4 < index / 4) key(Key.DirectionDown)
        while (model.selected % 4 > index % 4) key(Key.DirectionLeft)
        while (model.selected % 4 < index % 4) key(Key.DirectionRight)
    }
    private fun waitFlip() { Thread.sleep(360); ui.waitForIdle() }
    private fun capture(tag: String) {
        ui.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = java.io.File(context.getExternalFilesDir(null), "review").apply { mkdirs() }
        val file = java.io.File(directory, "memory-$tag-${System.currentTimeMillis()}.png")
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val copy = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "cp ${file.absolutePath} /sdcard/Download/${file.name}"
        )
        android.os.ParcelFileDescriptor.AutoCloseInputStream(copy).use { it.readBytes() }
    }
    @Test fun directionalNavigationReachesAllCardsAndPauseRestoresSelection() {
        select()
        for (i in 0..11) { to(i); ui.onNodeWithTag("card_$i").assertIsSelected().assertIsDisplayed() }
        back(); ui.onNodeWithTag("menu_0").assertIsSelected(); select()
        ui.onNodeWithTag("card_11").assertIsSelected()
        ui.activityRule.scenario.recreate(); ui.waitForIdle()
        assertEquals(11, model.selected)
        if (model.screen == Screen.PAUSE) select()
        ui.onNodeWithTag("card_11").assertIsSelected()
    }
    @Test fun mismatchBlocksThirdCardAndPauseCannotDuplicateTurn() {
        select()
        val second = model.game.cards.indices.first { model.game.cards[it] != model.game.cards[0] }
        select(); waitFlip(); to(second); select()
        val third = (second + 1) % 12
        to(third); select()
        assertEquals(1, model.game.moves)
        assertEquals(2, model.game.visible.size)
        back(); Thread.sleep(1200); assertEquals(1, model.game.moves)
        select(); ui.waitUntil(4000) { !model.game.comparing }
        assertTrue(model.game.visible.isEmpty())
        assertEquals(1, model.game.moves)
    }
    @Test fun heldSelectCannotFlipAnotherCardAfterAnimation() {
        select()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val down = android.os.SystemClock.uptimeMillis()
        instrumentation.sendKeySync(KeyEvent(down, down, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        waitFlip()
        key(Key.DirectionRight)
        instrumentation.sendKeySync(KeyEvent(down, android.os.SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 1))
        Thread.sleep(400)
        instrumentation.sendKeySync(KeyEvent(down, android.os.SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 2))
        instrumentation.sendKeySync(KeyEvent(down, android.os.SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        ui.waitForIdle()
        assertEquals(0, model.game.moves)
        assertEquals(setOf(0), model.game.visible)
    }
    @Test fun homeAndReturnPreservePendingCardAndSelection() {
        select(); to(6); select(); waitFlip()
        val card = model.game.first
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("input keyevent 3")
        ).use { it.readBytes() }
        Thread.sleep(600)
        assertEquals(Screen.PAUSE, model.screen)
        assertEquals(card, model.game.first)
        instrumentation.targetContext.startActivity(android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        ui.waitForIdle()
        ui.onNodeWithTag("menu_0").assertIsSelected()
        select(); ui.onNodeWithTag("card_6").assertIsSelected()
        assertEquals(card, model.game.first)
        assertEquals(0, model.game.moves)
    }
    @Test fun soundPreferenceSurvivesReopen() {
        select(); back()
        val previous = model.sound
        key(Key.DirectionDown); key(Key.DirectionDown); select()
        assertEquals(!previous, model.sound)
        assertEquals(!previous, GameViewModel(ui.activity.application).sound)
        select()
        assertEquals(previous, GameViewModel(ui.activity.application).sound)
    }
    @Test fun fullGameWithRealDirectionalInputCompletesAndRestarts() {
        select()
        val locations = model.game.cards.withIndex().groupBy({ it.value }, { it.index })
        Animal.entries.forEach { animal ->
            val pair = locations.getValue(animal)
            to(pair[0]); select(); waitFlip(); to(pair[1]); select()
            ui.waitUntil(4000) { animal in model.game.parade }
            if (model.game.pairs == 3) { Thread.sleep(800); capture("three-pairs") }
        }
        ui.waitUntil(4000) { model.screen == Screen.RESULTS }
        capture("results")
        assertEquals(6, model.game.moves)
        assertEquals(6, model.game.pairs)
        assertEquals(6, model.best)
        val reopened = GameViewModel(ui.activity.application)
        assertEquals(6, reopened.best)
        select(); assertEquals(0, model.game.moves); assertEquals(0, model.game.pairs)
        assertEquals(Screen.GAME, model.screen)
    }
}
