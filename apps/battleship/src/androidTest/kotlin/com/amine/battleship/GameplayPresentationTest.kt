package com.amine.battleship

import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import com.amine.battleship.ui.BattleshipApp
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class GameplayPresentationTest {
    @get:Rule val screen = createComposeRule()

    @Test fun denseFleetAndSunkFeedbackRemainVisibleWithBoardFocus() {
        var state by mutableStateOf(SessionState(loaded = true, screen = Screen.GAME,
            game = GameEngine.newGame(992L), selected = Coordinate(4, 4), showHint = true))
        screen.setContent { BattleshipApp(state, {}, {}) }
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        screen.onNodeWithTag("friendly_board").assertIsDisplayed()
        val targetText = mutableListOf<TextLayoutResult>()
        screen.onNodeWithTag("selected_target").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(targetText) }
        assertFalse("The target coordinate must show its complete text", targetText.single().hasVisualOverflow)
        val firstEnemy = screen.onNodeWithTag("targeting_board").getUnclippedBoundsInRoot()
        val firstFriendly = screen.onNodeWithTag("friendly_board").getUnclippedBoundsInRoot()
        assertEquals((firstEnemy.right - firstEnemy.left).value, (firstFriendly.right - firstFriendly.left).value, .1f)
        assertEquals(firstEnemy.top, firstFriendly.top)
        assertEquals(firstEnemy.bottom, firstFriendly.bottom)
        assertTrue("Grid pitch must be at least 30dp", (firstEnemy.right - firstEnemy.left).value * .91826f / 10f >= 30f)
        for (elapsed in listOf(500L, 1500L)) {
            screen.mainClock.advanceTimeBy(elapsed)
            assertEquals(firstEnemy, screen.onNodeWithTag("targeting_board").getUnclippedBoundsInRoot())
            assertEquals(firstFriendly, screen.onNodeWithTag("friendly_board").getUnclippedBoundsInRoot())
        }
        capture("overview")

        val dense = denseMatch()
        screen.runOnIdle { state = state.copy(game = dense, selected = Coordinate(9, 9), showHint = false) }
        screen.onNodeWithText("Carrier sunk").assertIsDisplayed()
        screen.onNodeWithText("INCOMING").assertDoesNotExist()
        screen.onNodeWithTag("last_shot_feedback").assertContentDescriptionContains("Incoming: Carrier sunk", substring = true)
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        val enemy = screen.onNodeWithTag("targeting_board").getUnclippedBoundsInRoot()
        val friendly = screen.onNodeWithTag("friendly_board").getUnclippedBoundsInRoot()
        assertEquals(firstEnemy, enemy)
        assertEquals(firstFriendly, friendly)
        val longFeedback = dense.copy(lastAttack = AttackEvent(Player.HUMAN,
            Shot(Coordinate(4, 4), ShotOutcome.SUNK, ShipType.BATTLESHIP)))
        screen.runOnIdle { state = state.copy(game = longFeedback, message = "Already targeted") }
        screen.onNodeWithText("Battleship sunk").assertIsDisplayed()
        val resultText = mutableListOf<TextLayoutResult>()
        screen.onNodeWithText("Battleship sunk").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(resultText) }
        assertFalse("Long feedback must fit its fixed status row", resultText.single().hasVisualOverflow)
        screen.onNodeWithText("Already targeted").assertIsDisplayed()
        assertEquals(firstEnemy, screen.onNodeWithTag("targeting_board").getUnclippedBoundsInRoot())
        assertEquals(firstFriendly, screen.onNodeWithTag("friendly_board").getUnclippedBoundsInRoot())
        capture("long-feedback")

        capture("dense")

        val unused = allCells().first { target -> dense.computerBoard.shots.none { it.coordinate == target } }
        val pending = GameEngine.attack(dense, Player.HUMAN, unused).state
        screen.runOnIdle { state = state.copy(game = pending, selected = unused, resolving = true) }
        screen.onNodeWithText("COMPUTER TURN").assertIsDisplayed()
        screen.onNodeWithText("YOUR SHOT").assertDoesNotExist()
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        capture("computer-turn")

        screen.runOnIdle { state = state.copy(game = denseMatch(lateGame = true),
            selected = Coordinate(9, 9), resolving = false) }
        screen.onNodeWithText("3 / 5 SUNK").assertIsDisplayed()
        screen.onNodeWithText("2 / 5 AFLOAT").assertIsDisplayed()
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        capture("late-game")
    }

    private fun denseMatch(lateGame: Boolean = false): GameState {
        var game = GameEngine.newGame(992L)
        fun targets(board: Board): List<Coordinate> {
            val carrier = board.ships.first { it.type == ShipType.AIRCRAFT_CARRIER }
            val occupied = board.ships.flatMap { it.cells }.toSet()
            val escortCells = if (lateGame) board.ships.filter {
                it.type in listOf(ShipType.BATTLESHIP, ShipType.CRUISER)
            }.flatMap { it.cells } else board.ships.filter {
                it.type != ShipType.AIRCRAFT_CARRIER
            }.take(3).map { it.cells.first() }
            return allCells().filter { it !in occupied }.take(if (lateGame) 60 else 13) + escortCells + carrier.cells
        }
        val human = targets(game.computerBoard)
        val computer = targets(game.playerBoard)
        for ((attack, response) in human.zip(computer)) {
            val shot = GameEngine.attack(game, Player.HUMAN, attack)
            check(shot.accepted)
            val reply = GameEngine.attack(shot.state, Player.COMPUTER, response)
            check(reply.accepted)
            game = reply.state
        }
        return game
    }

    private fun allCells() = (0..9).flatMap { row -> (0..9).map { column -> Coordinate(row, column) } }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureGameplay") != "true") return
        screen.waitForIdle()
        val command = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("screencap -p /data/local/tmp/battleship-design-$name.png")
        ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
    }
}
