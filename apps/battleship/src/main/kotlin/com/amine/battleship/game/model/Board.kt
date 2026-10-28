package com.amine.battleship.game.model

import kotlinx.serialization.Serializable

@Serializable
enum class ShotOutcome { MISS, HIT, SUNK }

@Serializable
data class Shot(
    val coordinate: Coordinate,
    val outcome: ShotOutcome,
    val shipType: ShipType? = null,
    val sunkCells: List<Coordinate> = emptyList(),
)

/** Shots are incoming attacks on this board, in the order that they occurred. */
@Serializable
data class Board(val ships: List<Ship>, val shots: List<Shot> = emptyList()) {
    val totalHits: Int get() = shots.count { it.outcome != ShotOutcome.MISS }

    fun shipSunk(ship: Ship): Boolean = ship.cells.all { cell ->
        shots.any { it.coordinate == cell && it.outcome != ShotOutcome.MISS }
    }
}

/** Only destroyed ships are included in an opponent board view. */
@Serializable
data class VisibleBoard(val ships: List<Ship>, val shots: List<Shot>)
