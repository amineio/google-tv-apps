package com.amine.battleship.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** An open-center sight leaves hit and miss markers visible beneath the selected target. */
internal fun DrawScope.drawTargetReticle(center: Offset, radius: Float, color: Color, stroke: Float) {
    fun sight(ink: Color, width: Float) {
        drawCircle(ink, radius, center, style = Stroke(width))
        for (direction in listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f))) {
            drawLine(ink, center + direction * (radius * .68f),
                center + direction * (radius * 1.36f), width)
        }
    }
    sight(Ocean.copy(alpha = .85f), stroke * 2.4f)
    sight(color, stroke)
}
