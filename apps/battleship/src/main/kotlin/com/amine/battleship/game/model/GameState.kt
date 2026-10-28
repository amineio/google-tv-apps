package com.amine.battleship.game.model

import kotlinx.serialization.Serializable

@Serializable
enum class Player {
    HUMAN, COMPUTER;

    fun other(): Player = if (this == HUMAN) COMPUTER else HUMAN
}

@Serializable
enum class GamePhase { MAIN_MENU, INITIALIZING, PLAYER_TURN, COMPUTER_TURN, GAME_OVER }

@Serializable
data class AttackEvent(val attacker: Player, val shot: Shot)

@Serializable
data class GameState(
    val playerBoard: Board,
    val computerBoard: Board,
    val phase: GamePhase = GamePhase.PLAYER_TURN,
    val winner: Player? = null,
    val lastAttack: AttackEvent? = null,
    val seed: Long = 0,
)

@Serializable
data class PlayerView(
    val ownBoard: Board,
    val opponentBoard: VisibleBoard,
    val phase: GamePhase,
    val winner: Player?,
    val lastAttack: AttackEvent?,
)

enum class AttackRejection { OUT_OF_BOUNDS, NOT_YOUR_TURN, ALREADY_ATTACKED, MATCH_NOT_ACTIVE }

data class AttackResult(
    val state: GameState,
    val shot: Shot? = null,
    val rejection: AttackRejection? = null,
) {
    val accepted: Boolean get() = shot != null && rejection == null
}
