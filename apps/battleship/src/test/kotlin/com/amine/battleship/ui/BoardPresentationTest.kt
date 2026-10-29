package com.amine.battleship.ui

import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.GamePhase
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.viewmodel.Confirmation
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardPresentationTest {
    @Test fun enemyGeometryAlignsCornerImpactsWithGridCellCenters() {
        val board = BoardGeometry.measure(1000f, compact = false)
        assertEquals(67f, board.originX, .001f)
        assertEquals(67f, board.originY, .001f)
        assertEquals(91.826f, board.pitch, .001f)
        assertEquals(board.originX + board.pitch * .5f, board.center(Coordinate(0, 0)).x, .001f)
        assertEquals(board.originY + board.pitch * 9.5f, board.center(Coordinate(9, 9)).y, .001f)
        assertEquals(board.originX + board.pitch * 10f, board.right, .001f)
        assertEquals(board.originY + board.pitch * 10f, board.bottom, .001f)
    }

    @Test fun friendlyGeometryPreservesItsWiderLabelGutter() {
        val board = BoardGeometry.measure(400f, compact = true)
        assertEquals(40f, board.originX, .001f)
        assertEquals(35.12f, board.pitch, .001f)
        assertTrue(board.center(Coordinate(9, 9)).x < board.right)
    }

    @Test fun impactProgressUsesAcceptedTimestampAndEndsAt480Milliseconds() {
        val accepted = 12_000_000_000L
        assertEquals(0f, impactProgress(accepted, accepted - 10L), 0f)
        assertEquals(0f, impactProgress(accepted, accepted), 0f)
        assertEquals(.25f, impactProgress(accepted, accepted + 120_000_000L), .00001f)
        assertEquals(.5f, impactProgress(accepted, accepted + 240_000_000L), .00001f)
        assertEquals(1f, impactProgress(accepted, accepted + 480_000_000L), 0f)
        assertEquals(1f, impactProgress(accepted, accepted + 2_000_000_000L), 0f)
    }

    @Test fun presentationClockRunsOnlyForResumedUnobstructedGameplay() {
        val active = SessionState(screen = Screen.GAME, game = GameEngine.newGame(321L))
        assertTrue(presentationEnabled(active, resumed = true))
        assertFalse(presentationEnabled(active, resumed = false))
        assertFalse(presentationEnabled(active.copy(paused = true), resumed = true))
        assertFalse(presentationEnabled(active.copy(confirmation = Confirmation.RESTART), resumed = true))
        assertFalse(presentationEnabled(active.copy(screen = Screen.HOME), resumed = true))
        assertFalse(presentationEnabled(active.copy(screen = Screen.RESULTS), resumed = true))
        val finalFeedback = active.copy(game = active.game!!.copy(phase = GamePhase.GAME_OVER), resolving = true)
        assertTrue(presentationEnabled(finalFeedback, resumed = true))
        assertFalse(presentationEnabled(finalFeedback.copy(resolving = false), resumed = true))
        assertFalse(presentationEnabled(active.copy(game = null), resumed = true))
    }
}
