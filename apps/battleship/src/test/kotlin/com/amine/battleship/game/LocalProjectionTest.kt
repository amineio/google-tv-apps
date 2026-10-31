package com.amine.battleship.game

import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import org.junit.Assert.*
import org.junit.Test

class LocalProjectionTest {
    private fun public(board: Board): VisibleBoard {
        val method = GameEngine.javaClass.methods.firstOrNull { it.name == "publicBoard" }
        assertNotNull("GameEngine.publicBoard must strip private ships from either fixed board", method)
        return method!!.invoke(GameEngine, board) as VisibleBoard
    }
    @Test fun neitherFreshBoardExposesAnIntactFleet() {
        val game = GameEngine.newGame(42)
        for (board in listOf(game.playerBoard, game.computerBoard)) {
            assertTrue(public(board).ships.isEmpty()); assertTrue(public(board).shots.isEmpty())
        }
    }
    @Test fun publicHitsContainNoUnsunkShipAssociation() {
        val board = GameEngine.newGame(42).playerBoard
        val hit = Shot(board.ships.first().cells.first(), ShotOutcome.HIT)
        val projection = public(board.copy(shots = listOf(hit, Shot(Coordinate(9, 9), ShotOutcome.MISS))))
        assertTrue(projection.ships.isEmpty()); assertEquals(2, projection.shots.size)
        assertNull(projection.shots.first().shipType); assertTrue(projection.shots.first().sunkCells.isEmpty())
    }
    @Test fun onlySunkShapesAndPublicMarkersSurviveProjection() {
        val board = GameEngine.newGame(42).computerBoard; val ship = board.ships.last()
        val shots = ship.cells.mapIndexed { index, cell -> if (index == ship.cells.lastIndex) Shot(cell, ShotOutcome.SUNK, ship.type, ship.cells) else Shot(cell, ShotOutcome.HIT) }
        val projection = public(board.copy(shots = shots))
        assertEquals(listOf(ship), projection.ships); assertEquals(shots, projection.shots)
        assertFalse(projection.ships.any { it.type != ship.type })
    }
}
