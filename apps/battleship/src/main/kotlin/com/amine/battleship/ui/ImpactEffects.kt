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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import com.amine.battleship.game.model.ShotOutcome
import com.amine.battleship.viewmodel.ImpactPresentation
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ImpactEffects(compact: Boolean, impact: ImpactPresentation?, now: State<Long>) {
    Canvas(Modifier.fillMaxSize()) {
        val event = impact ?: return@Canvas
        val progress = impactProgress(event.startedAtNanos, now.value, event.outcome)
        if (progress >= 1f) return@Canvas
        val geometry = BoardGeometry.measure(size.width, compact)
        fun center(cell: com.amine.battleship.game.model.Coordinate) = geometry.center(cell).let { Offset(it.x, it.y) }
        val at = center(event.coordinate)
        val elapsed = progress * impactDurationMillis(event.outcome)
        clipRect(geometry.originX, geometry.originY, geometry.right, geometry.bottom) {
            when (event.outcome) {
                ShotOutcome.MISS -> drawSplash(at, geometry.pitch, elapsed)
                ShotOutcome.HIT -> drawFire(at, geometry.pitch, elapsed)
                ShotOutcome.SUNK -> {
                    // Ignition stays on the final hit. Delayed fires belong to this hull only.
                    drawFire(at, geometry.pitch, elapsed, sinking = true)
                    val cells = event.sunkCells
                    val fireCells = if (cells.size <= 3) cells else listOf(cells.first(), cells[cells.size / 2], cells.last())
                    fireCells.filter { it != event.coordinate }.forEachIndexed { index, cell ->
                        val local = elapsed - 300f - index * 220f
                        if (local >= 0) drawFire(center(cell), geometry.pitch * .72f, local, sinking = true, heatElapsed = elapsed)
                    }
                    if (elapsed in 1100f..2450f) {
                        val fade = sin(((elapsed - 1100f) / 1350f) * PI).toFloat().coerceIn(0f, 1f)
                        cells.forEachIndexed { index, cell ->
                            val point = center(cell)
                            val radius = geometry.pitch * (.28f + (elapsed - 1100) / 1350f * .24f)
                            drawArc(Foam.copy(alpha = fade * .30f), index * 57f, 130f, false,
                                point - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(geometry.pitch * .025f))
                        }
                    }
                }
            }
        }
    }
}

private val Foam = Color(0xFFCBE5ED)
private val Spray = Color(0xFF91BECE)

/** Every ring and droplet is in the same overhead plane: equal axes and no screen-Y gravity. */
private fun DrawScope.drawSplash(at: Offset, pitch: Float, elapsed: Float) {
    if (elapsed < 180f) {
        val fade = 1f - elapsed / 180f
        drawCircle(Color(0xFF020C13).copy(alpha = fade * .9f), pitch * .16f, at)
        repeat(5) { index ->
            drawArc(Foam.copy(alpha = fade * .85f), index * 72f + 7f, 45f, false,
                at - Offset(pitch * .20f, pitch * .20f), Size(pitch * .40f, pitch * .40f), style = Stroke(pitch * .04f))
        }
    }
    if (elapsed in 120f..600f) {
        val crown = ((elapsed - 120f) / 480f).coerceIn(0f, 1f)
        val radius = pitch * (.18f + crown * .31f)
        repeat(9) { index ->
            val jitter = .80f + (index % 4) * .075f
            val start = index * 40f + (index % 3 - 1) * 6f
            val sweep = 11f + index % 4 * 5f
            drawArc(Foam.copy(alpha = (1f - crown * crown) * (.65f + index % 3 * .10f)), start, sweep, false,
                at - Offset(radius * jitter, radius * jitter), Size(radius * jitter * 2, radius * jitter * 2),
                style = Stroke(pitch * (.022f + (1 - crown) * .027f + index % 3 * .004f)))
        }
    }
    repeat(3) { index ->
        val ring = ((elapsed - 260f - index * 90f) / 840f).coerceIn(0f, 1f)
        if (ring > 0) {
            val radius = pitch * (.20f + ring * .76f)
            val alpha = (1 - ring) * (.40f - index * .07f)
            if (index == 0) drawCircle(Foam.copy(alpha = alpha * .72f), radius, at, style = Stroke(pitch * .018f))
            else repeat(4) { fragment ->
                val start = fragment * 90f + index * 17f + fragment % 2 * 9f
                val sweep = 37f + ((fragment + index) % 3) * 13f
                drawArc(Foam.copy(alpha = alpha * (.72f + fragment % 3 * .12f)), start, sweep, false,
                    at - Offset(radius, radius), Size(radius * 2, radius * 2),
                    style = Stroke(pitch * (.016f + fragment % 3 * .004f)))
            }
        }
    }
    // Uneven foam fragments persist after the crown, breaking the clean reticle pattern.
    if (elapsed in 280f..650f) repeat(11) { index ->
        val age = ((elapsed - 280f) / 370f).coerceIn(0f, 1f)
        val angle = index * 2.39996f + .2f
        val radius = pitch * (.28f + age * .32f + index % 3 * .035f)
        val point = at + Offset(cos(angle), sin(angle)) * radius
        drawCircle(Foam.copy(alpha = (1f - age) * (.58f + index % 3 * .12f)),
            pitch * (.014f + index % 4 * .004f), point)
    }

    val spray = ((elapsed - 100f) / 500f).coerceIn(0f, 1f)
    if (elapsed > 100 && spray < 1) repeat(17) { index ->
        val angle = index * 2.39996f
        val radius = pitch * (.14f + spray * (.40f + index % 4 * .08f))
        drawCircle(if (index % 3 == 0) Foam.copy(alpha = 1 - spray) else Spray.copy(alpha = (1 - spray) * .7f),
            pitch * (.014f + index % 3 * .005f), at + Offset(cos(angle), sin(angle)) * radius)
    }
}

