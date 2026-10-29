package com.amine.battleship.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.amine.battleship.R
import com.amine.battleship.localization.gameText

@Composable
internal fun TacticsScreen(onBack: () -> Unit) {
    val back = remember { FocusRequester() }
    LaunchedEffect(Unit) { withFrameNanos { }; back.requestFocus() }
    Column(Modifier.fillMaxSize().testTag("tactics_screen")) {
        Text(gameText(R.string.tactics_title), fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(2) { row ->
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(2) { column ->
                        TacticsCard(row * 2 + column, Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        NavalButton(gameText(R.string.back), Modifier.width(160.dp).focusRequester(back).testTag("tactics_back"), onClick = onBack)
    }
}

@Composable
private fun TacticsCard(kind: Int, modifier: Modifier) {
    val titles = listOf(R.string.tactics_checker_title, R.string.tactics_hit_title,
        R.string.tactics_three_title, R.string.tactics_water_title)
    val bodies = listOf(R.string.tactics_checker_body, R.string.tactics_hit_body,
        R.string.tactics_three_body, R.string.tactics_water_body)
    Row(modifier.fillMaxHeight().background(DeepWater.copy(alpha = .85f), RoundedCornerShape(10.dp))
        .padding(14.dp).testTag("tactics_card_$kind"), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        TacticsDiagram(kind, Modifier.width(112.dp).fillMaxHeight())
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(gameText(titles[kind]), fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("tactics_title_$kind"))
            Text(gameText(bodies[kind]), fontSize = 16.sp, lineHeight = 20.sp, color = Muted,
                modifier = Modifier.testTag("tactics_body_$kind"))
        }
    }
}

@Composable
private fun TacticsDiagram(kind: Int, modifier: Modifier) {
    Canvas(modifier.testTag("tactics_diagram_$kind")) {
        val side = minOf(size.width, size.height - if (kind == 2) 22.dp.toPx() else 0f)
        val count = if (kind == 2) 6 else 5
        val pitch = side / count
        val origin = Offset((size.width - side) / 2f, (size.height - side) / 2f + if (kind == 2) 11.dp.toPx() else 0f)
        fun center(row: Int, column: Int) = origin + Offset((column + .5f) * pitch, (row + .5f) * pitch)
        drawRect(Ocean, origin, Size(side, side))
        for (line in 0..count) {
            drawLine(Steel.copy(alpha = .32f), origin + Offset(line * pitch, 0f), origin + Offset(line * pitch, side), 1.dp.toPx())
            drawLine(Steel.copy(alpha = .32f), origin + Offset(0f, line * pitch), origin + Offset(side, line * pitch), 1.dp.toPx())
        }
        // Cyan dots are search candidates. Red crosses and pale rings are previous shots.
        fun candidate(row: Int, column: Int) = drawCircle(Cyan, pitch * .14f, center(row, column))
        fun hit(row: Int, column: Int) = drawTacticsHit(center(row, column), pitch * .19f)
        fun miss(row: Int, column: Int) = drawCircle(Ink, pitch * .19f, center(row, column), style = Stroke(1.6.dp.toPx()))
        when (kind) {
            0 -> for (row in 0 until count) for (column in 0 until count) if ((row + column) % 2 == 0) candidate(row, column)
            1 -> {
                hit(2, 2)
                listOf(1 to 2, 3 to 2, 2 to 1, 2 to 3).forEach { (row, column) -> candidate(row, column) }
            }
            2 -> {
                for (row in 0 until count) for (column in 0 until count) if ((row + column) % 3 == 0) candidate(row, column)
                val y = origin.y - 13.dp.toPx()
                val x = size.width / 2f
                drawRoundRect(Steel.copy(alpha = .7f), Offset(x - pitch, y - pitch * .3f),
                    Size(pitch * 2, pitch * .6f), androidx.compose.ui.geometry.CornerRadius(pitch * .2f))
                drawTacticsHit(Offset(x - pitch * .5f, y), pitch * .2f)
                drawTacticsHit(Offset(x + pitch * .5f, y), pitch * .2f)
            }
            3 -> {
                hit(4, 0); hit(4, 1); miss(1, 0); miss(3, 1)
                drawTargetReticle(center(1, 3), pitch * .32f, Cyan, 1.8.dp.toPx())
            }
        }
    }
}

private fun DrawScope.drawTacticsHit(center: Offset, arm: Float) {
    drawLine(Impact, center - Offset(arm, arm), center + Offset(arm, arm), 2.dp.toPx())
    drawLine(Impact, center + Offset(-arm, arm), center + Offset(arm, -arm), 2.dp.toPx())
}
