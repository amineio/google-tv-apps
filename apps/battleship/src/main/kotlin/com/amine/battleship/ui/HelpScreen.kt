package com.amine.battleship.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.amine.battleship.R
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.Ship
import com.amine.battleship.game.model.ShipType
import com.amine.battleship.localization.gameText
import com.amine.battleship.viewmodel.GameAction

@Composable
internal fun HelpScreen(onAction: (GameAction) -> Unit) {
    var tactics by rememberSaveable { mutableStateOf(false) }
    var returnToTactics by rememberSaveable { mutableStateOf(false) }
    val back = remember { FocusRequester() }
    val tacticsButton = remember { FocusRequester() }
    val closeTactics = { tactics = false; returnToTactics = true }
    BackHandler(enabled = tactics, onBack = closeTactics)
    if (tactics) {
        TacticsScreen(onBack = closeTactics)
        return
    }
    LaunchedEffect(returnToTactics) {
        withFrameNanos { }
        if (returnToTactics) tacticsButton.requestFocus() else back.requestFocus()
    }
    Column(Modifier.fillMaxSize().testTag("help_screen")) {
        Text(gameText(R.string.help_title), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(gameText(R.string.help_goal), fontSize = 18.sp, color = Muted)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HelpCard("aim", gameText(R.string.help_aim), gameText(R.string.help_aim_body), Modifier.weight(1f))
            HelpCard("fire", gameText(R.string.help_fire), gameText(R.string.help_fire_body), Modifier.weight(1f))
            HelpCard("sink", gameText(R.string.help_sink), gameText(R.string.help_sink_body), Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NavalButton(gameText(R.string.back), Modifier.width(160.dp).focusRequester(back).testTag("help_back")) {
                onAction(GameAction.Back)
            }
            NavalButton(gameText(R.string.tactics), Modifier.width(210.dp).focusRequester(tacticsButton).testTag("help_tactics")) {
                tactics = true
            }
        }
    }
}

@Composable
private fun HelpCard(kind: String, title: String, body: String, modifier: Modifier) {
    Column(modifier.fillMaxHeight().background(DeepWater.copy(alpha = .85f), RoundedCornerShape(10.dp))
        .padding(16.dp).testTag("help_card_$kind")) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            HelpDiagram(kind)
        }
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.testTag("help_title_$kind"))
        Spacer(Modifier.height(5.dp))
        Text(body, fontSize = 17.sp, lineHeight = 21.sp, color = Muted, maxLines = 3,
            modifier = Modifier.fillMaxWidth().height(63.dp).testTag("help_body_$kind"))
    }
}

@Composable
private fun HelpDiagram(kind: String) {
    val ships = shipBitmaps()
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().weight(1f).testTag("help_diagram_$kind")) {
            val side = minOf(size.width, size.height)
            val pitch = side / 4f
            val origin = Offset((size.width - side) / 2f, (size.height - side) / 2f)
            val stroke = 1.dp.toPx()
            drawRect(Ocean, origin, androidx.compose.ui.geometry.Size(side, side))
            for (line in 0..4) {
                drawLine(Steel.copy(alpha = .28f), origin + Offset(line * pitch, 0f),
                    origin + Offset(line * pitch, side), stroke)
                drawLine(Steel.copy(alpha = .28f), origin + Offset(0f, line * pitch),
                    origin + Offset(side, line * pitch), stroke)
            }
            when (kind) {
                "aim" -> {
                    val center = origin + Offset(1.5f * pitch, 1.5f * pitch)
                    drawTargetReticle(center, pitch * .32f, Cyan, 2.dp.toPx())
                }
                "fire" -> {
                    val miss = origin + Offset(1.5f * pitch, 1.5f * pitch)
                    val hit = origin + Offset(2.5f * pitch, 2.5f * pitch)
                    drawCircle(Ink, pitch * .21f, miss, style = Stroke(2.dp.toPx()))
                    val arm = pitch * .21f
                    drawLine(Impact, hit - Offset(arm, arm), hit + Offset(arm, arm), 3.dp.toPx())
                    drawLine(Impact, hit + Offset(-arm, arm), hit + Offset(arm, -arm), 3.dp.toPx())
                }
                "sink" -> drawNavalShip(Ship(ShipType.BATTLESHIP,
                    (0..3).map { Coordinate(1, it) }), ships.getValue(ShipType.BATTLESHIP), origin,
                    pitch, sunk = false)
            }
        }
        if (kind == "fire") {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("○ ${gameText(R.string.miss)}", fontSize = 15.sp, color = Ink)
                Text("× ${gameText(R.string.hit)}", fontSize = 15.sp, color = Impact)
            }
        } else {
            Spacer(Modifier.height(23.dp))
        }
    }
}
