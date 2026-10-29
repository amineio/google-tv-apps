package com.amine.battleship

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.Player
import com.amine.battleship.ui.BattleshipApp
import com.amine.battleship.ui.BoardGeometry
import com.amine.battleship.ui.Cyan
import com.amine.battleship.ui.Brass
import com.amine.battleship.viewmodel.GameAction
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class TargetReticleTest {
    @get:Rule val screen = createComposeRule()

    @Test fun circularReticleMovesWithoutLeavingTargetFramesOnPreviousShots() {
        val game = GameEngine.attack(GameEngine.newGame(42), Player.HUMAN, Coordinate(0, 0)).state
        var state by mutableStateOf(SessionState(loaded = true, screen = Screen.GAME,
            game = game, selected = Coordinate(0, 0)))
        screen.setContent { BattleshipApp(state, { action ->
            if (action is GameAction.Move) state = state.copy(selected = Coordinate(0, 1))
        }, {}) }
        val board = screen.onNodeWithTag("targeting_board")
        board.assertIsFocused()
        val before = board.captureToImage().toPixelMap()
        val geometry = BoardGeometry.measure(before.width.toFloat(), compact = false)
        fun pixels(image: androidx.compose.ui.graphics.PixelMap, column: Int, cornerOnly: Boolean, color: Color): Int {
            val x0 = (geometry.originX + column * geometry.pitch).toInt()
            val y0 = geometry.originY.toInt()
            val edge = geometry.pitch.toInt()
            var count = 0
            for (y in y0 until y0 + edge) for (x in x0 until x0 + edge) {
                if (cornerOnly && !(x < x0 + edge / 7 && y < y0 + edge / 7)) continue
                val pixel = image[x, y]
                if (abs(pixel.red - color.red) < .04f && abs(pixel.green - color.green) < .04f &&
                    abs(pixel.blue - color.blue) < .04f) count++
            }
            return count
        }
        assertEquals("A circular scope must leave cell corners clear", 0, pixels(before, 0, true, Cyan))
        assertTrue("The scope must be visible on the selected shot", pixels(before, 0, false, Cyan) > 15)
        screen.onAllNodes(isRoot()).onLast().performKeyInput { pressKey(Key.DirectionRight) }
        val after = board.captureToImage().toPixelMap()
        board.assertContentDescriptionContains("Target B1.", substring = true)
        assertEquals("The target highlight must leave the old shot", 0, pixels(after, 0, false, Cyan))
        assertEquals("A previous shot must not retain a corner frame", 0, pixels(after, 0, false, Brass))
        assertTrue("The scope must follow directional input", pixels(after, 1, false, Cyan) > 15)
    }
}
