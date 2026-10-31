package com.amine.memoryparade

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private fun paradeX(index: Int) = 690f + (index % 4) * 60f
private fun paradeFeet(index: Int) = if (index < 4) 269f else 367f

@Composable internal fun Parade(game: GameState, active: Boolean) {
    val arrival = remember { Animatable(1f) }
    var presented by remember { mutableIntStateOf(game.parade.size) }
    LaunchedEffect(game.parade.size, active) {
        if (active && game.parade.size > presented) {
            presented = game.parade.size
            arrival.snapTo(0f); arrival.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        } else { presented = game.parade.size; arrival.snapTo(1f) }
    }
    val motion = if (active && game.parade.isNotEmpty()) {
        val transition = rememberInfiniteTransition(label = "happy friends")
        val phase by transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "gentle sway")
        phase
    } else 0f
    Image(painterResource(R.drawable.parade_stage_expanded), null, Modifier.offset(632.dp, 180.dp).size(300.dp, 240.dp), contentScale = ContentScale.FillBounds)
    game.parade.forEachIndexed { index, animal ->
        if (index < game.parade.lastIndex || arrival.value >= .99f) {
            Image(painterResource(animal.drawable), animal.label,
                Modifier.offset((paradeX(index) - 29f).dp, (paradeFeet(index) - 84f).dp).size(58.dp, 84.dp).graphicsLayer {
                    translationY = if (active) sin(motion + index * 1.7f) * 2.4f * density else 0f
                    rotationZ = if (active) sin(motion + index * 1.7f) * 1.5f else 0f
                }, alignment = Alignment.BottomCenter)
        }
    }
    if (arrival.value < .99f && game.parade.isNotEmpty()) {
        val index = game.parade.lastIndex
        val layout = BoardLayout(game.size)
        val source = game.lastMatch.lastOrNull() ?: 0
        val sx = layout.cardX(source) + (layout.cardWidth - 58f) / 2
        val sy = layout.cardY(source) + (layout.cardHeight - 84f) / 2
        val tx = paradeX(index) - 29f
        val ty = paradeFeet(index) - 84f
        val p = arrival.value
        Image(painterResource(game.parade.last().drawable), null, Modifier.offset((sx + (tx - sx) * p).dp, (sy + (ty - sy) * p - sin(p * PI).toFloat() * 55f).dp)
            .size(58.dp, 84.dp).graphicsLayer { scaleX = 1.2f - p * .2f; scaleY = scaleX; rotationZ = sin(p * PI).toFloat() * 8f }, alignment = Alignment.BottomCenter)
    }
    if (game.completed && active) CompletionSparkles()
}

@Composable internal fun MatchSparkles(game: GameState) {
    val card = game.second ?: return
    val layout = BoardLayout(game.size)
    val progress = remember(card) { Animatable(0f) }
    LaunchedEffect(card) { delay(260); progress.animateTo(1f, tween(600)) }
    Canvas(Modifier.offset((layout.cardX(card) - 16).dp, (layout.cardY(card) - 16).dp).size((layout.cardWidth + 32).dp, (layout.cardHeight + 32).dp)) {
        val p = progress.value
        repeat(12) { i ->
            val a = i * PI * 2 / 12
            val radius = size.minDimension * (.12f + .52f * p)
            val c = Offset(size.width / 2 + cos(a).toFloat() * radius, size.height / 2 + sin(a).toFloat() * radius)
            val r = (1 - p) * 5f
            drawLine(Gold.copy(alpha = 1 - p), c - Offset(r, 0f), c + Offset(r, 0f), 2f)
            drawLine(Gold.copy(alpha = 1 - p), c - Offset(0f, r), c + Offset(0f, r), 2f)
        }
    }
}
@Composable private fun CompletionSparkles() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1500)) }
    Canvas(Modifier.fillMaxSize()) {
        repeat(40) { i ->
            val p = progress.value
            val x = size.width * ((i * 37 % 100) / 100f)
            val y = size.height * (.2f + p * .7f) + sin(i.toFloat()) * 80f
            drawCircle(listOf(Gold, Cyan, Color(0xFFFA9B8C))[i % 3].copy(alpha = 1 - p), 3f, Offset(x, y))
        }
    }
}
