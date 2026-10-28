package com.amine.battleship.game

import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.GamePhase
import com.amine.battleship.game.model.ShipType
import org.junit.Assert.*
import org.junit.Test

class ShipPlacementTest {
    @Test fun `many seeded fleets contain five straight nonoverlapping ships inside the board`() {
        repeat(512) { seed ->
            val state = GameEngine.newGame(seed.toLong())
            assertEquals(GamePhase.PLAYER_TURN, state.phase)
            for (board in listOf(state.playerBoard, state.computerBoard)) {
                assertEquals(5, board.ships.size)
                assertEquals(ShipType.entries.toSet(), board.ships.map { it.type }.toSet())
                val cells = board.ships.flatMap { it.cells }
                assertEquals(17, cells.size)
                assertEquals(17, cells.toSet().size)
                assertTrue(cells.all { it.row in 0..9 && it.column in 0..9 })
                for (ship in board.ships) {
                    assertEquals(ship.type.length, ship.cells.size)
                    val rows = ship.cells.map { it.row }.distinct()
                    val columns = ship.cells.map { it.column }.distinct()
                    assertTrue(rows.size == 1 || columns.size == 1)
                    val positions = if (rows.size == 1) columns.sorted() else rows.sorted()
                    assertEquals(positions.first() + ship.type.length - 1, positions.last())
                }
            }
        }
    }

    @Test fun `seed reproduces both fleets and different seeds vary placement`() {
        assertEquals(GameEngine.newGame(42), GameEngine.newGame(42))
        assertNotEquals(GameEngine.newGame(42).playerBoard, GameEngine.newGame(43).playerBoard)
    }

    @Test fun `coordinates use column letters and one based row labels`() {
        assertEquals("A1", Coordinate(0, 0).label)
        assertEquals("J10", Coordinate(9, 9).label)
        assertFalse(Coordinate(-1, 0).isValid)
        assertFalse(Coordinate(0, 10).isValid)
    }
}
