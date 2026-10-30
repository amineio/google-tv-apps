package com.amine.treasureorretreat

import android.view.KeyEvent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import kotlin.math.*

private val Cream = Color(0xFFFFE8AE)
private val Gold = Color(0xFFE6B55D)
private val Ink = Color(0xFF102421)

@Composable fun TreasureScreen(model: TreasureModel, key: (KeyEvent) -> Boolean) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(model.active) { if (model.active) focus.requestFocus() }
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).focusRequester(focus)
        .onPreviewKeyEvent { key(it.nativeKeyEvent) }.focusable().testTag("gameRoot")) {
        val scale = min(maxWidth.value / 960f, maxHeight.value / 540f)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(Modifier.requiredSize(960.dp, 540.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
                Image(painterResource(R.drawable.temple), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.22f), Color.Transparent, Color.Black.copy(.64f)))))
                if (model.screen == "welcome") Welcome(model) else GameScene(model)
                if (model.paused) PauseMenu(model)
            }
        }
    }
}

@Composable private fun Welcome(model: TreasureModel) {
    Column(Modifier.offset(58.dp, 66.dp).width(420.dp)) {
        Text("A TEMPLE ADVENTURE", color = Cream.copy(.8f), fontSize = 13.sp, letterSpacing = 3.sp)
        Spacer(Modifier.height(14.dp))
        Title(48.sp)
        Spacer(Modifier.height(20.dp))
        Text("Collect treasure. Know when to retreat.", color = Cream, fontSize = 19.sp)
        Spacer(Modifier.height(12.dp))
        Text("Three expeditions. One brave decision at a time.", color = Cream.copy(.85f), fontSize = 15.sp)
        Spacer(Modifier.height(32.dp))
        GameButton("Enter the temple", true, Color(0xFF225940), Modifier.width(288.dp).testTag("play"))
        Spacer(Modifier.height(18.dp))
        Text("Best treasure  ${model.best}", color = Gold, fontSize = 16.sp)
    }
    Chest(null, false, false, Modifier.offset(500.dp, 148.dp))
    Text("OK  Play      •      Back  Exit", color = Cream.copy(.75f), fontSize = 13.sp,
        modifier = Modifier.offset(58.dp, 488.dp))
}

@Composable private fun Title(size: TextUnit) {
    Text("Treasure", color = Cream, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
        fontSize = size, lineHeight = size, style = androidx.compose.ui.text.TextStyle(shadow = Shadow(Color.Black, androidx.compose.ui.geometry.Offset(2f, 4f), 6f)))
    Text("or Retreat", color = Gold, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold,
        fontSize = size * .8f, lineHeight = size)
}

@Composable private fun GameScene(model: TreasureModel) {
    val game = model.game
    Column(Modifier.offset(48.dp, 34.dp).width(250.dp)) {
        Title(30.sp)
        Text("EXPEDITION ${game.expedition} / 3", color = Cream, fontSize = 12.sp, letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 10.dp).testTag("expedition"))
    }
    ScorePanel(game, model.best, Modifier.offset(48.dp, 185.dp).width(205.dp))
    RemainingPanel(game, Modifier.offset(637.dp, 35.dp).width(275.dp))
    Chest(game.lastCard, game.phase == Phase.REVEALING, model.paused || !model.active,
        Modifier.offset(277.dp, 100.dp))
    BankingTransfer(game, model.paused || !model.active)
    ResultCard(game, Modifier.offset(696.dp, 212.dp).size(150.dp, 171.dp))
    val status = when {
        game.phase == Phase.FINISHED -> "Your expedition is complete"
        game.outcome == Outcome.TRAPPED -> "Trap! Unbanked treasure lost"
        game.outcome == Outcome.BANKED -> "Treasure secured!"
        game.phase == Phase.REVEALING -> "A discovery in the temple…"
        game.event == 0 -> "A little treasure to begin your journey"
        else -> "${game.lastCard?.label}  +${game.lastCard?.value}"
    }
    Text(status, color = Cream, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.offset(265.dp, 397.dp).width(430.dp).testTag("status"), textAlign = TextAlign.Center)
    if (game.phase == Phase.READY || game.phase == Phase.REVEALING) {
        Row(Modifier.offset(265.dp, 434.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GameButton("Explore Again", model.selected == 0, Color(0xFF24633D), Modifier.width(246.dp).testTag("explore"))
            GameButton("Bank Treasure", model.selected == 1, Color(0xFF7D2B23), Modifier.width(246.dp).testTag("bank"))
        }
    } else if (game.phase == Phase.RESULT) {
        GameButton(if (game.expedition == 3) "See final score" else "Next expedition", true,
            Color(0xFF24633D), Modifier.offset(350.dp, 434.dp).width(300.dp).testTag("next"))
    } else {
        FinalOverlay(model)
    }
    Text("← →  Choose     OK  Select     Back  Pause", color = Cream.copy(.75f), fontSize = 12.sp,
        modifier = Modifier.offset(48.dp, 501.dp))
}

