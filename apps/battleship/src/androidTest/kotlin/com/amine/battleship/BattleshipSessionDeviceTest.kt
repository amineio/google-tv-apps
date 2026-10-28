package com.amine.battleship

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.GamePhase
import com.amine.battleship.viewmodel.GameAction
import com.amine.battleship.viewmodel.Screen
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BattleshipSessionDeviceTest {
    @get:Rule val screen = createAndroidComposeRule<MainActivity>()

    @Test fun visibleButPausedActivityCannotAdvanceTheComputerTurn() {
        screen.waitUntil(10_000) { screen.activity.game.state.value.loaded }
        screen.runOnIdle {
            screen.activity.game.action(GameAction.Play)
            screen.activity.game.action(GameAction.Confirm)
            screen.activity.game.action(GameAction.Fire)
        }
        screen.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        android.os.SystemClock.sleep(1_600)
        assertTrue(screen.activity.game.state.value.game!!.playerBoard.shots.isEmpty())
        screen.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        assertTrue(screen.activity.game.state.value.paused)
    }

    @Test fun fullMatchPauseRematchAndActivityReturnUseTheRealSession() {
        screen.waitUntil(10_000) { screen.activity.game.state.value.loaded }
        screen.runOnIdle { screen.activity.game.action(GameAction.Play) }
        if (screen.activity.game.state.value.confirmation != null) {
            screen.runOnIdle { screen.activity.game.action(GameAction.Confirm) }
        }
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        key(Key.DirectionRight)
        key(Key.DirectionDown)
        val selected = screen.activity.game.state.value.selected
        // Activity recreation retains the authoritative match and pauses progression.
        val initial = screen.activity.game.state.value.game
        screen.activityRule.scenario.recreate()
        screen.waitUntil(5_000) { screen.activity.game.state.value.paused }
        assertEquals(initial, screen.activity.game.state.value.game)
        assertEquals(selected, screen.activity.game.state.value.selected)
        screen.onNodeWithText("Resume").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("targeting_board").assertIsFocused()

        // Test code uses the fleet to bound the match to 17 real remote attacks.
        // Production input and the computer opponent never receive this information.
        val targets = screen.activity.game.state.value.game!!.computerBoard.ships.flatMap { it.cells }
        for (target in targets) {
            screen.waitUntil(5_000) {
                val state = screen.activity.game.state.value
                state.game?.phase == GamePhase.PLAYER_TURN && !state.resolving
            }
            aim(target)
            key(Key.DirectionCenter)
        }
        screen.waitUntil(5_000) { screen.activity.game.state.value.screen == Screen.RESULTS }
        screen.onNodeWithText("VICTORY").assertIsDisplayed()
        assertEquals(17, screen.activity.game.state.value.game!!.computerBoard.shots.size)
        screen.onNodeWithText("Play Again").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        assertTrue(screen.activity.game.state.value.game!!.computerBoard.shots.isEmpty())
        assertTrue(screen.activity.game.state.value.game!!.playerBoard.shots.isEmpty())

        // Send Android Back through the Activity so BackHandler must open pause.
        screen.runOnUiThread { screen.activity.onBackPressedDispatcher.onBackPressed() }
        screen.onNodeWithText("Resume").assertIsFocused()
        key(Key.DirectionCenter)
        screen.onNodeWithTag("targeting_board").assertIsFocused()
    }

    private fun aim(target: Coordinate) {
        val current = screen.activity.game.state.value.selected
        repeat(kotlin.math.abs(target.row - current.row)) {
            key(if (target.row > current.row) Key.DirectionDown else Key.DirectionUp)
        }
        repeat(kotlin.math.abs(target.column - current.column)) {
            key(if (target.column > current.column) Key.DirectionRight else Key.DirectionLeft)
        }
        assertEquals(target, screen.activity.game.state.value.selected)
    }

    private fun key(key: Key) { screen.onAllNodes(isRoot()).onLast().performKeyInput { pressKey(key) } }
}
