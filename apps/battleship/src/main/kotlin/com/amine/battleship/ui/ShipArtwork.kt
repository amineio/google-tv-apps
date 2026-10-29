package com.amine.battleship.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.amine.battleship.R
import com.amine.battleship.game.model.Ship
import com.amine.battleship.game.model.ShipType
import kotlin.math.roundToInt

@Composable
internal fun shipBitmaps(): Map<ShipType, ImageBitmap> = mapOf(
    ShipType.AIRCRAFT_CARRIER to ImageBitmap.imageResource(R.drawable.ship_carrier),
    ShipType.BATTLESHIP to ImageBitmap.imageResource(R.drawable.ship_battleship),
    ShipType.CRUISER to ImageBitmap.imageResource(R.drawable.ship_cruiser),
    ShipType.SUBMARINE to ImageBitmap.imageResource(R.drawable.ship_submarine),
    ShipType.DESTROYER to ImageBitmap.imageResource(R.drawable.ship_destroyer),
)

private val WreckFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(.12f) })

/** Bow-up sources rotate as one hull. Sinking changes opacity, never hull position or span. */
internal fun DrawScope.drawNavalShip(ship: Ship, bitmap: ImageBitmap, origin: Offset, pitch: Float,
    sunk: Boolean, sinkingProgress: Float? = null) {
    if (ship.cells.isEmpty()) return
    val firstRow = ship.cells.minOf { it.row }
    val lastRow = ship.cells.maxOf { it.row }
    val firstColumn = ship.cells.minOf { it.column }
    val lastColumn = ship.cells.maxOf { it.column }
    val center = origin + Offset((firstColumn + lastColumn + 1) * pitch / 2f, (firstRow + lastRow + 1) * pitch / 2f)
    val length = (ship.cells.size - .15f) * pitch
    val widthLimit = pitch * when (ship.type) {
        ShipType.AIRCRAFT_CARRIER -> .80f
        ShipType.BATTLESHIP -> .68f
        ShipType.CRUISER -> .55f
        ShipType.SUBMARINE -> .44f
        ShipType.DESTROYER -> .42f
    }
    // Each distinct master is width-normalized to keep its full game footprint readable.
    val width = widthLimit
    val height = length
    val submersion = sinkingProgress?.let { ((it - .44f) / .40f).coerceIn(0f, 1f) }
    val alpha = if (!sunk) 1f else if (submersion != null) 1f - .70f * submersion else .30f
    rotate(if (firstRow == lastRow) 90f else 0f, center) {
        drawImage(bitmap, dstOffset = IntOffset((center.x - width / 2).roundToInt(), (center.y - height / 2).roundToInt()),
            dstSize = IntSize(width.roundToInt().coerceAtLeast(1), height.roundToInt().coerceAtLeast(1)),
            alpha = alpha, colorFilter = if (sunk && (submersion == null || submersion > .3f)) WreckFilter else null)
    }
}
