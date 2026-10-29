package com.amine.battleship.viewmodel

import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.GameState
import com.amine.battleship.game.model.ShotOutcome
import com.amine.battleship.game.model.ShipType

enum class ImpactBoard { ENEMY, FRIENDLY }

data class ImpactPresentation(
    val id: Long,
    val board: ImpactBoard,
    val coordinate: Coordinate,
    val outcome: ShotOutcome,
    val startedAtNanos: Long,
    val sunkCells: List<Coordinate> = emptyList(),
    val shipType: ShipType? = null,
)

enum class Screen { HOME, GAME, HELP, SETTINGS, RESULTS }
enum class Confirmation { RESTART, MAIN_MENU, NEW_GAME }

data class SessionState(
    val activeImpact: ImpactPresentation? = null,
    val loaded: Boolean = false,
    val screen: Screen = Screen.HOME,
    val game: GameState? = null,
    val selected: Coordinate = Coordinate(0, 0),
    val paused: Boolean = false,
    val confirmation: Confirmation? = null,
    val resolving: Boolean = false,
    val message: String = "",
    val soundEnabled: Boolean = true,
    val showHint: Boolean = true,
    val saveWarning: String? = null,
)

sealed interface GameAction {
    data object Play : GameAction
    data object Continue : GameAction
    data object Fire : GameAction
    data class CompleteImpact(val id: Long) : GameAction
    data class Move(val rowDelta: Int, val columnDelta: Int) : GameAction
    data object Pause : GameAction
    data object Resume : GameAction
    data object Restart : GameAction
    data object Home : GameAction
    data object Help : GameAction
    data object Settings : GameAction
    data object Back : GameAction
    data object Confirm : GameAction
    data object Cancel : GameAction
    data object ToggleSound : GameAction
}
