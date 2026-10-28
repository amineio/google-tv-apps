package com.amine.battleship.game

import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class GameEngineTest {
    @Test fun `shot reports miss and alternates turns without altering original snapshot`() {
        val initial = GameEngine.newGame(11)
        val miss = emptyCell(initial.computerBoard)
        val result = GameEngine.attack(initial, Player.HUMAN, miss)
        assertTrue(result.accepted)
        assertEquals(ShotOutcome.MISS, result.shot?.outcome)
        assertEquals(GamePhase.COMPUTER_TURN, result.state.phase)
        assertEquals(1, result.state.computerBoard.shots.size)
        assertTrue(initial.computerBoard.shots.isEmpty())
        val reply = GameEngine.attack(result.state, Player.COMPUTER, emptyCell(initial.playerBoard))
        assertEquals(GamePhase.PLAYER_TURN, reply.state.phase)
        assertTrue(GameEngine.isValid(reply.state))
    }

    @Test fun `hit damages only its ship and final segment sinks that ship`() {
        var state = GameEngine.newGame(12)
        val ship = state.computerBoard.ships.first { it.type == ShipType.DESTROYER }
        val first = GameEngine.attack(state, Player.HUMAN, ship.cells.first())
        assertEquals(ShotOutcome.HIT, first.shot?.outcome)
        assertNull(first.shot?.shipType)
        assertFalse(first.state.computerBoard.shipSunk(ship))
        assertEquals(1, first.state.computerBoard.totalHits)
        state = computerMiss(first.state)
        val last = GameEngine.attack(state, Player.HUMAN, ship.cells.last())
        assertEquals(ShotOutcome.SUNK, last.shot?.outcome)
        assertEquals(ShipType.DESTROYER, last.shot?.shipType)
        assertEquals(ship.cells.toSet(), last.shot?.sunkCells?.toSet())
        assertTrue(last.state.computerBoard.shipSunk(ship))
        assertEquals(1, last.state.computerBoard.ships.count { last.state.computerBoard.shipSunk(it) })
        assertEquals(GamePhase.COMPUTER_TURN, last.state.phase)
    }

    @Test fun `out of bounds wrong turn and repeat shots preserve the exact state`() {
        val initial = GameEngine.newGame(13)
        for (coordinate in listOf(Coordinate(-1, 0), Coordinate(10, 0), Coordinate(0, 10))) {
            val rejected = GameEngine.attack(initial, Player.HUMAN, coordinate)
            assertFalse(rejected.accepted)
            assertSame(initial, rejected.state)
        }
        val wrongTurn = GameEngine.attack(initial, Player.COMPUTER, Coordinate(0, 0))
        assertFalse(wrongTurn.accepted)
        assertSame(initial, wrongTurn.state)
        val coordinate = Coordinate(0, 0)
        val afterRound = computerMiss(GameEngine.attack(initial, Player.HUMAN, coordinate).state)
        val repeated = GameEngine.attack(afterRound, Player.HUMAN, coordinate)
        assertFalse(repeated.accepted)
        assertSame(afterRound, repeated.state)
        assertEquals(GamePhase.PLAYER_TURN, repeated.state.phase)
    }

    @Test fun `destroying all enemy ships wins and forbids all later attacks`() {
        var state = GameEngine.newGame(14)
        val targets = state.computerBoard.ships.flatMap { it.cells }
        targets.forEachIndexed { index, coordinate ->
            val result = GameEngine.attack(state, Player.HUMAN, coordinate)
            assertTrue(result.accepted)
            state = result.state
            if (index < targets.lastIndex) {
                assertNull(state.winner)
                state = computerMiss(state)
            }
        }
        assertEquals(GamePhase.GAME_OVER, state.phase)
        assertEquals(Player.HUMAN, state.winner)
        assertTrue(GameEngine.isValid(state))
        for (player in Player.entries) {
            val rejected = GameEngine.attack(state, player, emptyCell(state.computerBoard))
            assertFalse(rejected.accepted)
            assertSame(state, rejected.state)
        }
    }

    @Test fun `opponent view reveals sunk ships only while own ships remain visible`() {
        var state = GameEngine.newGame(15)
        val original = GameEngine.playerView(state)
        assertEquals(5, original.ownBoard.ships.size)
        assertTrue(original.opponentBoard.ships.isEmpty())
        val ship = state.computerBoard.ships.first { it.type == ShipType.CRUISER }
        ship.cells.forEachIndexed { index, coordinate ->
            state = GameEngine.attack(state, Player.HUMAN, coordinate).state
            val visible = GameEngine.playerView(state)
            if (index < ship.cells.lastIndex) assertTrue(visible.opponentBoard.ships.isEmpty())
            else assertEquals(listOf(ship), visible.opponentBoard.ships)
            if (index < ship.cells.lastIndex) state = computerMiss(state)
        }
        assertEquals(state.computerBoard.shots, GameEngine.playerView(state).opponentBoard.shots)
    }

    @Test fun `serialized state retains a pending computer turn and shot history`() {
        val original = GameEngine.newGame(16)
        val state = GameEngine.attack(original, Player.HUMAN, Coordinate(3, 3)).state
        val restored = Json.decodeFromString<GameState>(Json.encodeToString(state))
        assertEquals(state, restored)
        assertEquals(GamePhase.COMPUTER_TURN, restored.phase)
        assertTrue(GameEngine.isValid(restored))
    }

    @Test fun `save validation rejects overlapping fleets forged results and impossible turn counts`() {
        val state = GameEngine.newGame(17)
        val ships = state.playerBoard.ships
        val invalidShip = ships[1].copy(cells = ships[0].cells.take(ships[1].type.length))
        assertFalse(GameEngine.isValid(state.copy(playerBoard = state.playerBoard.copy(ships = listOf(ships[0], invalidShip) + ships.drop(2)))))
        val forged = Shot(emptyCell(state.computerBoard), ShotOutcome.HIT)
        assertFalse(GameEngine.isValid(state.copy(computerBoard = state.computerBoard.copy(shots = listOf(forged)))))
        assertFalse(GameEngine.isValid(state.copy(phase = GamePhase.COMPUTER_TURN)))
        assertFalse(GameEngine.isValid(state.copy(winner = Player.HUMAN)))
    }

    private fun computerMiss(state: GameState): GameState =
        GameEngine.attack(state, Player.COMPUTER, emptyCell(state.playerBoard)).also { assertTrue(it.accepted) }.state

    private fun emptyCell(board: Board): Coordinate =
        (0..9).flatMap { row -> (0..9).map { column -> Coordinate(row, column) } }
            .first { coordinate -> board.ships.none { coordinate in it.cells } && board.shots.none { it.coordinate == coordinate } }
}
