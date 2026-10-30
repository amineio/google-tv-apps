package com.amine.battleship.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.amine.battleship.R
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import com.amine.battleship.localization.gameText
import com.amine.battleship.viewmodel.*
import kotlin.math.ceil

@Composable
internal fun LocalGameplayScreen(state: SessionState, onAction: (GameAction) -> Unit, results: Boolean = false) {
    val game = state.game ?: return
    val frame = rememberBoardFrameTime(state, onAction)
    val focus = remember { FocusRequester() }
    val active = if (game.phase == GamePhase.COMPUTER_TURN) Player.COMPUTER else Player.HUMAN
    val activeProfile = if (active == Player.HUMAN) game.leftProfile else game.rightProfile
    val revealing = state.revealRemainingMs != null && !state.paused && state.confirmation == null
    val showComplete = revealing || results
    val aiming = !results && !revealing && !state.paused && state.readySeat == null
    val canFire = aiming && !state.resolving && !state.releaseRequired
    LaunchedEffect(state.paused, state.readySeat, state.revealRemainingMs == null, results, active) {
        if (aiming && state.confirmation == null) { withFrameNanos { }; focus.requestFocus() }
    }
    val resultFocus = remember { FocusRequester() }
    LaunchedEffect(results) { if (results) { withFrameNanos { }; resultFocus.requestFocus() } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = (maxWidth - 24.dp) / 2
        val boardSize = minOf(width, maxHeight - if (results) 154.dp else 142.dp)
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val winner = if (game.winner == Player.COMPUTER) game.rightProfile else game.leftProfile
            Row(Modifier.fillMaxWidth().height(34.dp), verticalAlignment = Alignment.CenterVertically) {
                if (results) CaptainPortrait(winner, Modifier.size(32.dp))
                Text(when {
                    results -> gameText(R.string.local_winner, profileName(winner))
                    revealing -> gameText(R.string.remember_fleets, ceil(state.revealRemainingMs / 1000.0).toInt())
                    state.resolving -> gameText(R.string.local_resolving)
                    else -> gameText(R.string.local_turn, profileName(activeProfile))
                }, fontSize = 23.sp, color = Cyan, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).testTag("local_turn"))
                if (!revealing && !results) {
                    val event = game.lastAttack
                    val outcome = when (event?.shot?.outcome) {
                        ShotOutcome.MISS -> gameText(R.string.miss)
                        ShotOutcome.HIT -> gameText(R.string.hit)
                        ShotOutcome.SUNK -> gameText(R.string.ship_sunk, event.shot.shipType?.let { shortName(it) }.orEmpty())
                        null -> ""
                    }
                    Text(outcome, color = Ink, fontSize = 16.sp)
                }
            }
            if (revealing) {
                Box(Modifier.fillMaxWidth().height(5.dp).background(Steel)) {
                    Box(Modifier.fillMaxWidth((state.revealRemainingMs / 10_000f).coerceIn(0f, 1f)).fillMaxHeight().background(Cyan).testTag("preview_progress"))
                }
            } else Spacer(Modifier.height(5.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf(Player.HUMAN, Player.COMPUTER).forEach { owner ->
                    val profile = if (owner == Player.HUMAN) game.leftProfile else game.rightProfile
                    // After reveal, no intact ships enter the board or any fleet roster.
                    val board = if (owner == Player.HUMAN) game.playerBoard else game.computerBoard
                    val projection = if (showComplete) VisibleBoard(board.ships, board.shots) else GameEngine.publicBoard(board)
                    val targeting = aiming && owner != active
                    val name = profileName(profile)
                    val fleet = gameText(R.string.fleet_name, name)
                    val selected = if (targeting) state.selected else null
                    val shot = projection.shots.firstOrNull { it.coordinate == selected }
                    val targetState = when (shot?.outcome) {
                        ShotOutcome.MISS -> gameText(R.string.miss)
                        ShotOutcome.HIT -> gameText(R.string.hit)
                        ShotOutcome.SUNK -> gameText(R.string.sunk_ship)
                        null -> gameText(R.string.unexplored)
                    }
                    val description = if (targeting) gameText(R.string.local_board_target, profileName(activeProfile), fleet, state.selected.label, targetState)
                        else if (showComplete) fleet else gameText(R.string.local_board_public, fleet, projection.shots.size)
                    Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.width(boardSize).height(30.dp), verticalAlignment = Alignment.CenterVertically) {
                            CaptainPortrait(profile, Modifier.size(28.dp)); Spacer(Modifier.width(7.dp))
                            Text(fleet, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).semantics { contentDescription = fleet })
                            if (targeting) Text(state.selected.label, fontSize = 16.sp, color = Cyan, modifier = Modifier.testTag("selected_target"))
                        }
                        GameBoard(projection.ships, projection.shots, Modifier.size(boardSize),
                            selected = selected, canFire = targeting && canFire,
                            focusRequester = if (targeting) focus else null, onAction = onAction, frameTime = frame,
                            accessibleDescription = description, fixedTag = if (owner == Player.HUMAN) "left_fleet_board" else "right_fleet_board",
                            impact = state.activeImpact?.takeIf { it.board == if (owner == Player.HUMAN) ImpactBoard.LEFT else ImpactBoard.RIGHT })
                        Text(gameText(R.string.local_afloat, name, 5 - projection.shots.count { it.outcome == ShotOutcome.SUNK }),
                            color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(boardSize).height(21.dp))
                    }
                }
            }
            if (results) Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                NavalButton(gameText(R.string.local_rematch), Modifier.focusRequester(resultFocus), primary = true) { onAction(GameAction.Restart) }
                NavalButton(gameText(R.string.main_menu)) { onAction(GameAction.Home) }
                NavalButton(gameText(R.string.new_game)) { onAction(GameAction.ChooseMode) }
            } else if (state.saveWarning != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(gameText(R.string.save_failed), color = Impact, fontSize = 14.sp)
                    NavalButton(gameText(R.string.retry_save)) { onAction(GameAction.RetrySave) }
                }
            } else if (state.message == "Already targeted") Text(gameText(R.string.already_targeted), color = Impact, fontSize = 14.sp)
        }
    }
}