@Composable private fun ScorePanel(game: GameState, best: Int, modifier: Modifier) {
    val shownUnbanked by animateIntAsState(game.unbanked, tween(450), label = "treasure")
    val shownBanked by animateIntAsState(game.banked, tween(650), label = "banked")
    Box(modifier.height(230.dp).testTag("ledger")) {
        Image(painterResource(R.drawable.ledger), null, Modifier.matchParentSize(), contentScale = ContentScale.FillBounds)
        Column(Modifier.fillMaxSize().padding(start = 30.dp, end = 26.dp, top = 36.dp, bottom = 24.dp)) {
            Text("At risk", color = Ink, fontSize = 14.sp)
            Text("$shownUnbanked", color = Ink, fontWeight = FontWeight.Bold, fontSize = 35.sp, modifier = Modifier.testTag("unbanked"))
            Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(1.dp).background(Ink.copy(.25f)))
            Text("Safely banked", color = Ink, fontSize = 14.sp)
            Text("$shownBanked", color = Ink, fontWeight = FontWeight.Bold, fontSize = 30.sp, modifier = Modifier.testTag("banked"))
            Text("Best  $best", color = Ink.copy(.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable private fun RemainingPanel(game: GameState, modifier: Modifier) {
    Column(modifier.background(Ink.copy(.92f), RoundedCornerShape(12.dp)).border(1.dp, Gold.copy(.6f), RoundedCornerShape(12.dp)).carvedCorners().padding(15.dp)) {
        Text("Remaining in this expedition", color = Cream, fontSize = 14.sp)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Card.entries.forEach { card ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(card.art()), null, Modifier.size(36.dp))
                    Text("${game.remaining(card)}", color = Cream, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text("Trap risk: ${game.remaining(Card.TRAP)} / ${game.deck.size}", color = Gold, fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 9.dp).testTag("risk"))
    }
}

private fun GameState.resultArt(): Int = if (lastCard == Card.TRAP) when ((event + expedition) % 4) {
    0 -> R.drawable.snake
    1 -> R.drawable.boulder
    2 -> R.drawable.spikes
    else -> R.drawable.guardian
} else lastCard?.art() ?: R.drawable.gold

private fun Card.art(): Int = when (this) {
    Card.GOLD -> R.drawable.gold
    Card.RUBY -> R.drawable.ruby
    Card.EMERALD -> R.drawable.emerald
    Card.TRAP -> R.drawable.snake
}

@Composable private fun ResultCard(game: GameState, modifier: Modifier) {
    val card = game.lastCard ?: return
    Column(modifier.background(Brush.verticalGradient(listOf(Color(0xFF203D32), Ink)), RoundedCornerShape(13.dp))
        .border(2.dp, Gold, RoundedCornerShape(13.dp)).carvedCorners().padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (card == Card.TRAP) "TEMPLE TRAP" else "DISCOVERED", color = Cream.copy(.8f), fontSize = 10.sp, letterSpacing = 1.sp)
        Image(painterResource(game.resultArt()), card.label, Modifier.weight(1f).fillMaxWidth().padding(5.dp))
        Text(if (card == Card.TRAP) "Treasure lost" else "+${card.value}  ${card.label}", color = Cream,
            fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable private fun Chest(card: Card?, revealing: Boolean, paused: Boolean, modifier: Modifier) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(revealing, paused) {
        if (revealing && !paused) { progress.snapTo(0f); progress.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
    }
    val p = if (paused) 1f else progress.value
    Box(modifier.size(365.dp, 280.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFC954).copy(alpha = if (card == Card.TRAP) .1f else .3f), Color.Transparent)), radius = size.width * .48f,
                center = androidx.compose.ui.geometry.Offset(size.width * .5f, size.height * .6f))
            if (revealing && !paused) repeat(16) { n ->
                val angle = n * 2f * PI.toFloat() / 16
                val radius = size.width * (.14f + p * .34f)
                drawCircle(Cream.copy(alpha = (1f - p).coerceIn(0f, 1f)), 2f + (n % 3),
                    androidx.compose.ui.geometry.Offset(size.width / 2 + cos(angle) * radius, size.height * .6f + sin(angle) * radius * .65f))
            }
        }
        // The 800x410 bitmap hinge runs from approximately (74,350) to (775,350).
        // Match the rear lip from (837,405) to (1150,444) in the 1920x1080 capture.
        Image(painterResource(R.drawable.chest_lid), null, Modifier.offset(124.dp, 32.dp).size(182.dp, 93.3.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(.5f, .854f)
                rotationX = (1f-p) * 77f
                rotationZ = 7f
                cameraDistance = 16f * density
            })
        Image(painterResource(R.drawable.chest_base), "Treasure chest", Modifier.offset(48.dp, 90.dp).size(268.dp, 200.dp)
            .graphicsLayer { translationX = if (revealing && !paused) sin(p * PI.toFloat() * 8) * (1-p) * 7f else 0f })
        if (revealing && card != null && !paused) Image(painterResource(card.art()), null,
            Modifier.offset(132.dp, (100 - p * 70).dp).size(105.dp).graphicsLayer { alpha = sin(p * PI.toFloat()).coerceIn(0f,1f); scaleX = .6f + p*.6f; scaleY = scaleX })
    }
}

@Composable private fun GameButton(label: String, selected: Boolean, fill: Color, modifier: Modifier = Modifier) {
    Box(modifier.height(57.dp).graphicsLayer { scaleX = if (selected) 1.025f else 1f; scaleY = scaleX }
        .shadow(if (selected) 12.dp else 4.dp, RoundedCornerShape(14.dp))
        .background(Brush.verticalGradient(listOf(fill.copy(red = (fill.red + .08f).coerceAtMost(1f)), fill)), RoundedCornerShape(14.dp))
        .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Gold, RoundedCornerShape(14.dp))
        .drawWithContent {
            drawContent()
            drawRoundRect(Gold.copy(.7f), topLeft = androidx.compose.ui.geometry.Offset(6.dp.toPx(), 6.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width - 12.dp.toPx(), size.height - 12.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
        }.semantics { this.selected = selected; role = Role.Button }, contentAlignment = Alignment.Center) {
        Text(if (selected) "›  $label" else label, color = Cream, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun PauseMenu(model: TreasureModel) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(.78f)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(350.dp).background(Ink, RoundedCornerShape(18.dp)).border(2.dp, Gold, RoundedCornerShape(18.dp)).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Expedition paused", color = Cream, fontSize = 27.sp, fontFamily = FontFamily.Serif)
            listOf("Resume", "Restart", "Sound: ${if (model.sound) "On" else "Off"}", "Exit").forEachIndexed { index, label ->
                GameButton(label, index == model.menuSelected, Color(0xFF285341), Modifier.fillMaxWidth().testTag("pause$index"))
            }
        }
    }
}

@Composable private fun FinalOverlay(model: TreasureModel) {
    Box(Modifier.fillMaxSize().background(Ink.copy(.7f)), contentAlignment = Alignment.Center) {
        FinaleParticles(model.paused || !model.active)
        Column(Modifier.width(650.dp).background(Brush.verticalGradient(listOf(Ink.copy(.95f), Color(0xFF153A2D).copy(.94f))), RoundedCornerShape(25.dp))
            .border(2.dp, Gold, RoundedCornerShape(25.dp)).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("A journey worth taking", color = Gold, fontSize = 18.sp, letterSpacing = 2.sp)
            Text("Treasure secured!", color = Cream, fontSize = 43.sp, fontFamily = FontFamily.Serif)
            Image(painterResource(R.drawable.gold), null, Modifier.size(100.dp))
            Text("${model.game.banked}", color = Cream, fontSize = 48.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("finalScore"))
            Text("Best treasure  ${model.best}", color = Gold, fontSize = 20.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                GameButton("Play Again", model.selected == 0, Color(0xFF24633D), Modifier.width(230.dp).testTag("again"))
                GameButton("Exit", model.selected == 1, Color(0xFF6A3929), Modifier.width(180.dp).testTag("exit"))
            }
        }
    }
}

@Composable private fun BankingTransfer(game: GameState, paused: Boolean) {
    val transfer = remember { Animatable(1f) }
    LaunchedEffect(game.event, paused) {
        if (game.outcome == Outcome.BANKED && game.phase == Phase.REVEALING && !paused) {
            transfer.snapTo(0f)
            transfer.animateTo(1f, tween(780, easing = LinearEasing))
        }
    }
    if (game.outcome == Outcome.BANKED && game.phase == Phase.REVEALING && !paused) {
        repeat(7) { index ->
            val p = ((transfer.value * 1.4f - index * .06f)).coerceIn(0f, 1f)
            Image(painterResource(R.drawable.gold), null,
                Modifier.offset((454f - 310f*p).dp, (275f + 36f*p - sin(p*PI.toFloat())*90f).dp)
                    .size(32.dp).graphicsLayer { alpha = if (p > .98f || p < .01f) 0f else 1f; rotationZ = p*120f })
        }
    }
}

@Composable private fun FinaleParticles(paused: Boolean) {
    val burst = remember { Animatable(0f) }
    LaunchedEffect(paused) { if (!paused) burst.animateTo(1f, tween(1600, easing = LinearEasing)) }
    if (burst.value < 1f && !paused) Canvas(Modifier.fillMaxSize()) {
        repeat(32) { index ->
            val p = burst.value
            val a = index * 2f * PI.toFloat() / 32f
            val r = size.width * p * .6f
            val center = androidx.compose.ui.geometry.Offset(size.width*.5f + cos(a)*r, size.height*.45f + sin(a)*r*.7f + p*p*size.height*.2f)
            drawCircle(if (index % 3 == 0) Color(0xFF7DE5A7).copy(alpha = 1-p) else Cream.copy(alpha = 1-p), 3f + index%4, center)
        }
    }
}


private fun Modifier.carvedCorners(): Modifier = drawWithContent {
    drawContent()
    val inset = 7.dp.toPx()
    for (x in listOf(inset, size.width-inset)) for (y in listOf(inset, size.height-inset)) {
        drawCircle(Color(0xFF624124).copy(.5f), 2.6.dp.toPx(), androidx.compose.ui.geometry.Offset(x+.8.dp.toPx(), y+1.dp.toPx()))
        drawCircle(Gold, 2.dp.toPx(), androidx.compose.ui.geometry.Offset(x, y))
        drawCircle(Cream.copy(.6f), .65.dp.toPx(), androidx.compose.ui.geometry.Offset(x-.5.dp.toPx(), y-.5.dp.toPx()))
    }
}
