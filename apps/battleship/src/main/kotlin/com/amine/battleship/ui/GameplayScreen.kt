package com.amine.battleship.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
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
        game.phase == GamePhase.GAME_OVER -> if (game.winner == Player.HUMAN) "VICTORY" else "DEFEAT"
        state.resolving && game.phase == GamePhase.PLAYER_TURN -> "SHOT RESOLVING"
        playerTurn -> "YOUR TURN"
        else -> "COMPUTER TURN"
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columnWidth = (maxWidth - 24.dp) / 2
        val boardSize = minOf(columnWidth, maxHeight - 142.dp)
        val chosenShot = view.opponentBoard.shots.firstOrNull { it.coordinate == state.selected }
        val targetState = when {
            state.message == "Already targeted" -> "Already targeted"
            chosenShot?.coordinate in view.opponentBoard.shots.flatMap { it.sunkCells } -> "Sunk ship"
            chosenShot?.outcome == ShotOutcome.MISS -> "Miss"
            chosenShot != null -> "Hit"
            else -> "Unexplored"
        }
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("BATTLESHIP", fontWeight = FontWeight.Bold, fontSize = 23.sp, letterSpacing = 2.sp)
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(190.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(turnLabel, fontSize = 14.sp, color = if (playerTurn) Cyan else Ink,
                        fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, maxLines = 1)
                }
                Spacer(Modifier.width(22.dp))
                Text("BACK  Pause", fontSize = 13.sp, color = Muted)
            }
            Spacer(Modifier.height(3.dp))
            Row(Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.width(columnWidth).fillMaxHeight().testTag("selected_target")
                    .semantics { contentDescription = "Target ${state.selected.label}. $targetState." },
                    verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("TARGET")
                    Spacer(Modifier.width(14.dp))
                    Text(state.selected.label, color = Cyan, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(49.dp))
                    Text(targetState, color = Muted, fontSize = 13.sp, maxLines = 1)
                }
                Box(Modifier.width(columnWidth)) { LastShotFeedback(game.lastAttack) }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.width(columnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.width(boardSize).height(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("ENEMY WATERS")
                        Spacer(Modifier.weight(1f))
                        Text("${view.opponentBoard.ships.size} / 5 SUNK", color = Muted, fontSize = 13.sp)
                    }
                    GameBoard(view.opponentBoard.ships, view.opponentBoard.shots, Modifier.size(boardSize),
                        selected = state.selected, lastShot = view.opponentBoard.shots.lastOrNull(),
                        canFire = playerTurn && !state.paused && state.confirmation == null,
                        focusRequester = boardFocus, onAction = onAction, frameTime = frameTime,
                        impact = state.activeImpact?.takeIf { it.board == ImpactBoard.ENEMY })
                    Box(Modifier.width(boardSize).height(34.dp), contentAlignment = Alignment.CenterStart) {
                        Text("ARROWS  Aim    OK  Fire    BACK  Pause", color = Muted, fontSize = 13.sp, maxLines = 1)
                    }
                }
                Column(Modifier.width(columnWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.width(boardSize).height(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("YOUR FLEET")
                        Spacer(Modifier.weight(1f))
                        Text("${view.ownBoard.ships.count { !view.ownBoard.shipSunk(it) }} / 5 AFLOAT", color = Muted, fontSize = 13.sp)
                    }
                    GameBoard(view.ownBoard.ships, view.ownBoard.shots, Modifier.size(boardSize),
                        lastShot = view.ownBoard.shots.lastOrNull(), frameTime = frameTime,
                        impact = state.activeImpact?.takeIf { it.board == ImpactBoard.FRIENDLY })
                    Row(Modifier.width(boardSize).height(34.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        view.ownBoard.ships.forEach { ship ->
                            Box(Modifier.weight(1f)) { FleetShip(ship.type, view.ownBoard.shots.count { it.coordinate in ship.cells }) }
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(16.dp)) {
                state.saveWarning?.let { Text(it, color = Impact, fontSize = 13.sp, maxLines = 1) }
            }
        }
    }
}

@Composable
private fun LastShotFeedback(event: AttackEvent?) {
    val result = when (event?.shot?.outcome) {
        ShotOutcome.MISS -> "Miss"
        ShotOutcome.HIT -> "Hit"
        ShotOutcome.SUNK -> "${event.shot.shipType?.let(::shortName)} sunk"
        null -> "Ready"
    }
    val source = if (event?.attacker == Player.COMPUTER) "Incoming" else "Your shot"
    val description = if (event == null) "No shots fired." else "$source: $result at ${event.shot.coordinate.label}."
    Row(Modifier.fillMaxWidth().height(32.dp).testTag("last_shot_feedback")
        .semantics { contentDescription = description }, verticalAlignment = Alignment.CenterVertically) {
        Text(if (event == null) "LAST SHOT" else source.uppercase(), color = Muted,
            fontSize = 13.sp, letterSpacing = .5.sp, modifier = Modifier.width(86.dp))
        val color = if (event?.shot?.outcome in listOf(ShotOutcome.HIT, ShotOutcome.SUNK)) Impact else Ink
        Text(if (event == null) "—" else if (event.shot.outcome == ShotOutcome.MISS) "○" else "×",
            color = color, fontSize = 25.sp, modifier = Modifier.width(28.dp))
        Text(result, color = color, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FleetShip(type: ShipType, damage: Int) {
    val sunk = damage == type.length
    Column {
        Text(shortName(type), color = if (sunk) Impact else Ink,
            fontSize = 13.sp, lineHeight = 17.sp, maxLines = 1)
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
