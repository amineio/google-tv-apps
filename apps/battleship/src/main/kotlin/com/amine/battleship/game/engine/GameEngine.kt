package com.amine.battleship.game.engine

import com.amine.battleship.game.model.*
import kotlin.random.Random

object GameEngine {
    fun newGame(seed: Long): GameState {
        val random = Random(seed)
        return GameState(
            playerBoard = Board(ShipPlacement.placeFleet(random)),
            computerBoard = Board(ShipPlacement.placeFleet(random)),
            seed = seed,
        )
    }

    fun attack(state: GameState, attacker: Player, coordinate: Coordinate): AttackResult {
        fun reject(reason: AttackRejection) = AttackResult(state, rejection = reason)
        if (state.phase != GamePhase.PLAYER_TURN && state.phase != GamePhase.COMPUTER_TURN) {
            return reject(AttackRejection.MATCH_NOT_ACTIVE)
        }
        val activePlayer = if (state.phase == GamePhase.PLAYER_TURN) Player.HUMAN else Player.COMPUTER
        if (attacker != activePlayer) return reject(AttackRejection.NOT_YOUR_TURN)
        if (!coordinate.isValid) return reject(AttackRejection.OUT_OF_BOUNDS)
        val target = if (attacker == Player.HUMAN) state.computerBoard else state.playerBoard
        if (target.shots.any { it.coordinate == coordinate }) return reject(AttackRejection.ALREADY_ATTACKED)

        val shot = resolveShot(target.ships, target.shots.map { it.coordinate }.toSet(), coordinate)
        val nextBoard = target.copy(shots = target.shots + shot)
        val winner = if (nextBoard.ships.all { nextBoard.shipSunk(it) }) attacker else null
        val nextPhase = when {
            winner != null -> GamePhase.GAME_OVER
            attacker == Player.HUMAN -> GamePhase.COMPUTER_TURN
            else -> GamePhase.PLAYER_TURN
        }
        val next = state.copy(
            playerBoard = if (attacker == Player.COMPUTER) nextBoard else state.playerBoard,
            computerBoard = if (attacker == Player.HUMAN) nextBoard else state.computerBoard,
            phase = nextPhase,
            winner = winner,
            lastAttack = AttackEvent(attacker, shot),
        )
        return AttackResult(next, shot)
    }

    fun playerView(state: GameState, viewer: Player = Player.HUMAN): PlayerView {
        val own = if (viewer == Player.HUMAN) state.playerBoard else state.computerBoard
        val opponent = if (viewer == Player.HUMAN) state.computerBoard else state.playerBoard
        return PlayerView(
            ownBoard = own,
            opponentBoard = VisibleBoard(opponent.ships.filter { opponent.shipSunk(it) }, opponent.shots),
            phase = state.phase,
            winner = state.winner,
            lastAttack = state.lastAttack,
        )
    }

    /** Rejects malformed saves, including shot outcomes that contradict the saved fleets. */
    fun isValid(state: GameState): Boolean {
        if (!validBoard(state.playerBoard) || !validBoard(state.computerBoard)) return false
        val humanShots = state.computerBoard.shots.size
        val computerShots = state.playerBoard.shots.size
        val humanLost = state.playerBoard.ships.all { state.playerBoard.shipSunk(it) }
        val computerLost = state.computerBoard.ships.all { state.computerBoard.shipSunk(it) }
        val validPhase = when (state.phase) {
            GamePhase.PLAYER_TURN -> state.winner == null && !humanLost && !computerLost && humanShots == computerShots
            GamePhase.COMPUTER_TURN -> state.winner == null && !humanLost && !computerLost && humanShots == computerShots + 1
            GamePhase.GAME_OVER -> when (state.winner) {
                Player.HUMAN -> computerLost && !humanLost && humanShots == computerShots + 1
                Player.COMPUTER -> humanLost && !computerLost && humanShots == computerShots
                null -> false
            }
            else -> false
        }
        if (!validPhase) return false
        val expectedLast = when {
            humanShots == 0 && computerShots == 0 -> null
            humanShots == computerShots -> state.playerBoard.shots.lastOrNull()?.let { AttackEvent(Player.COMPUTER, it) }
            else -> state.computerBoard.shots.lastOrNull()?.let { AttackEvent(Player.HUMAN, it) }
        }
        return state.lastAttack == expectedLast
    }

    private fun validBoard(board: Board): Boolean {
        if (board.ships.size != 5 || board.ships.map { it.type }.toSet() != ShipType.entries.toSet()) return false
        val occupied = mutableSetOf<Coordinate>()
        for (ship in board.ships) {
            if (ship.cells.size != ship.type.length || ship.cells.any { !it.isValid || !occupied.add(it) }) return false
            val rows = ship.cells.map { it.row }.distinct().sorted()
            val columns = ship.cells.map { it.column }.distinct().sorted()
            val line = when {
                rows.size == 1 -> columns
                columns.size == 1 -> rows
                else -> return false
            }
            if (line.size != ship.type.length || line.last() - line.first() + 1 != ship.type.length) return false
        }
        val previous = mutableSetOf<Coordinate>()
        for (shot in board.shots) {
            if (!shot.coordinate.isValid || shot.coordinate in previous) return false
            if (shot != resolveShot(board.ships, previous, shot.coordinate)) return false
            // No saved history can continue after this board loses its final ship.
            if (board.ships.all { ship -> ship.cells.all { it in previous } }) return false
            previous.add(shot.coordinate)
        }
        return true
    }

    private fun resolveShot(ships: List<Ship>, previous: Set<Coordinate>, coordinate: Coordinate): Shot {
        val ship = ships.firstOrNull { coordinate in it.cells } ?: return Shot(coordinate, ShotOutcome.MISS)
        return if (ship.cells.all { it == coordinate || it in previous }) {
            Shot(coordinate, ShotOutcome.SUNK, ship.type, ship.cells)
        } else {
            Shot(coordinate, ShotOutcome.HIT)
        }
    }
}
