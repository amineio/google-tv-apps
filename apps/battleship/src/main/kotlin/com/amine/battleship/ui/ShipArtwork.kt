package com.amine.battleship.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.Ship
import com.amine.battleship.game.model.ShipType

/** Native deck silhouettes. All dimensions are fractions of a board cell. */
internal fun DrawScope.drawNavalShip(ship: Ship, origin: Offset, pitch: Float, sunk: Boolean) {
    if (ship.cells.isEmpty() || pitch <= 0f) return
    val first = ship.cells.minWith(compareBy<Coordinate> { it.row }.thenBy { it.column })
    val last = ship.cells.maxWith(compareBy<Coordinate> { it.row }.thenBy { it.column })
    val center = origin + Offset(
        (first.column + last.column + 1f) * pitch / 2f,
        (first.row + last.row + 1f) * pitch / 2f,
    )
    val length = ship.type.length.toFloat()
    val halfLength = length / 2f - .15f
    val halfWidth = when (ship.type) {
        ShipType.AIRCRAFT_CARRIER -> .38f
        ShipType.BATTLESHIP -> .37f
        ShipType.CRUISER -> .35f
        ShipType.SUBMARINE -> .28f
        ShipType.DESTROYER -> .32f
    }
    val steel = if (sunk) lerp(Steel, Impact, .62f) else Steel
    val deck = lerp(steel, Ocean, if (sunk) .28f else .18f)
    val highlight = lerp(steel, Ink, if (sunk) .12f else .38f)
    val detail = lerp(steel, Ocean, .68f)
    fun point(x: Float, y: Float) = center + Offset(x * pitch, y * pitch)

    rotate(if (first.row == last.row) 0f else 90f, center) {
        // The clip is a final guard. The shadow and outline also fit without clipping.
        clipRect(
            center.x - length * pitch / 2f + pitch * .04f,
            center.y - pitch * .46f,
            center.x + length * pitch / 2f - pitch * .04f,
            center.y + pitch * .46f,
        ) {
            val hull = Path().apply {
                if (ship.type == ShipType.SUBMARINE) {
                    val stern = point(-halfLength, 0f)
                    moveTo(stern.x, stern.y)
                    cubicTo(
                        point(-halfLength, -halfWidth).x, point(-halfLength, -halfWidth).y,
                        point(-halfLength + .3f, -halfWidth).x, point(-halfLength + .3f, -halfWidth).y,
                        point(halfLength - .4f, -halfWidth).x, point(halfLength - .4f, -halfWidth).y,
                    )
                    cubicTo(
                        point(halfLength + .08f, -halfWidth).x, point(halfLength + .08f, -halfWidth).y,
                        point(halfLength + .08f, halfWidth).x, point(halfLength + .08f, halfWidth).y,
                        point(halfLength - .4f, halfWidth).x, point(halfLength - .4f, halfWidth).y,
                    )
                    cubicTo(
                        point(-halfLength + .3f, halfWidth).x, point(-halfLength + .3f, halfWidth).y,
                        point(-halfLength, halfWidth).x, point(-halfLength, halfWidth).y,
                        stern.x, stern.y,
                    )
                } else {
                    val sternWidth = if (ship.type == ShipType.AIRCRAFT_CARRIER) .9f else .67f
                    val bowShoulder = if (ship.type == ShipType.AIRCRAFT_CARRIER) .33f else .53f
                    val points = listOf(
                        point(-halfLength, -halfWidth * sternWidth),
                        point(-halfLength + .2f, -halfWidth),
                        point(halfLength - bowShoulder, -halfWidth),
                        point(halfLength, 0f),
                        point(halfLength - bowShoulder, halfWidth),
                        point(-halfLength + .2f, halfWidth),
                        point(-halfLength, halfWidth * sternWidth),
                    )
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                close()
            }
            translate(pitch * .035f, pitch * .045f) {
                drawPath(hull, Ocean.copy(alpha = .9f), style = Stroke(pitch * .06f))
            }
            drawPath(hull, Brush.linearGradient(
                colors = listOf(highlight, steel, deck),
                start = point(0f, -halfWidth),
                end = point(0f, halfWidth),
            ))
            drawPath(hull, detail, style = Stroke(pitch * .035f))

            fun deckBlock(x: Float, y: Float, width: Float, height: Float) {
                drawRoundRect(detail, point(x + .025f, y + .035f),
                    Size(width * pitch, height * pitch), CornerRadius(pitch * .05f))
                drawRoundRect(deck, point(x, y),
                    Size(width * pitch, height * pitch), CornerRadius(pitch * .05f))
                drawLine(highlight, point(x + .05f, y + .035f),
                    point(x + width - .05f, y + .035f), pitch * .025f)
            }
            fun turret(x: Float, radius: Float, forward: Boolean, twin: Boolean) {
                val direction = if (forward) 1f else -1f
                val end = x + direction * (radius + .19f)
                val barrelY = if (twin) .045f else 0f
                drawCircle(detail, radius * pitch + pitch * .025f, point(x + .02f, .025f))
                drawCircle(deck, radius * pitch, point(x, 0f))
                drawLine(highlight, point(x - radius * .5f, -radius * .45f),
                    point(x + radius * .5f, -radius * .45f), pitch * .025f)
                drawLine(detail, point(x, -barrelY), point(end, -barrelY), pitch * .045f)
                if (twin) drawLine(detail, point(x, barrelY), point(end, barrelY), pitch * .045f)
            }

            when (ship.type) {
                ShipType.AIRCRAFT_CARRIER -> {
                    // A long offset runway and a separate island distinguish the flat deck.
                    drawLine(detail.copy(alpha = .7f), point(-halfLength + .15f, .17f),
                        point(halfLength - .38f, .03f), pitch * .22f)
                    drawLine(highlight.copy(alpha = .8f), point(-halfLength + .18f, .07f),
                        point(halfLength - .48f, -.04f), pitch * .025f)
                    drawLine(highlight.copy(alpha = .6f), point(-halfLength + .18f, .27f),
                        point(halfLength - .48f, .16f), pitch * .025f)
                    repeat(4) { index ->
                        val x = -1.65f + index * .92f
                        drawLine(highlight.copy(alpha = .75f), point(x, .15f - index * .025f),
                            point(x + .25f, .15f - index * .025f), pitch * .035f)
                    }
                    deckBlock(-.52f, -.30f, .88f, .19f)
                    deckBlock(-.2f, -.28f, .3f, .12f)
                }
                ShipType.BATTLESHIP -> {
                    deckBlock(-.46f, -.19f, .89f, .38f)
                    deckBlock(-.16f, -.13f, .34f, .26f)
                    turret(-1.25f, .16f, forward = false, twin = true)
                    turret(-.76f, .15f, forward = false, twin = true)
                    turret(.75f, .15f, forward = true, twin = true)
                    turret(1.24f, .16f, forward = true, twin = true)
                }
                ShipType.CRUISER -> {
                    deckBlock(-.42f, -.17f, .7f, .34f)
                    deckBlock(.02f, -.12f, .32f, .24f)
                    drawCircle(detail, pitch * .085f, point(-.2f, 0f))
                    turret(-.87f, .12f, forward = false, twin = false)
                    turret(.85f, .12f, forward = true, twin = false)
                }
                ShipType.SUBMARINE -> {
                    drawLine(highlight.copy(alpha = .45f), point(-.92f, -.13f),
                        point(.85f, -.13f), pitch * .03f)
                    deckBlock(-.3f, -.12f, .6f, .24f)
                    drawLine(detail, point(.03f, -.12f), point(.03f, -.23f), pitch * .055f)
                    drawLine(detail, point(-1.16f, -.22f), point(-1.16f, .22f), pitch * .065f)
                }
                ShipType.DESTROYER -> {
                    deckBlock(-.36f, -.15f, .55f, .3f)
                    drawCircle(detail, pitch * .085f, point(-.14f, 0f))
                    turret(.46f, .115f, forward = true, twin = false)
                    drawLine(detail, point(-.63f, -.13f), point(-.63f, .13f), pitch * .08f)
                }
            }
        }
    }
}
