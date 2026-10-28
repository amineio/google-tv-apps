package com.amine.battleship.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.Ship
import com.amine.battleship.game.model.Shot
import com.amine.battleship.game.model.ShotOutcome
import com.amine.battleship.viewmodel.GameAction
import kotlin.math.cos
import kotlin.math.sin

/** One focus target owns all aiming input. Decorative cells never enter the focus graph. */
@Composable
internal fun GameBoard(
    ships: List<Ship>,
    shots: List<Shot>,
    modifier: Modifier = Modifier,
    selected: Coordinate? = null,
    lastShot: Shot? = null,
    canFire: Boolean = false,
    focusRequester: FocusRequester? = null,
    onAction: (GameAction) -> Unit = {},
) {
    val labels = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    } }
    val impact = remember { Animatable(1f) }
    LaunchedEffect(lastShot) {
        if (lastShot != null) { impact.snapTo(0f); impact.animateTo(1f, tween(650)) }
    }
    val sunkCells = shots.flatMap { it.sunkCells }.toSet()
    val chosenShot = shots.firstOrNull { it.coordinate == selected }
    val selectedState = when {
        selected in sunkCells -> "Sunk ship"
        chosenShot?.outcome == ShotOutcome.MISS -> "Miss"
        chosenShot != null -> "Hit"
        else -> "Unexplored"
    }
    var inputModifier = modifier.aspectRatio(1f)
    if (selected != null && focusRequester != null) {
        inputModifier = inputModifier.focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                when (event.key) {
                    Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight -> {
                        if (event.type == KeyEventType.KeyDown) {
                            val delta = when (event.key) {
                                Key.DirectionUp -> -1 to 0
                                Key.DirectionDown -> 1 to 0
                                Key.DirectionLeft -> 0 to -1
                                else -> 0 to 1
                            }
                            onAction(GameAction.Move(delta.first, delta.second))
                        }
                        true
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0 && canFire) {
                            onAction(GameAction.Fire)
                        }
                        true
                    }
                    else -> false
                }
            }
            .semantics {
                contentDescription = "Target ${selected.label}. ${if (canFire) "Your turn" else "Computer turn"}. $selectedState."
                stateDescription = selectedState
            }
            .testTag("targeting_board")
            .focusable()
    } else {
        inputModifier = inputModifier.semantics { contentDescription = "Your fleet. ${shots.size} incoming shots." }
            .testTag("friendly_board")
    }
    Canvas(inputModifier) {
        val gutter = size.width * if (selected == null) .10f else .067f
        val pitch = (size.width - gutter * 1.22f) / 10f
        val side = pitch * 10f
        val origin = Offset(gutter, gutter)
        fun center(coordinate: Coordinate) = origin + Offset((coordinate.column + .5f) * pitch, (coordinate.row + .5f) * pitch)
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFF102C3D), Color(0xFF081C2A)), origin, origin + Offset(side, side)),
            origin, Size(side, side), CornerRadius(3.dp.toPx()))
        for (row in 0..9) for (column in 0..9) {
            if ((row + column) % 2 == 0) drawRect(DeepWater.copy(alpha = .32f),
                origin + Offset(column * pitch, row * pitch), Size(pitch, pitch))
        }
        ships.forEach { ship ->
            drawNavalShip(ship, origin, pitch, ship.cells.all { cell -> shots.any { it.coordinate == cell } })
        }
        for (line in 0..10) {
            val axis = line * pitch
            drawLine(Muted.copy(alpha = if (line == 0 || line == 10) .6f else .28f),
                origin + Offset(axis, 0f), origin + Offset(axis, side), 1.dp.toPx())
            drawLine(Muted.copy(alpha = if (line == 0 || line == 10) .6f else .28f),
                origin + Offset(0f, axis), origin + Offset(side, axis), 1.dp.toPx())
        }
        labels.textSize = if (selected == null) minOf(13.sp.toPx(), pitch * .8f) else pitch * .43f
        for (index in 0..9) {
            labels.color = (if (selected?.column == index) Cyan else Muted).toArgb()
            drawContext.canvas.nativeCanvas.drawText(('A' + index).toString(),
                origin.x + (index + .5f) * pitch, gutter * .63f, labels)
            labels.color = (if (selected?.row == index) Cyan else Muted).toArgb()
            drawContext.canvas.nativeCanvas.drawText((index + 1).toString(), gutter * .39f,
                origin.y + (index + .5f) * pitch - (labels.ascent() + labels.descent()) / 2f, labels)
        }
        shots.forEach { shot ->
            val at = center(shot.coordinate)
            if (shot.outcome == ShotOutcome.MISS) {
                drawCircle(Ink.copy(alpha = .85f), pitch * .125f, at, style = Stroke(pitch * .065f))
            } else {
                val radius = pitch * .185f
                val color = if (shot.coordinate in sunkCells) Ink else Impact
                drawLine(color, at - Offset(radius, radius), at + Offset(radius, radius), pitch * .085f)
                drawLine(color, at + Offset(-radius, radius), at + Offset(radius, -radius), pitch * .085f)
            }
        }
        lastShot?.let { shot ->
            val at = center(shot.coordinate)
            val color = if (shot.outcome == ShotOutcome.MISS) Ink else Impact
            val extent = pitch * .41f
            val arm = pitch * .17f
            for (x in listOf(-1f, 1f)) for (y in listOf(-1f, 1f)) {
                val corner = at + Offset(x * extent, y * extent)
                drawLine(Brass, corner, corner - Offset(x * arm, 0f), 1.4.dp.toPx())
                drawLine(Brass, corner, corner - Offset(0f, y * arm), 1.4.dp.toPx())
            }
            if (impact.value < 1f) {
                val progress = impact.value
                drawCircle(color.copy(alpha = (1f - progress) * .75f),
                    pitch * (.24f + progress * .9f), at, style = Stroke(pitch * .065f * (1f - progress) + 1f))
                if (shot.outcome != ShotOutcome.MISS) {
                    repeat(8) { ray ->
                        val angle = ray * Math.PI / 4
                        val direction = Offset(cos(angle).toFloat(), sin(angle).toFloat())
                        drawLine(Impact.copy(alpha = 1f - progress), at + direction * pitch * progress * .4f,
                            at + direction * pitch * (.15f + progress * .8f), 2.dp.toPx())
                    }
                }
            }
        }
        selected?.let { coordinate ->
            val corner = origin + Offset(coordinate.column * pitch, coordinate.row * pitch)
            drawRect(Cyan.copy(alpha = .08f), corner, Size(pitch, pitch))
            drawRect(Cyan.copy(alpha = .15f), corner, Size(pitch, pitch), style = Stroke(8.dp.toPx()))
            drawRect(Cyan, corner, Size(pitch, pitch), style = Stroke(2.5.dp.toPx()))
            if (chosenShot == null) {
                val at = center(coordinate)
                val marker = pitch * .07f
                drawLine(Cyan, at - Offset(marker, 0f), at + Offset(marker, 0f), 1.5.dp.toPx())
                drawLine(Cyan, at - Offset(0f, marker), at + Offset(0f, marker), 1.5.dp.toPx())
            }
        }
    }
}
