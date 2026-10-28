package com.amine.battleship.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.amine.battleship.R
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.GamePhase
import com.amine.battleship.game.model.Player
import com.amine.battleship.game.model.ShipType
import com.amine.battleship.game.model.ShotOutcome
import com.amine.battleship.viewmodel.Confirmation
import com.amine.battleship.viewmodel.GameAction
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState

@Composable
fun BattleshipApp(state: SessionState, onAction: (GameAction) -> Unit, onExit: () -> Unit) {
    NavalTheme {
        BackHandler {
            if (state.screen == Screen.HOME) onExit() else onAction(GameAction.Back)
        }
        Box(Modifier.fillMaxSize().background(Ocean)) {
            Image(painterResource(R.drawable.naval_background), null, Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop)
            val home = state.screen == Screen.HOME
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                if (home) listOf(Ocean.copy(alpha = .96f), Ocean.copy(alpha = .69f), Ocean.copy(alpha = .12f))
                else listOf(Ocean.copy(alpha = .97f), Ocean.copy(alpha = .92f)))))
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().padding(horizontal = maxWidth * .05f, vertical = maxHeight * .05f)) {
                    when {
                        !state.loaded -> LoadingScreen()
                        state.screen == Screen.HOME -> HomeScreen(state, onAction, onExit)
                        state.screen == Screen.GAME -> GameScreen(state, onAction)
                        state.screen == Screen.HELP -> HelpScreen(onAction)
                        state.screen == Screen.SETTINGS -> SettingsScreen(state, onAction)
                        state.screen == Screen.RESULTS -> ResultsScreen(state, onAction)
                    }
                }
            }
            if (state.screen == Screen.GAME && state.paused && state.confirmation == null) {
                PauseScreen(onAction)
            }
            if (state.confirmation != null) ConfirmationScreen(state.confirmation, onAction)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Preparing the fleet…", color = Cyan, fontSize = 25.sp)
    }
}

