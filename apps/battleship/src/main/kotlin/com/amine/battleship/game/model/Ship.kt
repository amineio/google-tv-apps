package com.amine.battleship.game.model

import kotlinx.serialization.Serializable

@Serializable
enum class ShipType(val label: String, val length: Int) {
    AIRCRAFT_CARRIER("Aircraft Carrier", 5),
    BATTLESHIP("Battleship", 4),
    CRUISER("Cruiser", 3),
    SUBMARINE("Submarine", 3),
    DESTROYER("Destroyer", 2),
}

@Serializable
data class Ship(val type: ShipType, val cells: List<Coordinate>)
