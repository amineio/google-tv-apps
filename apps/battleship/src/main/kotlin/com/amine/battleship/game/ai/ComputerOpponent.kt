package com.amine.battleship.game.ai

import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.Ship
import com.amine.battleship.game.model.ShipType
import com.amine.battleship.game.model.Shot
import com.amine.battleship.game.model.ShotOutcome

/** This is the complete information boundary for a computer opponent. */
data class AttackKnowledge(
    val shots: List<Shot>,
    val sunkShips: List<Ship>,
    val remainingShipLengths: List<Int>,
) {
    companion object {
        fun fromShots(shots: List<Shot>): AttackKnowledge {
            val sunk = shots.filter { it.outcome == ShotOutcome.SUNK }.mapNotNull { shot ->
                shot.shipType?.let { Ship(it, shot.sunkCells) }
            }
            val sunkTypes = sunk.map { it.type }.toSet()
            return AttackKnowledge(
                shots = shots.toList(),
                sunkShips = sunk,
                remainingShipLengths = ShipType.entries.filter { it !in sunkTypes }.map { it.length },
            )
        }
    }
}

fun interface ComputerOpponent {
    fun chooseShot(knowledge: AttackKnowledge): Coordinate
}