@Composable
private fun HomeScreen(state: SessionState, onAction: (GameAction) -> Unit, onExit: () -> Unit) {
    val first = remember { FocusRequester() }
    val unfinished = state.game?.phase in listOf(GamePhase.PLAYER_TURN, GamePhase.COMPUTER_TURN)
    LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Eyebrow("OFFLINE · SINGLE PLAYER")
        Spacer(Modifier.height(8.dp))
        Text("BATTLESHIP", fontSize = 58.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp,
            lineHeight = 64.sp, maxLines = 1)
        Text("A calm sea. A hidden fleet. Your move.", color = Muted, fontSize = 19.sp)
        Spacer(Modifier.height(23.dp))
        Column(Modifier.width(235.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (unfinished) NavalButton("Continue Game", Modifier.fillMaxWidth().focusRequester(first), primary = true) {
                onAction(GameAction.Continue)
            }
            NavalButton("Play Game", Modifier.fillMaxWidth().then(if (!unfinished) Modifier.focusRequester(first) else Modifier),
                primary = !unfinished) { onAction(GameAction.Play) }
            NavalButton("How to Play", Modifier.fillMaxWidth()) { onAction(GameAction.Help) }
            NavalButton("Settings", Modifier.fillMaxWidth()) { onAction(GameAction.Settings) }
            NavalButton("Exit", Modifier.fillMaxWidth(), onClick = onExit)
        }
        state.saveWarning?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, fontSize = 14.sp, color = Impact, modifier = Modifier.widthIn(max = 520.dp))
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        Text("NORMAL OPPONENT   /   NO INTERNET NEEDED", color = Muted, fontSize = 12.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun GameScreen(state: SessionState, onAction: (GameAction) -> Unit) {
    val game = state.game ?: return
    val view = GameEngine.playerView(game)
    val boardFocus = remember { FocusRequester() }
    LaunchedEffect(state.paused, state.confirmation) {
        if (!state.paused && state.confirmation == null) { withFrameNanos { }; boardFocus.requestFocus() }
    }
    val playerTurn = game.phase == GamePhase.PLAYER_TURN && !state.resolving
    val playerShots = view.opponentBoard.shots
    val enemyLast = playerShots.lastOrNull()
    val friendlyLast = view.ownBoard.shots.lastOrNull()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("BATTLESHIP", fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            Box(Modifier.background(if (playerTurn) Cyan.copy(alpha = .12f) else Ink.copy(alpha = .07f),
                RoundedCornerShape(20.dp)).padding(horizontal = 15.dp, vertical = 6.dp)) {
                Text(if (state.resolving && game.phase == GamePhase.PLAYER_TURN) "SHOT RESOLVING"
                    else if (playerTurn) "YOUR TURN" else "COMPUTER TURN", fontSize = 14.sp,
                    color = if (playerTurn) Cyan else Muted, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            }
            Spacer(Modifier.width(18.dp))
            Text("BACK  Pause", fontSize = 14.sp, color = Muted)
        }
        Spacer(Modifier.height(13.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val boardSize = minOf(maxHeight - 31.dp, maxWidth * .56f)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(30.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(boardSize)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow("ENEMY WATERS")
                        Spacer(Modifier.weight(1f))
                        Text("${view.opponentBoard.ships.size} / 5 SUNK", fontSize = 12.sp, color = Muted)
                    }
                    Spacer(Modifier.height(6.dp))
                    GameBoard(ships = view.opponentBoard.ships, shots = playerShots,
                        modifier = Modifier.size(boardSize), selected = state.selected, lastShot = enemyLast,
                        canFire = playerTurn && !state.paused && state.confirmation == null,
                        focusRequester = boardFocus, onAction = onAction)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Eyebrow("TARGET")
                            Text(state.selected.label, color = Cyan, fontSize = 36.sp,
                                lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.width(23.dp))
                        Column {
                            Text(if (playerTurn) "Ready to fire" else "Stand by, captain", fontSize = 20.sp,
                                fontWeight = FontWeight.Medium)
                            Text(if (playerTurn) "Press OK to attack" else "The computer is taking its turn",
                                color = Muted, fontSize = 14.sp)
                        }
                    }
                    Box(Modifier.fillMaxWidth().heightIn(min = 47.dp)
                        .background(DeepWater.copy(alpha = .72f), RoundedCornerShape(7.dp))
                        .border(1.dp, Muted.copy(alpha = .16f), RoundedCornerShape(7.dp))
                        .padding(horizontal = 13.dp, vertical = 9.dp)) {
                        Text(state.message, fontSize = 18.sp, color = feedbackColor(game.lastAttack?.shot?.outcome),
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Eyebrow("YOUR FLEET")
                            Spacer(Modifier.height(5.dp))
                            GameBoard(ships = view.ownBoard.ships, shots = view.ownBoard.shots,
                                modifier = Modifier.fillMaxWidth(), lastShot = friendlyLast)
                        }
                        Column(Modifier.weight(.88f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            view.ownBoard.ships.forEach { ship ->
                                val damage = view.ownBoard.shots.count { it.coordinate in ship.cells }
                                FleetShip(ship.type, damage)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(17.dp)) {
                        Text("×  HIT", color = Impact, fontSize = 12.sp, letterSpacing = .6.sp)
                        Text("○  MISS", color = Ink, fontSize = 12.sp, letterSpacing = .6.sp)
                        Text("◎  LAST SHOT", color = Muted, fontSize = 12.sp, letterSpacing = .6.sp)
                    }
                    state.saveWarning?.let { Text(it, color = Impact, fontSize = 12.sp, maxLines = 2) }
                }
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(if (state.showHint) "Use arrows to aim. Press OK to fire. Press Back to pause."
            else "ARROWS  Aim     OK  Fire     BACK  Pause", color = Muted, fontSize = if (state.showHint) 15.sp else 13.sp)
    }
}

@Composable
private fun FleetShip(type: ShipType, damage: Int) {
    Column {
        Text(if (damage == type.length) "${shortName(type)} · SUNK" else shortName(type),
            color = if (damage == type.length) Impact else Muted, fontSize = 12.sp, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(type.length) { segment ->
                Box(Modifier.width(13.dp).height(5.dp).background(
                    if (segment < damage) Impact else Steel.copy(alpha = .65f), RoundedCornerShape(1.dp)))
            }
        }
    }
}

private fun shortName(type: ShipType): String = when (type) {
    ShipType.AIRCRAFT_CARRIER -> "Carrier"
    ShipType.BATTLESHIP -> "Battleship"
    ShipType.CRUISER -> "Cruiser"
    ShipType.SUBMARINE -> "Submarine"
    ShipType.DESTROYER -> "Destroyer"
}

private fun feedbackColor(outcome: ShotOutcome?): Color = when (outcome) {
    ShotOutcome.HIT, ShotOutcome.SUNK -> Impact
    else -> Ink
}

@Composable
private fun HelpScreen(onAction: (GameAction) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Eyebrow("CAPTAIN’S BRIEFING")
        Text("HOW TO PLAY", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(19.dp))
        Text("Find and sink the computer’s five hidden ships.", fontSize = 22.sp)
        Spacer(Modifier.height(12.dp))
        Text("Use arrows to aim. Press OK to fire. Press Back to pause.", fontSize = 20.sp, color = Cyan)
        Spacer(Modifier.height(13.dp))
        Text("You fire first. Each side fires once per turn, even after a hit.\nA cross marks a hit. A ring marks a miss. A sunk ship becomes visible.\nShips can touch. The same cell cannot be attacked twice.",
            fontSize = 18.sp, lineHeight = 28.sp, color = Muted)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            ShipType.entries.forEach { type ->
                Column {
                    Text(shortName(type), fontSize = 14.sp, color = Ink)
                    Text("${type.length} cells", fontSize = 14.sp, color = Muted)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        NavalButton("Back", Modifier.width(160.dp).focusRequester(first)) { onAction(GameAction.Back) }
    }
}

@Composable
private fun SettingsScreen(state: SessionState, onAction: (GameAction) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Eyebrow("MAKE YOURSELF AT HOME")
        Text("SETTINGS", fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        NavalButton("Sound effects: ${if (state.soundEnabled) "On" else "Off"}",
            Modifier.width(280.dp).focusRequester(first), primary = true) { onAction(GameAction.ToggleSound) }
        Spacer(Modifier.height(16.dp))
        Text("Difficulty: Normal", fontSize = 22.sp)
        Text("A fair computer opponent that learns from its own shots.", color = Muted, fontSize = 17.sp)
        Spacer(Modifier.height(26.dp))
        NavalButton("Back", Modifier.width(160.dp)) { onAction(GameAction.Back) }
    }
}

@Composable
private fun PauseScreen(onAction: (GameAction) -> Unit) {
    Modal(onDismiss = { onAction(GameAction.Resume) }) {
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
        Eyebrow("ALL HANDS AT EASE")
        Text("GAME PAUSED", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Your fleet will wait for you.", color = Muted, fontSize = 16.sp)
        Spacer(Modifier.height(19.dp))
        Column(Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            NavalButton("Resume", Modifier.fillMaxWidth().focusRequester(first), primary = true) { onAction(GameAction.Resume) }
            NavalButton("Restart Game", Modifier.fillMaxWidth()) { onAction(GameAction.Restart) }
            NavalButton("Settings", Modifier.fillMaxWidth()) { onAction(GameAction.Settings) }
            NavalButton("Return to Main Menu", Modifier.fillMaxWidth()) { onAction(GameAction.Home) }
        }
    }
}

@Composable
private fun ConfirmationScreen(confirmation: Confirmation, onAction: (GameAction) -> Unit) {
    Modal(onDismiss = { onAction(GameAction.Cancel) }) {
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
        val home = confirmation == Confirmation.MAIN_MENU
        Eyebrow("YOUR COMMAND")
        Text(if (home) "Return to main menu?" else "Start a new game?", fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text(if (home) "Your match stays saved. Select Continue Game to return."
            else "This replaces your current match with a new fleet.",
            fontSize = 17.sp, color = Muted, modifier = Modifier.widthIn(max = 430.dp))
        Spacer(Modifier.height(23.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            NavalButton("Keep Playing", Modifier.focusRequester(first), primary = true) { onAction(GameAction.Cancel) }
            NavalButton(if (home) "Main Menu" else "New Game") { onAction(GameAction.Confirm) }
        }
    }
}

@Composable
private fun ResultsScreen(state: SessionState, onAction: (GameAction) -> Unit) {
    val game = state.game ?: return
    val won = game.winner == Player.HUMAN
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
    val shots = game.computerBoard.shots
    val hits = shots.count { it.outcome != ShotOutcome.MISS }
    val sunk = shots.count { it.outcome == ShotOutcome.SUNK }
    val accuracy = if (shots.isEmpty()) 0 else (hits * 100f / shots.size).toInt()
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(if (won) "ENEMY FLEET DEFEATED" else "A BRAVE EFFORT, CAPTAIN")
        Spacer(Modifier.height(7.dp))
        Text(if (won) "VICTORY" else "DEFEAT", fontSize = 64.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp, color = if (won) Cyan else Ink)
        Text(if (won) "Well played. The waters are yours." else "A new fleet means a fresh start.", color = Muted, fontSize = 21.sp)
        Spacer(Modifier.height(31.dp))
        Row(Modifier.fillMaxWidth(.85f).background(DeepWater.copy(alpha = .68f), RoundedCornerShape(10.dp))
            .padding(vertical = 22.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ResultStat("Shots fired", shots.size.toString())
            ResultStat("Successful hits", hits.toString())
            ResultStat("Accuracy", "$accuracy%")
            ResultStat("Ships sunk", "$sunk / 5")
        }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            NavalButton("Play Again", Modifier.width(190.dp).focusRequester(first), primary = true) { onAction(GameAction.Play) }
            NavalButton("Main Menu", Modifier.width(190.dp)) { onAction(GameAction.Home) }
        }
    }
}

@Composable
private fun ResultStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Cyan, fontSize = 33.sp, fontWeight = FontWeight.SemiBold)
        Text(label, color = Muted, fontSize = 15.sp)
    }
}

@Composable
private fun Modal(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Ocean.copy(alpha = .88f)), contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 560.dp).background(DeepWater, RoundedCornerShape(12.dp))
                .border(1.dp, Cyan.copy(alpha = .36f), RoundedCornerShape(12.dp)).padding(32.dp), content = content)
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(text, color = Cyan, fontSize = 12.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
}
