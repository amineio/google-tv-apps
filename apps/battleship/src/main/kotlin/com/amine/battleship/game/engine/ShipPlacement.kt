package com.amine.battleship.game.engine

import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.Coordinate.Companion.BOARD_SIZE
import com.amine.battleship.game.model.Ship
import com.amine.battleship.game.model.ShipType
import kotlin.random.Random

object ShipPlacement {
    fun placeFleet(random: Random): List<Ship> {
        val occupied = mutableSetOf<Coordinate>()
        return ShipType.entries.map { type ->
            val candidates = buildList {
                for (row in 0 until BOARD_SIZE) {
                    for (column in 0 until BOARD_SIZE) {
                        if (column + type.length <= BOARD_SIZE) {
                            add(List(type.length) { Coordinate(row, column + it) })
                        }
                        if (row + type.length <= BOARD_SIZE) {
                            add(List(type.length) { Coordinate(row + it, column) })
                        }
                    }
                }
            }.filter { cells -> cells.none { it in occupied } }
            val cells = candidates[random.nextInt(candidates.size)]
            occupied.addAll(cells)
            Ship(type, cells)
        }
    }
}
