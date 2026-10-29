package com.amine.battleship

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.Player
import com.amine.battleship.ui.BattleshipApp
import com.amine.battleship.viewmodel.GameAction
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BattleshipRemoteTest {
    @get:Rule val screen = createComposeRule()

    @Test fun languageChangesWithTheRemoteAndKeepsTheLanguageControlFocused() {
        var state by mutableStateOf(SessionState(loaded = true, screen = Screen.SETTINGS))
        screen.setContent { BattleshipApp(state, { action ->
            if (action == GameAction.CycleLanguage) state = state.copy(language = state.language.next())
        }, {}) }
        screen.onNodeWithText("Sound: On").assertIsFocused()
        key(Key.DirectionDown)
        screen.onNodeWithText("Language: English").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithText("Taal: Nederlands").assertIsFocused()
        screen.onNodeWithText("INSTELLINGEN").assertIsDisplayed()
        key(Key.DirectionCenter)
        screen.onNodeWithText("Langue : Français").assertIsFocused()
        screen.onNodeWithText("PARAMÈTRES").assertIsDisplayed()
        key(Key.DirectionCenter)
        screen.onNodeWithText("Language: English").assertIsFocused()
        key(Key.DirectionDown)
        screen.onNodeWithText("Back").assertIsFocused()
    }

    @Test fun aimingKeepsLastShotFeedbackSeparateFromTheSelectedTarget() {
        val game = GameEngine.newGame(42)
        val hit = game.computerBoard.ships.first().cells.first()
        val attacked = GameEngine.attack(game, Player.HUMAN, hit).state
        var state by mutableStateOf(activeGame().copy(game = attacked, selected = hit, message = "Hit"))
        screen.setContent { BattleshipApp(state, { action ->
            if (action is GameAction.Move) state = state.copy(selected = Coordinate(
                (state.selected.row + action.rowDelta).coerceIn(0, 9),
                (state.selected.column + action.columnDelta).coerceIn(0, 9)))
        }, {}) }
        val direction = if (hit.column == 9) Key.DirectionLeft else Key.DirectionRight
        key(direction)
        val target = Coordinate(hit.row, hit.column + if (hit.column == 9) -1 else 1)
        screen.onNodeWithTag("selected_target").assertContentDescriptionContains("Target ${target.label}.", substring = true)
        screen.onNodeWithTag("last_shot_feedback").assertContentDescriptionEquals("Your shot: Hit at ${hit.label}.")
        screen.onNodeWithTag("targeting_board").assertIsFocused()
    }

    @Test fun shotFeedbackStaysCompactWithoutNarrationOrRepeatedTurnInstructions() {
        val game = GameEngine.newGame(42)
        val hit = game.computerBoard.ships.first().cells.first()
        val attacked = GameEngine.attack(game, Player.HUMAN, hit).state
        screen.setContent { BattleshipApp(activeGame().copy(game = attacked, selected = hit, message = "Hit"), {}, {}) }
        screen.onNode(hasText("Hit") and hasAnyAncestor(hasTestTag("last_shot_feedback"))).assertIsDisplayed()
        screen.onNodeWithText("TARGET").assertDoesNotExist()
        screen.onNodeWithText("BATTLESHIP").assertDoesNotExist()
        screen.onNodeWithText("BACK  Pause").assertDoesNotExist()
        screen.onNodeWithText("ARROWS  Aim    OK  Fire    BACK  Pause").assertDoesNotExist()
        screen.onNodeWithText("YOUR SHOT").assertDoesNotExist()
        screen.onNodeWithTag("captain_portrait").assertIsDisplayed()
        val enemy = screen.onNodeWithTag("targeting_board").fetchSemanticsNode().boundsInRoot
        val friendly = screen.onNodeWithTag("friendly_board").fetchSemanticsNode().boundsInRoot
        assertEquals(enemy.width, friendly.width, 1f)
        assertEquals(enemy.height, friendly.height, 1f)
        assertEquals(enemy.top, friendly.top, 1f)
        screen.onNodeWithText(hit.label).assertIsDisplayed()
        screen.onNodeWithText("Stand by, captain").assertDoesNotExist()
        screen.onNodeWithText("The computer is taking its turn").assertDoesNotExist()
        screen.onNodeWithText("Ready to fire").assertDoesNotExist()
        screen.onNodeWithTag("targeting_board").assertIsFocused()
    }

    @Test fun compactBattleKeepsInvalidTargetAndSaveFailureVisible() {
        screen.setContent { BattleshipApp(activeGame().copy(
            message = "Already targeted", saveWarning = "Could not save game."), {}, {}) }
        screen.onNodeWithText("Already targeted").assertIsDisplayed()
        screen.onNodeWithText("Could not save game.").assertIsDisplayed()
        screen.onNodeWithTag("selected_target")
            .assertContentDescriptionEquals("Target A1. Already targeted.")
        screen.onNodeWithText("Unexplored").assertDoesNotExist()
        screen.onNodeWithTag("targeting_board").assertIsFocused()
    }

    // A lost initial focus or incorrect menu action breaks navigation without touch.
    @Test fun mainMenuOffersOnlyWorkingRemoteActions() {
        var state by mutableStateOf(SessionState(loaded = true))
        var exited = false
        screen.setContent {
            BattleshipApp(state, onAction = { action ->
                state = when (action) {
                    GameAction.Help -> state.copy(screen = Screen.HELP)
                    GameAction.Settings -> state.copy(screen = Screen.SETTINGS)
                    GameAction.Back -> state.copy(screen = Screen.HOME)
                    GameAction.ToggleSound -> state.copy(soundEnabled = !state.soundEnabled)
                    else -> state
                }
            }, onExit = { exited = true })
        }
        screen.onNodeWithText("Play Game").assertIsFocused()
        key(Key.DirectionDown)
        screen.onNodeWithText("How to Play").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithText("HOW TO PLAY").assertIsDisplayed()
        screen.onNodeWithText("Back").assertIsFocused()
        key(Key.DirectionCenter)
        key(Key.DirectionDown)
        key(Key.DirectionDown)
        screen.onNodeWithText("Settings").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithText("Sound: On").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithText("Sound: Off").assertIsFocused()
        key(Key.DirectionDown)
        key(Key.DirectionDown)
        key(Key.DirectionCenter)
        repeat(3) { key(Key.DirectionDown) }
        screen.onNodeWithText("Exit").assertIsFocused()
        key(Key.DirectionCenter)
        screen.runOnIdle { assertEquals(true, exited) }
    }

    // Swapped arrows, wrapping, missing focus, or a trapped edge breaks this test.
    @Test fun everyCellIsReachableWithDirectionalInputAndNoWrapping() {
        var state by mutableStateOf(activeGame())
        screen.setContent { BattleshipApp(state, { action ->
            if (action is GameAction.Move) state = state.copy(selected = Coordinate(
                (state.selected.row + action.rowDelta).coerceIn(0, 9),
                (state.selected.column + action.columnDelta).coerceIn(0, 9)))
        }, {}) }
        val visited = mutableSetOf<String>()
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        repeat(10) { row ->
            repeat(10) { step ->
                val column = if (row % 2 == 0) step else 9 - step
                val expected = "${'A' + column}${row + 1}"
                screen.onNodeWithTag("targeting_board")
                    .assertContentDescriptionContains("Target $expected.", substring = true)
                visited += expected
                if (step < 9) key(if (row % 2 == 0) Key.DirectionRight else Key.DirectionLeft)
            }
            if (row < 9) key(Key.DirectionDown)
        }
        assertEquals(100, visited.size)
        repeat(3) { key(Key.DirectionDown); key(Key.DirectionLeft) }
        screen.onNodeWithTag("targeting_board")
            .assertContentDescriptionContains("Target A10.", substring = true)
        repeat(12) { key(Key.DirectionUp); key(Key.DirectionRight) }
        screen.onNodeWithTag("targeting_board")
            .assertContentDescriptionContains("Target J1.", substring = true)
    }

    // A clickable handler, KeyUp fire, or accepted repeat event causes extra shots.
    @Test fun heldSelectFiresOnceAcrossTurnChangesAndConsumesKeyUp() {
        var state by mutableStateOf(activeGame())
        var fires = 0
        lateinit var activity: Activity
        screen.setContent {
            activity = LocalContext.current.activity()
            BattleshipApp(state, { if (it == GameAction.Fire) fires++ }, {})
        }
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        screen.runOnIdle {
            activity.dispatchKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0))
            state = state.copy(resolving = true)
            activity.dispatchKeyEvent(KeyEvent(0, 80, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 1))
        }
        screen.runOnIdle { state = state.copy(resolving = false) }
        screen.runOnIdle {
            activity.dispatchKeyEvent(KeyEvent(0, 800, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 2))
            activity.dispatchKeyEvent(KeyEvent(0, 820, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 0))
            assertEquals(1, fires)
        }
        key(Key.DirectionCenter)
        screen.runOnIdle { assertEquals(2, fires) }
    }

    // Focus escaping during a computer turn makes the next player turn unusable.
    @Test fun computerTurnRetainsBoardFocusAndBlocksFiring() {
        val game = GameEngine.newGame(44L)
        val turn = GameEngine.attack(game, Player.HUMAN, Coordinate(0, 0)).state
        var fires = 0
        screen.setContent {
            BattleshipApp(activeGame().copy(game = turn), { if (it == GameAction.Fire) fires++ }, {})
        }
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        repeat(4) { key(Key.DirectionCenter) }
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        screen.runOnIdle { assertEquals(0, fires) }
    }

    // Closing overlays must restore the selected coordinate and board focus.
    @Test fun pauseAndConfirmationReturnFocusWithoutLosingTarget() {
        var state by mutableStateOf(activeGame().copy(selected = Coordinate(4, 6)))
        screen.setContent { BattleshipApp(state, { action ->
            state = when (action) {
                GameAction.Pause, GameAction.Back -> state.copy(paused = true)
                GameAction.Resume -> state.copy(paused = false)
                GameAction.Restart -> state.copy(confirmation = com.amine.battleship.viewmodel.Confirmation.RESTART)
                GameAction.Cancel -> state.copy(confirmation = null)
                GameAction.Confirm -> activeGame()
                else -> state
            }
        }, {}) }
        screen.runOnIdle { state = state.copy(paused = true) }
        screen.onNodeWithText("Resume").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        screen.onNodeWithTag("targeting_board").assertContentDescriptionContains("Target G5.", substring = true)
        screen.runOnIdle { state = state.copy(paused = true) }
        key(Key.DirectionDown)
        key(Key.DirectionCenter)
        screen.onNodeWithText("Keep Playing").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithText("Resume").assertIsFocused()
        key(Key.DirectionDown)
        key(Key.DirectionCenter)
        key(Key.DirectionRight)
        key(Key.DirectionCenter)
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        screen.onNodeWithTag("targeting_board").assertContentDescriptionContains("Target A1.", substring = true)
        screen.runOnIdle { assertTrue(state.game!!.playerBoard.shots.isEmpty()) }
    }

    private fun activeGame() = SessionState(loaded = true, screen = Screen.GAME,
        game = GameEngine.newGame(982L), showHint = true)

    private fun Context.activity(): Activity = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> error("A test Activity is required")
    }

    private fun key(key: Key) {
        screen.onAllNodes(isRoot()).onLast().performKeyInput { pressKey(key) }
    }
}
