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
    private fun start(size: BoardSize = BoardSize.CLASSIC) {
        assertEquals(Screen.WELCOME, model.screen)
        while (model.menuIndex > size.ordinal) key(Key.DirectionLeft)
        while (model.menuIndex < size.ordinal) key(Key.DirectionRight)
        ui.onNodeWithTag("board_${size.key}").assertIsSelected().assertIsDisplayed()
        select(); assertEquals(size, model.game.size)
    }
    private fun to(index: Int) {
        val columns = model.game.size.columns
        while (model.selected / columns > index / columns) key(Key.DirectionUp)
        while (model.selected / columns < index / columns) key(Key.DirectionDown)
        while (model.selected % columns > index % columns) key(Key.DirectionLeft)
        while (model.selected % columns < index % columns) key(Key.DirectionRight)
    }
    private fun waitFlip() { Thread.sleep(350); ui.waitForIdle() }
    private fun chooseBoardFromGame() { back(); repeat(3) { key(Key.DirectionDown) }; select(); assertEquals(Screen.WELCOME, model.screen) }
    private fun capture(tag: String) {
        ui.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = java.io.File(context.getExternalFilesDir(null), "review").apply { mkdirs() }
        val file = java.io.File(directory, "memory-v11-$tag-${System.currentTimeMillis()}.png")
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cp ${file.absolutePath} /sdcard/Download/${file.name}")
        ).use { it.readBytes() }
    }
    @Test fun directionalNavigationReachesEveryCellOnEveryBoardAndRestoresFocus() {
        for (size in BoardSize.entries) {
            start(size)
            for (i in 0 until size.cards) { to(i); ui.onNodeWithTag("card_$i").assertIsSelected().assertIsDisplayed() }
            back(); ui.onNodeWithTag("menu_0").assertIsSelected(); select()
            ui.onNodeWithTag("card_${size.cards - 1}").assertIsSelected()
            chooseBoardFromGame()
        }
    }
    @Test fun mismatchBlocksThirdCardAndPauseCannotDuplicateTurnOnEveryBoard() {
        for (size in BoardSize.entries) {
            start(size)
            val second = model.game.cards.indices.first { model.game.cards[it] != model.game.cards[0] }
            select(); waitFlip(); to(second); select()
            to((second + 1) % size.cards); select()
            assertEquals(1, model.game.moves)
            assertEquals(2, model.game.visible.size)
            back(); Thread.sleep(1000); assertEquals(1, model.game.moves)
            select(); ui.waitUntil(4000) { !model.game.comparing }
            assertTrue(model.game.visible.isEmpty()); assertEquals(1, model.game.moves)
            chooseBoardFromGame()
        }
    }
    @Test fun heldSelectCannotFlipAnotherCardAfterAnimation() {
        start()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val down = android.os.SystemClock.uptimeMillis()
        instrumentation.sendKeySync(KeyEvent(down, down, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        waitFlip(); key(Key.DirectionRight)
        instrumentation.sendKeySync(KeyEvent(down, android.os.SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 1))
        Thread.sleep(400)
        instrumentation.sendKeySync(KeyEvent(down, android.os.SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 2))
        instrumentation.sendKeySync(KeyEvent(down, android.os.SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))
        ui.waitForIdle(); assertEquals(0, model.game.moves); assertEquals(setOf(0), model.game.visible)
    }
    @Test fun homeAndRecreationPreservePendingCardAndSelection() {
        start(); to(6); select(); waitFlip()
        val card = model.game.first
        ui.activityRule.scenario.recreate(); ui.waitForIdle()
        assertEquals(Screen.PAUSE, model.screen); select()
        ui.onNodeWithTag("card_6").assertIsSelected(); assertEquals(card, model.game.first)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand("input keyevent 3")).use { it.readBytes() }
        Thread.sleep(600); assertEquals(Screen.PAUSE, model.screen); assertEquals(card, model.game.first)
        instrumentation.targetContext.startActivity(android.content.Intent(instrumentation.targetContext, MainActivity::class.java)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        ui.waitForIdle(); ui.onNodeWithTag("menu_0").assertIsSelected(); select()
        ui.onNodeWithTag("card_6").assertIsSelected(); assertEquals(card, model.game.first); assertEquals(0, model.game.moves)
    }
    @Test fun soundPreferenceSurvivesReopen() {
        start(); back()
        val previous = model.sound
        key(Key.DirectionDown); key(Key.DirectionDown); select()
        assertEquals(!previous, model.sound); assertEquals(!previous, GameViewModel(ui.activity.application).sound)
        select(); assertEquals(previous, GameViewModel(ui.activity.application).sound)
    }
    @Test fun audioEventsConsumeOnceAndDoNotReplayAfterRecreationOrPause() {
        start()
        ui.runOnIdle {
            model.direction(Direction.RIGHT)
            assertEquals(if (model.sound) "navigate" else null, model.consumeAudioEvent())
            assertNull(model.consumeAudioEvent())
            model.direction(Direction.LEFT)
            model.onActivityActive(false)
            assertNull(model.consumeAudioEvent())
        }
        ui.activityRule.scenario.recreate(); ui.waitForIdle()
        assertNull(model.consumeAudioEvent())
        assertEquals(Screen.PAUSE, model.screen)
    }
    @Test fun easyGameCompletesAndKeepsSeparateRecord() = complete(BoardSize.EASY)
    @Test fun classicGameCompletesAndKeepsSeparateRecord() = complete(BoardSize.CLASSIC)
    @Test fun challengeGameCompletesWithEightAnimalsAndKeepsRecord() = complete(BoardSize.CHALLENGE)
    private fun complete(size: BoardSize) {
        start(size)
        val locations = model.game.cards.withIndex().groupBy({ it.value }, { it.index })
        model.game.cards.distinct().forEach { animal ->
            val pair = locations.getValue(animal)
            to(pair[0]); select(); waitFlip(); to(pair[1]); select()
            ui.waitUntil(4000) { animal in model.game.parade }
            if (model.game.pairs == minOf(3, size.pairs - 1)) capture("${size.key}-progress")
        }
        ui.waitForIdle()
        assertEquals(Screen.GAME, model.screen)
        capture("${size.key}-full-parade")
        ui.waitUntil(5000) { model.screen == Screen.RESULTS }
        capture("${size.key}-results")
        assertEquals(size.pairs, model.game.moves); assertEquals(size.pairs, model.game.pairs); assertEquals(size.pairs, model.best)
        val reopened = GameViewModel(ui.activity.application)
        assertEquals(size, reopened.chosenBoard); assertEquals(size.pairs, reopened.bestFor(size))
        select(); assertEquals(0, model.game.moves); assertEquals(0, model.game.pairs); assertEquals(size, model.game.size)
        chooseBoardFromGame(); ui.onNodeWithTag("board_${size.key}").assertIsSelected()
    }
}
