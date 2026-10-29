package com.amine.battleship.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.amine.battleship.localization.*
import com.amine.battleship.R
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import com.amine.battleship.viewmodel.ImpactBoard
import com.amine.battleship.viewmodel.GameAction
import com.amine.battleship.viewmodel.SessionState

@Composable
internal fun GameScreen(state: SessionState, onAction: (GameAction) -> Unit) {
    val frameTime = rememberBoardFrameTime(state, onAction)
    val game = state.game ?: return
    val view = GameEngine.playerView(game)
    val boardFocus = remember { FocusRequester() }
    LaunchedEffect(state.paused, state.confirmation) {
        if (!state.paused && state.confirmation == null) { withFrameNanos { }; boardFocus.requestFocus() }
    }
    val playerTurn = game.phase == GamePhase.PLAYER_TURN && !state.resolving
    val turnLabel = when {
        game.phase == GamePhase.GAME_OVER -> if (game.winner == Player.HUMAN) gameText(R.string.victory) else gameText(R.string.defeat)
        state.resolving && game.phase == GamePhase.PLAYER_TURN -> gameText(R.string.shot_resolving)
        playerTurn -> gameText(R.string.your_turn)
        else -> gameText(R.string.computer_turn)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columnWidth = (maxWidth - 24.dp) / 2
        val boardSize = minOf(columnWidth, maxHeight - 126.dp)
        val chosenShot = view.opponentBoard.shots.firstOrNull { it.coordinate == state.selected }
        val targetState = when {
            state.message == "Already targeted" -> gameText(R.string.already_targeted)
            chosenShot?.coordinate in view.opponentBoard.shots.flatMap { it.sunkCells } -> gameText(R.string.sunk_ship)
            chosenShot?.outcome == ShotOutcome.MISS -> gameText(R.string.miss)
            chosenShot != null -> gameText(R.string.hit)
            else -> gameText(R.string.unexplored)
        }
        val targetDescription = gameText(R.string.target_description, state.selected.label, targetState)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.captain_portrait), gameText(R.string.captain),
                    Modifier.size(40.dp).clip(CircleShape).testTag("captain_portrait"),
                    contentScale = ContentScale.Crop)
                Spacer(Modifier.width(12.dp))
                val humanActive = if (game.phase == GamePhase.GAME_OVER) game.winner == Player.HUMAN else playerTurn
                Box(Modifier.width(190.dp), contentAlignment = Alignment.CenterStart) {
                    if (humanActive) TurnLabel(turnLabel, true)
                }
                Box(Modifier.weight(1f)) { LastShotFeedback(game.lastAttack) }
                Box(Modifier.width(190.dp), contentAlignment = Alignment.CenterEnd) {
                    if (!humanActive) TurnLabel(turnLabel, false)
                }
                Spacer(Modifier.width(12.dp))
                Image(painterResource(R.drawable.opponent_portrait), gameText(R.string.opponent),
                    Modifier.size(40.dp).clip(CircleShape).testTag("opponent_portrait"),
                    contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.width(columnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.width(boardSize).height(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel(gameText(R.string.enemy_waters))
                        Spacer(Modifier.width(12.dp))
                        Text(state.selected.label, color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("selected_target")
                                .semantics { contentDescription = targetDescription })
                        Spacer(Modifier.weight(1f))
                        Text(gameText(R.string.sunk_count, view.opponentBoard.ships.size), color = Muted, fontSize = 13.sp)
                    }
                    GameBoard(view.opponentBoard.ships, view.opponentBoard.shots, Modifier.size(boardSize),
                        selected = state.selected,
                        canFire = playerTurn && !state.paused && state.confirmation == null,
                        focusRequester = boardFocus, onAction = onAction, frameTime = frameTime,
                        impact = state.activeImpact?.takeIf { it.board == ImpactBoard.ENEMY })
                    Box(Modifier.width(boardSize).height(34.dp), contentAlignment = Alignment.CenterStart) {
                        if (state.message == "Already targeted") {
                            Text(gameText(R.string.already_targeted), color = Impact, fontSize = 13.sp, maxLines = 1)
                        }
                    }
                }
                Column(Modifier.width(columnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.width(boardSize).height(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel(gameText(R.string.your_fleet))
                        Spacer(Modifier.weight(1f))
                        Text(gameText(R.string.afloat_count, view.ownBoard.ships.count { !view.ownBoard.shipSunk(it) }), color = Muted, fontSize = 13.sp)
                    }
                    GameBoard(view.ownBoard.ships, view.ownBoard.shots, Modifier.size(boardSize),
                        frameTime = frameTime,
                        impact = state.activeImpact?.takeIf { it.board == ImpactBoard.FRIENDLY })
                    Row(Modifier.width(boardSize).height(44.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        view.ownBoard.ships.forEach { ship ->
                            Box(Modifier.weight(1f)) { FleetShip(ship.type, view.ownBoard.shots.count { it.coordinate in ship.cells }) }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(16.dp)) {
                state.saveWarning?.let { Text(localizedSaveWarning(it), color = Impact, fontSize = 13.sp, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun LastShotFeedback(event: AttackEvent?) {
    val result = when (event?.shot?.outcome) {
        ShotOutcome.MISS -> gameText(R.string.miss)
        ShotOutcome.HIT -> gameText(R.string.hit)
        ShotOutcome.SUNK -> gameText(R.string.ship_sunk, event.shot.shipType?.let { shortName(it) }.orEmpty())
        null -> gameText(R.string.ready)
    }
    val source = if (event?.attacker == Player.COMPUTER) gameText(R.string.incoming) else gameText(R.string.your_shot)
    val description = if (event == null) gameText(R.string.no_shots) else gameText(R.string.shot_description, source, result, event.shot.coordinate.label)
    Row(Modifier.fillMaxWidth().height(32.dp).testTag("last_shot_feedback")
        .semantics { contentDescription = description }, verticalAlignment = Alignment.CenterVertically) {
        val color = if (event?.shot?.outcome in listOf(ShotOutcome.HIT, ShotOutcome.SUNK)) Impact else Ink
        if (event != null) {
            Text(if (event.shot.outcome == ShotOutcome.MISS) "○" else "×",
                color = color, fontSize = 25.sp, modifier = Modifier.width(28.dp))
            Text(result, color = color, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
                maxLines = 1, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FleetShip(type: ShipType, damage: Int) {
    val sunk = damage == type.length
    Column {
        Text(gameText(when (type) {
            ShipType.AIRCRAFT_CARRIER -> R.string.roster_carrier
            ShipType.BATTLESHIP -> R.string.roster_battleship
            ShipType.CRUISER -> R.string.roster_cruiser
            ShipType.SUBMARINE -> R.string.roster_submarine
            ShipType.DESTROYER -> R.string.roster_destroyer
        }), color = if (sunk) Impact else Ink,
            fontSize = 13.sp, lineHeight = 16.sp, maxLines = 2, modifier = Modifier.height(34.dp))
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(type.length) { segment ->
                Canvas(Modifier.width(10.dp).height(5.dp)) {
                    drawRoundRect(if (segment < damage) Impact else Steel.copy(alpha = .8f),
                        cornerRadius = CornerRadius(1.dp.toPx()))
                    if (segment < damage) {
                        drawLine(Ocean, Offset(size.width * .3f, size.height * .85f),
                            Offset(size.width * .62f, size.height * .15f), 1.5.dp.toPx())
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Cyan, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 1.5.sp,
        fontWeight = FontWeight.SemiBold)
}

@Composable
private fun TurnLabel(label: String, human: Boolean) {
    Text(label, fontSize = 14.sp, color = if (human) Cyan else Ink,
        fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, maxLines = 1,
        modifier = Modifier.testTag(if (human) "human_turn_indicator" else "opponent_turn_indicator"))
}
