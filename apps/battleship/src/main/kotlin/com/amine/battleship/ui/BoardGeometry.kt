package com.amine.battleship.ui

import com.amine.battleship.game.model.Coordinate

/** All layers share these pixel bounds, including their clipping rectangle. */
internal data class BoardGeometry(val originX: Float, val originY: Float, val pitch: Float) {
    val side: Float get() = pitch * 10f
    val right: Float get() = originX + side
    val bottom: Float get() = originY + side
    fun center(coordinate: Coordinate): BoardPoint = BoardPoint(
        originX + (coordinate.column + .5f) * pitch,
        originY + (coordinate.row + .5f) * pitch,
    )

    companion object {
        fun measure(width: Float, compact: Boolean): BoardGeometry {
            val gutter = width * if (compact) .10f else .067f
            return BoardGeometry(gutter, gutter, (width - gutter * 1.22f) / 10f)
        }
    }
}

internal data class BoardPoint(val x: Float, val y: Float)
