package com.amine.battleship.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import com.amine.battleship.game.model.ShotOutcome
import com.amine.battleship.viewmodel.ImpactPresentation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
internal fun ImpactEffects(compact: Boolean, impact: ImpactPresentation?, now: State<Long>) {
    Canvas(Modifier.fillMaxSize().graphicsLayer()) {
        val event = impact ?: return@Canvas
        val progress = impactProgress(event.startedAtNanos, now.value)
        if (progress >= 1f) return@Canvas
        val geometry = BoardGeometry.measure(size.width, compact)
        val point = geometry.center(event.coordinate)
        clipRect(geometry.originX, geometry.originY, geometry.right, geometry.bottom) {
            val at = Offset(point.x, point.y)
            when (event.outcome) {
                ShotOutcome.MISS -> drawSplash(at, geometry.pitch, progress)
                ShotOutcome.HIT -> drawBurst(at, geometry.pitch, progress, sunk = false)
                ShotOutcome.SUNK -> drawBurst(at, geometry.pitch, progress, sunk = true)
            }
        }
    }
}

private val Foam = Color(0xFFCBE5ED)
private val Spray = Color(0xFF91BECE)

private fun DrawScope.drawSplash(at: Offset, pitch: Float, p: Float) {
    val fade = 1f - p
    // Surface rings spread beneath the short crown. Their shadows give the water displacement depth.
    repeat(3) { index ->
        val ring = (p - index * .12f).coerceAtLeast(0f)
        if (ring > 0f) {
            val rx = pitch * (.20f + ring * (1.02f + index * .11f))
            val ry = rx * .61f
            drawOval(Color(0xFF03131D).copy(alpha = fade * .45f), at - Offset(rx, ry - pitch * .035f),
                Size(rx * 2f, ry * 2f), style = Stroke(pitch * .065f))
            drawOval(Foam.copy(alpha = fade * (.54f - index * .10f)), at - Offset(rx, ry),
                Size(rx * 2f, ry * 2f), style = Stroke(pitch * (.026f + fade * .015f)))
        }
    }
    val crownTime = (p / .64f).coerceIn(0f, 1f)
    val lift = sin(crownTime * PI).toFloat() * pitch * .48f
    val spread = pitch * (.18f + p * .27f)
    if (crownTime < 1f) {
        val crown = Path()
        crown.moveTo(at.x - spread, at.y + pitch * .08f)
        repeat(9) { tooth ->
            val x = at.x - spread + spread * 2f * tooth / 8f
            val height = lift * if (tooth % 2 == 0) 1f else .48f
            crown.lineTo(x, at.y - height + pitch * .03f)
        }
        crown.lineTo(at.x + spread * .75f, at.y + pitch * .18f)
        crown.quadraticTo(at.x, at.y + pitch * .27f, at.x - spread, at.y + pitch * .08f)
        crown.close()
        drawPath(crown, Brush.verticalGradient(listOf(Foam.copy(alpha = fade * .85f),
            Spray.copy(alpha = fade * .33f)), at.y - lift, at.y + pitch * .24f))
        drawPath(crown, Foam.copy(alpha = fade * .65f), style = Stroke(pitch * .022f))
    }
    repeat(13) { index ->
        val angle = index * 2.39996f
        val velocity = .55f + (index % 4) * .13f
        val radial = pitch * (.12f + p * velocity)
        val drop = at + Offset(cos(angle) * radial,
            sin(angle) * radial * .48f - pitch * (1.8f * p - 1.65f * p * p) * velocity)
        val radius = pitch * (.018f + (index % 3) * .006f) * sqrt(fade)
        drawOval(Spray.copy(alpha = fade * .6f), drop + Offset(-radius, radius * .4f),
            Size(radius * 2.1f, radius * 3.1f))
        drawOval(Foam.copy(alpha = fade * .92f), drop - Offset(radius * .65f, radius),
            Size(radius * 1.25f, radius * 2.4f))
    }
}

private fun DrawScope.drawBurst(at: Offset, pitch: Float, p: Float, sunk: Boolean) {
    val fade = 1f - p
    val strength = if (sunk) 1.2f else 1f
    val pressure = pitch * (.23f + p * .92f * strength)
    drawOval(Spray.copy(alpha = fade * .30f), at - Offset(pressure, pressure * .63f),
        Size(pressure * 2f, pressure * 1.26f), style = Stroke(pitch * .035f))

    // Smoke sits behind the hot core. Its lifted lobes remain visible outside the shot cross.
    val smokeFade = sin(p * PI).toFloat() * fade
    repeat(if (sunk) 9 else 6) { index ->
        val angle = index * 2.39996f
        val spread = pitch * (.14f + p * .23f) * strength
        val center = at + Offset(cos(angle) * spread,
            sin(angle) * spread * .66f - pitch * (.09f + p * .40f))
        val radius = pitch * (.16f + p * .24f) * (1f + (index % 3) * .11f) * strength
        drawCircle(Brush.radialGradient(listOf(Color(0xFF7D8182).copy(alpha = smokeFade * .72f),
            Color(0xFF4E585E).copy(alpha = smokeFade * .53f), Color.Transparent), center, radius), radius, center)
    }

    // Hold the ignition through 120 ms, then lose heat before the 480 ms deadline.
    val heat = (1f - ((p - .18f) / .60f).coerceIn(0f, 1f))
    if (heat > 0f) {
        val radius = pitch * (.43f + p * .24f) * strength
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE3A1).copy(alpha = heat * .98f),
            Color(0xFFF2A454).copy(alpha = heat * .85f), Color(0xFFC96E38).copy(alpha = heat * .36f),
            Color.Transparent), at, radius), radius, at)
        repeat(if (sunk) 7 else 5) { index ->
            val angle = index * 2.39996f
            val lobe = at + Offset(cos(angle) * pitch * (.10f + p * .16f) * strength,
                sin(angle) * pitch * (.07f + p * .12f) * strength - pitch * p * .12f)
            val lobeRadius = pitch * (.13f + p * .12f) * strength
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE7AA).copy(alpha = heat * .90f),
                Color(0xFFFFB453).copy(alpha = heat * .72f), Color.Transparent), lobe, lobeRadius), lobeRadius, lobe)
        }
        drawCircle(Color(0xFFFFEDBC).copy(alpha = heat * .95f),
            pitch * (.15f + p * .06f) * heat * strength, at - Offset(0f, pitch * p * .08f))
    }
    repeat(if (sunk) 23 else 15) { index ->
        val angle = index * 2.39996f + .25f
        val direction = Offset(cos(angle), sin(angle) * .82f - .13f)
        val speed = .69f + (index % 5) * .13f
        val distance = pitch * (.28f + p * speed * strength)
        val drop = Offset(0f, pitch * p * p * .43f)
        val tip = at + direction * distance + drop
        val tail = at + direction * (distance - pitch * (.12f + fade * .11f)) + drop
        val sparkFade = fade * (1f - p * .25f)
        drawLine(Color(0xFFF2A64D).copy(alpha = sparkFade * .85f), tail, tip,
            pitch * (.035f + (index % 2) * .012f) * fade + .5f)
        drawLine(Color(0xFFFFE8A6).copy(alpha = sparkFade),
            tip - direction * pitch * .065f, tip, pitch * .022f * fade + .5f)
    }
}
