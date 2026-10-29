package com.amine.battleship.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
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
        game.phase == GamePhase.GAME_OVER -> if (game.winner == Player.HUMAN) "VICTORY" else "DEFEAT"
        state.resolving && game.phase == GamePhase.PLAYER_TURN -> "SHOT RESOLVING"
        playerTurn -> "YOUR TURN"
        else -> "COMPUTER TURN"
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("BATTLESHIP", fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            Row(Modifier.background(Cyan.copy(alpha = if (playerTurn) .13f else .05f), RoundedCornerShape(4.dp))
                .border(1.dp, Cyan.copy(alpha = if (playerTurn) .35f else .12f), RoundedCornerShape(4.dp))
                .padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).background(if (playerTurn) Cyan else Muted, RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(8.dp))
                Text(turnLabel, fontSize = 14.sp, color = if (playerTurn) Cyan else Ink,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.width(18.dp))
            Text("BACK  Pause", fontSize = 14.sp, color = Muted)
        }
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val boardSize = minOf(maxHeight - 27.dp, maxWidth * .51f)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(boardSize)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("ENEMY WATERS")
                        Spacer(Modifier.weight(1f))
                        Text("${view.opponentBoard.ships.size} / 5 SUNK", color = Muted, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(7.dp))
                    GameBoard(view.opponentBoard.ships, view.opponentBoard.shots, Modifier.size(boardSize),
                        selected = state.selected, lastShot = view.opponentBoard.shots.lastOrNull(),
                        canFire = playerTurn && !state.paused && state.confirmation == null,
                        focusRequester = boardFocus, onAction = onAction, frameTime = frameTime,
                        impact = state.activeImpact?.takeIf { it.board == ImpactBoard.ENEMY })
                }
                Column(Modifier.weight(1f).fillMaxHeight()
                    .background(Brush.linearGradient(listOf(Color(0xFF102637), Color(0xFF0A1A27))), RoundedCornerShape(9.dp))
                    .border(1.dp, Steel.copy(alpha = .25f), RoundedCornerShape(9.dp))
                    .padding(14.dp)) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).heightIn(min = 80.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        val chosenShot = view.opponentBoard.shots.firstOrNull { it.coordinate == state.selected }
                        val targetState = when {
                            state.message == "Already targeted" -> "Already targeted"
                            chosenShot?.coordinate in view.opponentBoard.shots.flatMap { it.sunkCells } -> "Sunk ship"
                            chosenShot?.outcome == ShotOutcome.MISS -> "Miss"
                            chosenShot != null -> "Hit"
                            else -> "Unexplored"
                        }
                        Column(Modifier.width(132.dp).padding(bottom = 6.dp).testTag("selected_target")
                            .semantics { contentDescription = "Target ${state.selected.label}. $targetState." }) {
                            SectionLabel("TARGET")
                            Text(state.selected.label, color = Cyan, fontSize = 38.sp, lineHeight = 40.sp,
                                fontWeight = FontWeight.SemiBold)
                            Text(targetState, fontSize = 13.sp, lineHeight = 17.sp, color = Muted)
                        }
                        Image(painterResource(R.drawable.command_battleship), null,
                            Modifier.weight(1f).height(80.dp), contentScale = ContentScale.Fit)
                    }
                    PanelRule()
                    LastShotFeedback(game.lastAttack)
                    PanelRule()
                    Spacer(Modifier.height(11.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("YOUR FLEET")
                        Spacer(Modifier.weight(1f))
                        Text("${view.ownBoard.ships.count { !view.ownBoard.shipSunk(it) }} / 5 AFLOAT", color = Muted, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                        val friendlySize = minOf(maxHeight, maxWidth * .59f)
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            GameBoard(view.ownBoard.ships, view.ownBoard.shots, Modifier.size(friendlySize),
                                lastShot = view.ownBoard.shots.lastOrNull(), frameTime = frameTime,
                                impact = state.activeImpact?.takeIf { it.board == ImpactBoard.FRIENDLY })
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                view.ownBoard.ships.forEach { ship ->
                                    FleetShip(ship.type, view.ownBoard.shots.count { it.coordinate in ship.cells })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                        Text("× HIT", color = Impact, fontSize = 13.sp)
                        Text("○ MISS", color = Ink, fontSize = 13.sp)
                        Text("⌜ LAST", color = Brass, fontSize = 13.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        state.saveWarning?.let { Text(it, color = Impact, fontSize = 13.sp, maxLines = 2) }
        Text(if (state.showHint) "Use arrows to aim. Press OK to fire. Press Back to pause."
            else "ARROWS  Aim     OK  Fire     BACK  Pause", color = Muted, fontSize = 13.sp)
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
    Row(Modifier.fillMaxWidth().heightIn(min = 43.dp).testTag("last_shot_feedback")
        .semantics { contentDescription = description }, verticalAlignment = Alignment.CenterVertically) {
        Text(if (event == null) "LAST SHOT" else source.uppercase(), color = Muted,
            fontSize = 13.sp, letterSpacing = .5.sp, modifier = Modifier.width(91.dp))
        val color = if (event?.shot?.outcome in listOf(ShotOutcome.HIT, ShotOutcome.SUNK)) Impact else Ink
        Text(if (event == null) "—" else if (event.shot.outcome == ShotOutcome.MISS) "○" else "×",
            color = color, fontSize = 25.sp, modifier = Modifier.width(28.dp))
        Text(result, color = color, fontSize = 18.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium,
            maxLines = 2, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FleetShip(type: ShipType, damage: Int) {
    val sunk = damage == type.length
    Column {
        Text(if (sunk) "${shortName(type)} · sunk" else shortName(type), color = if (sunk) Impact else Ink,
            fontSize = 13.sp, lineHeight = 17.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(type.length) { segment ->
                Canvas(Modifier.width(12.dp).height(6.dp)) {
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
private fun PanelRule() { Box(Modifier.fillMaxWidth().height(1.dp).background(Steel.copy(alpha = .2f))) }
