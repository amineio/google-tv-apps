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
                when {
                    home -> listOf(Ocean.copy(alpha = .96f), Ocean.copy(alpha = .69f), Ocean.copy(alpha = .12f))
                    state.screen == Screen.GAME -> listOf(Ocean.copy(alpha = .84f), Ocean.copy(alpha = .72f))
                    else -> listOf(Ocean.copy(alpha = .97f), Ocean.copy(alpha = .92f))
                })))
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

internal fun shortName(type: ShipType): String = when (type) {
    ShipType.AIRCRAFT_CARRIER -> "Carrier"
    ShipType.BATTLESHIP -> "Battleship"
    ShipType.CRUISER -> "Cruiser"
    ShipType.SUBMARINE -> "Submarine"
    ShipType.DESTROYER -> "Destroyer"
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
        NavalButton("Sound: ${if (state.soundEnabled) "On" else "Off"}",
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
