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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import com.amine.battleship.localization.*
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
    ProvideGameLanguage(state.language) {
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
}

@Composable
private fun LoadingScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(gameText(R.string.loading), color = Cyan, fontSize = 25.sp)
    }
}

@Composable
private fun HomeScreen(state: SessionState, onAction: (GameAction) -> Unit, onExit: () -> Unit) {
    val first = remember { FocusRequester() }
    val unfinished = state.game?.phase in listOf(GamePhase.PLAYER_TURN, GamePhase.COMPUTER_TURN)
    LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(gameText(R.string.title), fontSize = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
            lineHeight = 64.sp, maxLines = 1)
        Spacer(Modifier.height(23.dp))
        Column(Modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            if (unfinished) NavalButton(gameText(R.string.continue_game), Modifier.fillMaxWidth().focusRequester(first), primary = true) {
                onAction(GameAction.Continue)
            }
            NavalButton(gameText(R.string.play_game), Modifier.fillMaxWidth().then(if (!unfinished) Modifier.focusRequester(first) else Modifier),
                primary = !unfinished) { onAction(GameAction.Play) }
            NavalButton(gameText(R.string.help), Modifier.fillMaxWidth()) { onAction(GameAction.Help) }
            NavalButton(gameText(R.string.settings), Modifier.fillMaxWidth()) { onAction(GameAction.Settings) }
            NavalButton(gameText(R.string.exit), Modifier.fillMaxWidth(), onClick = onExit)
        }
        state.saveWarning?.let {
            Spacer(Modifier.height(12.dp))
            Text(localizedSaveWarning(it), fontSize = 14.sp, color = Impact, modifier = Modifier.widthIn(max = 520.dp))
        }
    }

}

@Composable
internal fun shortName(type: ShipType): String = when (type) {
    ShipType.AIRCRAFT_CARRIER -> gameText(R.string.carrier)
    ShipType.BATTLESHIP -> gameText(R.string.battleship)
    ShipType.CRUISER -> gameText(R.string.cruiser)
    ShipType.SUBMARINE -> gameText(R.string.submarine)
    ShipType.DESTROYER -> gameText(R.string.destroyer)
}

@Composable
private fun SettingsScreen(state: SessionState, onAction: (GameAction) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(gameText(R.string.settings_title), fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        NavalButton(gameText(if (state.soundEnabled) R.string.sound_on else R.string.sound_off),
            Modifier.width(280.dp).focusRequester(first), primary = true) { onAction(GameAction.ToggleSound) }
        Spacer(Modifier.height(16.dp))
        NavalButton(gameText(R.string.language, state.language.nativeName),
            Modifier.width(360.dp).testTag("language_setting")) { onAction(GameAction.CycleLanguage) }
        Spacer(Modifier.height(26.dp))
        NavalButton(gameText(R.string.back), Modifier.width(160.dp)) { onAction(GameAction.Back) }
    }
}

@Composable
private fun PauseScreen(onAction: (GameAction) -> Unit) {
    Modal(onDismiss = { onAction(GameAction.Resume) }) {
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
        Text(gameText(R.string.paused), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(19.dp))
        Column(Modifier.width(310.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            NavalButton(gameText(R.string.resume), Modifier.fillMaxWidth().focusRequester(first), primary = true) { onAction(GameAction.Resume) }
            NavalButton(gameText(R.string.restart), Modifier.fillMaxWidth()) { onAction(GameAction.Restart) }
            NavalButton(gameText(R.string.settings), Modifier.fillMaxWidth()) { onAction(GameAction.Settings) }
            NavalButton(gameText(R.string.return_menu), Modifier.fillMaxWidth()) { onAction(GameAction.Home) }
        }
    }
}

@Composable
private fun ConfirmationScreen(confirmation: Confirmation, onAction: (GameAction) -> Unit) {
    Modal(onDismiss = { onAction(GameAction.Cancel) }) {
        val first = remember { FocusRequester() }
        LaunchedEffect(Unit) { withFrameNanos { }; first.requestFocus() }
        val home = confirmation == Confirmation.MAIN_MENU
        Text(if (home) gameText(R.string.return_question) else gameText(R.string.new_question), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Text(if (home) gameText(R.string.saved_explanation)
            else gameText(R.string.replace_explanation),
            fontSize = 17.sp, color = Muted, modifier = Modifier.widthIn(max = 430.dp))
        Spacer(Modifier.height(23.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            NavalButton(gameText(R.string.keep_playing), Modifier.focusRequester(first), primary = true) { onAction(GameAction.Cancel) }
            NavalButton(if (home) gameText(R.string.main_menu) else gameText(R.string.new_game)) { onAction(GameAction.Confirm) }
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
        Eyebrow(if (won) gameText(R.string.enemy_defeated) else gameText(R.string.brave_effort))
        Spacer(Modifier.height(7.dp))
        Text(if (won) gameText(R.string.victory) else gameText(R.string.defeat), fontSize = 64.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp, color = if (won) Cyan else Ink)
        Text(if (won) gameText(R.string.victory_message) else gameText(R.string.defeat_message), color = Muted, fontSize = 21.sp)
        Spacer(Modifier.height(31.dp))
        Row(Modifier.fillMaxWidth(.85f).background(DeepWater.copy(alpha = .68f), RoundedCornerShape(10.dp))
            .padding(vertical = 22.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ResultStat(gameText(R.string.shots_fired), shots.size.toString())
            ResultStat(gameText(R.string.successful_hits), hits.toString())
            ResultStat(gameText(R.string.accuracy), "$accuracy%")
            ResultStat(gameText(R.string.ships_sunk), "$sunk / 5")
        }
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            NavalButton(gameText(R.string.play_again), Modifier.width(220.dp).focusRequester(first), primary = true) { onAction(GameAction.Play) }
            NavalButton(gameText(R.string.main_menu), Modifier.width(220.dp)) { onAction(GameAction.Home) }
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