/** Smoke sits below warm lobes. Sinking shares one global heat deadline across its delayed fires. */
private fun DrawScope.drawFire(at: Offset, pitch: Float, elapsed: Float, sinking: Boolean = false,
    heatElapsed: Float = elapsed) {
    val duration = if (sinking) 2500f else 1500f
    if (elapsed < 0f || heatElapsed >= duration) return
    val p = heatElapsed / duration
    val holdUntil = if (sinking) 1400f else 650f
    val fadeUntil = if (sinking) 1900f else 1050f
    val ignition = (elapsed / if (sinking) 180f else 80f).coerceIn(.15f, 1f)
    val heat = (1f - ((heatElapsed - holdUntil) / (fadeUntil - holdUntil)).coerceIn(0f, 1f)) * ignition

    val smoke = sin(p * PI).toFloat() * .64f * (elapsed / 300f).coerceIn(0f, 1f)
    if (elapsed >= 160) repeat(7) { index ->
        val angle = index * 2.39996f + .6f
        val point = at + Offset(cos(angle), sin(angle)) * pitch * (.10f + p * .32f)
        val radius = pitch * (.16f + p * .22f + index % 3 * .025f)
        drawCircle(Brush.radialGradient(listOf(Color(0xFF677076).copy(alpha = smoke * .68f),
            Color(0xFF252D34).copy(alpha = smoke * .48f), Color.Transparent), point, radius), radius, point)
    }
    if (heat > 0) {
        repeat(6) { index ->
            val angle = index * 2.39996f + sin(elapsed * .008f + index * .7f) * .055f
            val flicker = .92f + .08f * sin(elapsed * .022f + index * 1.37f)
            val spread = pitch * (.025f + minOf(elapsed / 420f, 1f) * .15f)
            val point = at + Offset(cos(angle), sin(angle)) * spread
            val radius = pitch * (.19f + index % 3 * .025f) * flicker
            drawCircle(Brush.radialGradient(
                0f to Color(0xFFFFD37B).copy(alpha = heat * .98f),
                .25f to Color(0xFFFFB552).copy(alpha = heat * .94f),
                .62f to Color(0xFFF4852E).copy(alpha = heat * .80f),
                1f to Color.Transparent, center = point, radius = radius), radius, point)
        }
        // A small hot center adds depth without covering the full cell or its focus border.
        val coreRadius = pitch * .085f * (0.94f + .06f * sin(elapsed * .019f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFEDBC).copy(alpha = heat * .90f),
            Color(0xFFFFB24F).copy(alpha = heat * .65f), Color.Transparent), at, coreRadius), coreRadius, at)
    }
    if (elapsed < 700) repeat(11) { index ->
        val angle = index * 2.39996f + .3f
        val direction = Offset(cos(angle), sin(angle))
        val travel = (elapsed / 700f).coerceIn(0f, 1f)
        val point = at + direction * pitch * (.18f + travel * (.24f + index % 4 * .055f))
        drawLine(Color(0xFFF6BC68).copy(alpha = (1 - travel) * .85f), point - direction * pitch * .055f,
            point, pitch * .02f)
    }
}
