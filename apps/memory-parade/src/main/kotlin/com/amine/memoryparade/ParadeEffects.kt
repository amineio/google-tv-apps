package com.amine.memoryparade

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
    Image(painterResource(R.drawable.parade_stage), null, Modifier.offset(632.dp, 176.dp).size(300.dp, 274.dp), contentScale = ContentScale.FillBounds)
    Column(Modifier.offset(650.dp, 157.dp).width(265.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Label(if (game.completed) "Everyone is here!" else "Your forest friends", 19, Gold)
        Spacer(Modifier.height(12.dp))
        repeat(2) { row ->
            Box(Modifier.height(116.dp).fillMaxWidth()) {
                Row(Modifier.width(216.dp).align(Alignment.TopCenter), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(3) { col ->
                        val index = row * 3 + col
                        Box(Modifier.size(72.dp, 100.dp), contentAlignment = Alignment.BottomCenter) {
                            if (index < game.parade.size && (index < game.parade.lastIndex || arrival.value >= .99f)) {
                                Image(painterResource(game.parade[index].drawable), game.parade[index].label,
                                    Modifier.size(72.dp, 96.dp).graphicsLayer {
                                        translationY = if (active) sin(motion + index * 1.7f) * 2.4f * density else 0f
                                        rotationZ = if (active) sin(motion + index * 1.7f) * 1.5f else 0f
                                    })
                            }
                        }
                    }
                }
            }
            if (row == 0) Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(13.dp))
        Label(if (game.parade.isEmpty()) "Who will join first?" else "${game.parade.size} friends together", 16)
    }
    if (arrival.value < .99f && game.parade.isNotEmpty()) {
        val index = game.parade.lastIndex
        val source = game.lastMatch.lastOrNull() ?: 0
        val sx = 48f + (source % 4) * 144f + 22f
        val sy = 120f + (source / 4) * 125f + 10f
        val tx = 665f + (index % 3) * 72f
        val ty = 196f + (index / 3) * 122f
        val p = arrival.value
        Image(painterResource(game.parade.last().drawable), null, Modifier.offset((sx + (tx - sx) * p).dp, (sy + (ty - sy) * p - sin(p * PI).toFloat() * 65f).dp)
            .size(90.dp, 100.dp).graphicsLayer { scaleX = .8f + p * .2f; scaleY = .8f + p * .2f; rotationZ = sin(p * PI).toFloat() * 8f })
    }
    if (game.completed && active) CompletionSparkles()
}

@Composable internal fun MatchSparkles(card: Int) {
    val progress = remember(card) { Animatable(0f) }
    LaunchedEffect(card) { delay(260); progress.animateTo(1f, tween(600)) }
    Canvas(Modifier.offset((48 + card % 4 * 144 - 20).dp, (120 + card / 4 * 125 - 20).dp).size(174.dp, 155.dp)) {
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
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1150)) }
    Canvas(Modifier.fillMaxSize()) {
        repeat(45) { i ->
            val p = progress.value
            val x = size.width * ((i * 37 % 100) / 100f)
            val y = size.height * (.2f + p * .7f) + sin(i.toFloat()) * 80f
            drawCircle(listOf(Gold, Cyan, Color(0xFFFA9B8C))[i % 3].copy(alpha = 1 - p), 3f, Offset(x, y))
        }
    }
}
