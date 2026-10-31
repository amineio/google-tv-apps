package com.amine.battleship.viewmodel

import com.amine.battleship.localization.AppLanguage
import com.amine.battleship.game.model.Player
import com.amine.battleship.game.model.PlayerProfile
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.GameState
import com.amine.battleship.game.model.ShotOutcome
import com.amine.battleship.game.model.ShipType

enum class ImpactBoard { ENEMY, FRIENDLY, LEFT, RIGHT }

data class ImpactPresentation(
    val id: Long,
    val board: ImpactBoard,
    val coordinate: Coordinate,
    val outcome: ShotOutcome,
    val startedAtNanos: Long,
    val sunkCells: List<Coordinate> = emptyList(),
    val shipType: ShipType? = null,
)

enum class Screen { HOME, GAME, HELP, SETTINGS, RESULTS, MODE, SETUP }
enum class Confirmation { RESTART, MAIN_MENU, NEW_GAME }

data class LocalSetupDraft(
    val left: PlayerProfile = PlayerProfile("fox"),
    val right: PlayerProfile = PlayerProfile("owl"),
    val slot: Int = 0,
    val nameError: Boolean = false,
)

data class SessionState(
    val localStarting: Boolean = false,
    val setup: LocalSetupDraft? = null,
    val readyLeftProfile: PlayerProfile? = null,
    val readyRightProfile: PlayerProfile? = null,
    val readySeat: Player? = null,
    val revealRemainingMs: Long? = null,
    val previewInterrupted: Boolean = false,
    val releaseRequired: Boolean = false,
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
    val language: AppLanguage = AppLanguage.ENGLISH,
    val showHint: Boolean = true,
    val saveWarning: String? = null,
)

sealed interface GameAction {
    data object ChooseMode : GameAction
    data object LocalSetup : GameAction
    data class SelectPortrait(val id: String) : GameAction
    data class SaveName(val name: String) : GameAction
    data object NextParticipant : GameAction
    data object StartLocal : GameAction
    data object Ready : GameAction
    data object ReleaseConfirm : GameAction
    data object RetrySave : GameAction
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
    data object CycleLanguage : GameAction
    data object ToggleSound : GameAction
}
