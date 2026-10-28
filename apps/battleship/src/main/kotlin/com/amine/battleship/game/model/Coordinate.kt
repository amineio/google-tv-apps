package com.amine.battleship.game.model

import kotlinx.serialization.Serializable

@Serializable
data class Coordinate(val row: Int, val column: Int) {
    val isValid: Boolean get() = row in 0 until BOARD_SIZE && column in 0 until BOARD_SIZE
    val label: String get() = "${'A' + column}${row + 1}"

    companion object {
        const val BOARD_SIZE = 10
    }
}
