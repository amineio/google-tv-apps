package com.amine.battleship.game

import com.amine.battleship.game.ai.AttackKnowledge
import com.amine.battleship.game.ai.HuntTargetStrategy
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class HuntTargetStrategyTest {
    @Test fun `one unresolved hit targets an adjacent unattacked cell`() {
        val hit = Coordinate(4, 4)
        val knowledge = AttackKnowledge.fromShots(listOf(Shot(hit, ShotOutcome.HIT)))
        repeat(30) { seed ->
            val target = HuntTargetStrategy(seed.toLong()).chooseShot(knowledge)
            assertEquals(1, abs(target.row - hit.row) + abs(target.column - hit.column))
        }
    }

    @Test fun `aligned hits prioritize extending their inferred direction`() {
        val knowledge = AttackKnowledge.fromShots(listOf(
            Shot(Coordinate(4, 4), ShotOutcome.HIT),
            Shot(Coordinate(4, 5), ShotOutcome.HIT),
        ))
        repeat(30) { seed ->
            val target = HuntTargetStrategy(seed.toLong()).chooseShot(knowledge)
            assertTrue(target == Coordinate(4, 3) || target == Coordinate(4, 6))
        }
    }

    @Test fun `sinking touching ship retains other unresolved damage`() {
        val shots = listOf(
            Shot(Coordinate(3, 3), ShotOutcome.HIT),
            Shot(Coordinate(4, 3), ShotOutcome.HIT),
            Shot(Coordinate(3, 4), ShotOutcome.SUNK, ShipType.DESTROYER,
                listOf(Coordinate(3, 3), Coordinate(3, 4))),
        )
        val knowledge = AttackKnowledge.fromShots(shots)
        assertEquals(listOf(5, 4, 3, 3), knowledge.remainingShipLengths)
        assertEquals(listOf(Ship(ShipType.DESTROYER, listOf(Coordinate(3, 3), Coordinate(3, 4)))), knowledge.sunkShips)
        repeat(30) { seed ->
            val target = HuntTargetStrategy(seed.toLong()).chooseShot(knowledge)
            assertEquals(1, abs(target.row - 4) + abs(target.column - 3))
            assertFalse(shots.any { it.coordinate == target })
        }
    }

    @Test fun `identical public history produces identical choice regardless of hidden fleet`() {
        val first = GameEngine.newGame(91)
        val second = GameEngine.newGame(92)
        assertNotEquals(first.playerBoard.ships, second.playerBoard.ships)
        val knowledgeOne = AttackKnowledge.fromShots(first.playerBoard.shots)
        val knowledgeTwo = AttackKnowledge.fromShots(second.playerBoard.shots)
        assertEquals(HuntTargetStrategy(9).chooseShot(knowledgeOne), HuntTargetStrategy(9).chooseShot(knowledgeTwo))
    }

    @Test fun `knowledge reconstructed from history produces deterministic replay`() {
        val history = listOf(Shot(Coordinate(0, 0), ShotOutcome.MISS), Shot(Coordinate(4, 4), ShotOutcome.HIT))
        assertEquals(
            HuntTargetStrategy(71).chooseShot(AttackKnowledge.fromShots(history)),
            HuntTargetStrategy(71).chooseShot(AttackKnowledge.fromShots(history.toList())),
        )
    }

    @Test fun `several partially damaged touching ships remain targetable after each sinking`() {
        val touchingFleet = listOf(
            Ship(ShipType.AIRCRAFT_CARRIER, (1..5).map { Coordinate(2, it) }),
            Ship(ShipType.BATTLESHIP, (1..4).map { Coordinate(3, it) }),
            Ship(ShipType.CRUISER, (1..3).map { Coordinate(4, it) }),
            Ship(ShipType.SUBMARINE, (1..3).map { Coordinate(5, it) }),
            Ship(ShipType.DESTROYER, (1..2).map { Coordinate(6, it) }),
        )
        var state = GameEngine.newGame(40).copy(computerBoard = Board(touchingFleet))
        val passiveTargets = (0..9).flatMap { row -> (0..9).map { column -> Coordinate(row, column) } }
            .filter { cell -> state.playerBoard.ships.none { cell in it.cells } }.iterator()
        fun fireAndReply(cell: Coordinate) {
            val result = GameEngine.attack(state, Player.HUMAN, cell)
            assertTrue(result.accepted)
            state = result.state
            if (state.phase != GamePhase.GAME_OVER) {
                assertTrue("Target strategy abandoned damaged ships", passiveTargets.hasNext())
                state = GameEngine.attack(state, Player.COMPUTER, passiveTargets.next()).state
            }
        }
        listOf(Coordinate(2, 1), Coordinate(2, 2), Coordinate(3, 1), Coordinate(4, 1),
            Coordinate(5, 1), Coordinate(6, 1)).forEach { fireAndReply(it) }
        val strategy = HuntTargetStrategy(32)
        while (state.phase != GamePhase.GAME_OVER) {
            fireAndReply(strategy.chooseShot(AttackKnowledge.fromShots(state.computerBoard.shots)))
        }
        assertEquals(Player.HUMAN, state.winner)
        assertEquals(5, state.computerBoard.shots.count { it.outcome == ShotOutcome.SUNK })
        assertTrue(GameEngine.isValid(state))
    }

    @Test fun `one thousand seeded complete games never repeat or leave the board`() {
        repeat(1_000) { seed ->
            var state = GameEngine.newGame(seed.toLong())
            val human = HuntTargetStrategy(seed.toLong() xor 0x5A5A)
            val computer = HuntTargetStrategy(seed.toLong() xor 0xA5A5)
            var turns = 0
            while (state.phase != GamePhase.GAME_OVER && turns < 200) {
                val attacker = if (state.phase == GamePhase.PLAYER_TURN) Player.HUMAN else Player.COMPUTER
                val board = if (attacker == Player.HUMAN) state.computerBoard else state.playerBoard
                val opponent = if (attacker == Player.HUMAN) human else computer
                val target = opponent.chooseShot(AttackKnowledge.fromShots(board.shots))
                assertTrue("Out of bounds in seed $seed", target.isValid)
                assertFalse("Repeated shot in seed $seed", board.shots.any { it.coordinate == target })
                val result = GameEngine.attack(state, attacker, target)
                assertTrue("Rejected AI shot in seed $seed", result.accepted)
                state = result.state
                turns++
            }
            assertEquals("No completion in seed $seed", GamePhase.GAME_OVER, state.phase)
            assertNotNull(state.winner)
            assertTrue("Invalid final state in seed $seed", GameEngine.isValid(state))
            assertTrue(turns <= 199)
        }
    }
}
