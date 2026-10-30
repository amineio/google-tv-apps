package com.amine.battleship

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import com.amine.battleship.persistence.*
import com.amine.battleship.ui.BattleshipApp
import com.amine.battleship.viewmodel.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

/** All focus changes use directional events. No destination receives forced focus. */
@RunWith(AndroidJUnit4::class)
class LocalTwoPlayerRemoteTest {
    @get:Rule val screen = createComposeRule()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: GameSession
    private lateinit var activity: Activity
    private lateinit var file: File
    @Volatile private var nowNanos = 0L

    @After fun close() { scope.cancel(); if (::file.isInitialized) file.delete() }

    private fun launch(saved: SavedSession? = null) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File.createTempFile("local-battleship-", ".json", context.cacheDir)
        val repository = GameSaveRepository(file)
        if (saved != null) repository.save(saved)
        session = GameSession(scope, repository, seedSource = { 42L }, nanoTimeSource = { nowNanos })
        screen.setContent {
            activity = LocalContext.current.activity()
            val state by session.state.collectAsState()
            BattleshipApp(state, session::action, {})
        }
        screen.waitUntil(5_000) { session.state.value.loaded }
    }

    private fun localSetup(existing: Boolean = false) {
        if (existing) key(Key.DirectionDown)
        screen.onNodeWithText("Play Game").assertIsFocused(); key(Key.DirectionCenter)
        screen.onNodeWithText("Computer").assertIsFocused()
        key(Key.DirectionDown); screen.onNodeWithText("Two captains").assertIsFocused(); key(Key.DirectionCenter)
        screen.onNodeWithTag("portrait_fox").assertIsFocused()
    }

    private fun reviewAndStart(existing: Boolean = false) {
        // Bottom portrait rows explicitly lead to Edit name for predictable remote navigation.
        key(Key.DirectionDown); expectFocused(hasTestTag("portrait_storm"))
        key(Key.DirectionDown); expectFocused(hasText("Edit name (optional)"))
        key(Key.DirectionRight); expectFocused(hasText("Next captain")); key(Key.DirectionCenter)
        expectFocused(hasTestTag("portrait_owl"))
        key(Key.DirectionDown); expectFocused(hasTestTag("portrait_reed"))
        key(Key.DirectionDown); expectFocused(hasText("Edit name (optional)"))
        key(Key.DirectionRight); expectFocused(hasText("Review captains")); key(Key.DirectionCenter)
        expectFocused(hasText("Start match")); key(Key.DirectionCenter)
        if (existing) { screen.onNodeWithText("Keep Playing").assertIsFocused(); key(Key.DirectionRight); key(Key.DirectionCenter) }
        screen.onNodeWithText("Fox, ready?").assertIsDisplayed()
    }

    private fun readyAndHide() {
        key(Key.DirectionCenter); screen.onNodeWithText("Owl, ready?").assertIsDisplayed()
        key(Key.DirectionCenter)
        screen.waitUntil(5_000) { session.state.value.revealRemainingMs != null }
        screen.onNodeWithText("Remember both fleets · 10").assertIsDisplayed()
        screen.runOnIdle { nowNanos = 10_000_000_000L }
        screen.waitUntil(5_000) { session.state.value.revealRemainingMs == null }
        assertBoardFocused("right_fleet_board")
    }

    @Test fun chooserUsesDirectionalFocusAndCancelPreservesThePreviousMatch() {
        val old = GameEngine.newGame(19)
        launch(SavedSession(game = old)); localSetup(existing = true)
        key(Key.DirectionDown); screen.onNodeWithTag("portrait_storm").assertIsFocused()
        key(Key.DirectionRight); screen.onNodeWithTag("portrait_reed").assertIsFocused()
        key(Key.DirectionCenter)
        screen.runOnIdle { assertEquals("reed", session.state.value.setup!!.left.portraitId) }
        nativeBack()
        screen.onNodeWithText("Continue Game").assertIsFocused()
        screen.runOnIdle { assertEquals(old, session.state.value.game); assertEquals(old, GameSaveRepository(file).load()!!.game) }
    }

    @Test fun editorCancelRestoresFocusAndDefaultNameWithoutSavingDraft() {
        launch(); localSetup()
        key(Key.DirectionDown); key(Key.DirectionDown)
        screen.onNodeWithText("Edit name (optional)").assertIsFocused(); key(Key.DirectionCenter)
        screen.onNodeWithTag("name_editor").assertIsFocused().performTextInput("Harbor")
        // Dismiss the native IME, then cancel the dialog through its Back handler.
        nativeBack()
        if (screen.onAllNodesWithTag("name_editor").fetchSemanticsNodes().isNotEmpty()) nativeBack()
        screen.waitUntil(5_000) { screen.onAllNodesWithTag("name_editor").fetchSemanticsNodes().isEmpty() }
        screen.onNodeWithText("Edit name (optional)").assertIsFocused()
        screen.runOnIdle { assertNull(session.state.value.setup!!.left.customName); assertNull(session.state.value.game) }
    }

    @Test fun previewBlocksShotsThenBothFixedBoardsAlternateThroughReady() {
        launch(); localSetup(); reviewAndStart()
        key(Key.DirectionCenter); key(Key.DirectionCenter)
        screen.waitUntil(5_000) { session.state.value.revealRemainingMs != null }
        key(Key.DirectionRight); key(Key.DirectionCenter)
        screen.runOnIdle {
            assertEquals(Coordinate(0, 0), session.state.value.selected)
            assertTrue(session.state.value.game!!.computerBoard.shots.isEmpty())
            nowNanos = 10_000_000_000L
        }
        screen.waitUntil(5_000) { session.state.value.revealRemainingMs == null }
        assertBoardFocused("right_fleet_board")
            .assertContentDescriptionContains("Fox targets Owl’s fleet. Target A1.", substring = true)
        screen.onNodeWithTag("left_fleet_board").assertContentDescriptionEquals("Fox’s fleet. 0 public shots. Intact ships hidden.")
        key(Key.DirectionRight); key(Key.DirectionCenter)
        screen.waitUntil(6_000) { session.state.value.readySeat == Player.COMPUTER }
        screen.onNodeWithText("Owl, ready?").assertIsDisplayed()
        key(Key.DirectionCenter)
        assertBoardFocused("left_fleet_board")
            .assertContentDescriptionContains("Owl targets Fox’s fleet.", substring = true)
        key(Key.DirectionDown); key(Key.DirectionCenter)
        screen.waitUntil(6_000) { session.state.value.readySeat == Player.HUMAN }
        screen.runOnIdle { assertEquals(1, session.state.value.game!!.playerBoard.shots.size); assertEquals(1, session.state.value.game!!.computerBoard.shots.size) }
    }

    @Test fun heldReadyCannotBecomeTheNextCaptainsShot() {
        launch(); localSetup(); reviewAndStart(); readyAndHide()
        key(Key.DirectionCenter)
        screen.waitUntil(6_000) { session.state.value.readySeat == Player.COMPUTER }
        screen.runOnIdle { activity.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0)) }
        assertBoardFocused("left_fleet_board")
        screen.runOnIdle {
            activity.dispatchKeyEvent(KeyEvent(0, 100, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 1))
            activity.dispatchKeyEvent(KeyEvent(0, 120, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))
            assertTrue(session.state.value.game!!.playerBoard.shots.isEmpty())
        }
        key(Key.DirectionCenter)
        screen.runOnIdle { assertEquals(1, session.state.value.game!!.playerBoard.shots.size) }
    }

    @Test fun interruptedPreviewReturnsHiddenWithNamedReadyAfterResume() {
        launch(); localSetup(); reviewAndStart()
        key(Key.DirectionCenter); key(Key.DirectionCenter)
        screen.waitUntil(5_000) { session.state.value.revealRemainingMs != null }
        nativeBack()
        screen.onNodeWithText("Resume").assertIsFocused()
        screen.onNodeWithText("The preview ended. Both fleets remain hidden.").assertIsDisplayed()
        key(Key.DirectionCenter)
        screen.onNodeWithText("Fox, ready?").assertIsDisplayed(); key(Key.DirectionCenter)
        assertBoardFocused("right_fleet_board")
        screen.onNodeWithText("Remember both fleets · 10").assertDoesNotExist()
        screen.onNodeWithTag("left_fleet_board").assertContentDescriptionEquals("Fox’s fleet. 0 public shots. Intact ships hidden.")
    }

    private fun assertBoardFocused(tag: String): SemanticsNodeInteraction {
        try {
            return screen.onNodeWithTag(tag).assertIsFocused()
        } catch (failure: AssertionError) {
            val state = session.state.value
            throw AssertionError("Board focus/tag failure: screen=${state.screen}, mode=${state.game?.mode}, " +
                "phase=${state.game?.phase}, reveal=${state.revealRemainingMs}, ready=${state.readySeat}, " +
                "paused=${state.paused}, resolving=${state.resolving}. Semantics:\n" +
                screen.onAllNodes(isRoot()).printToString(), failure)
        }
    }
    private fun expectFocused(matcher: SemanticsMatcher) {
        try {
            screen.onNode(matcher).assertIsFocused()
        } catch (failure: AssertionError) {
            throw AssertionError("Unexpected D-pad destination. Focused semantics:\n" +
                screen.onAllNodes(isFocused()).printToString(), failure)
        }
    }
    private fun nativeBack() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        screen.waitForIdle()
    }
    private fun key(key: Key) { screen.onAllNodes(isRoot()).onLast().performKeyInput { pressKey(key) } }
    private fun Context.activity(): Activity = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> error("Activity required")
    }
}
