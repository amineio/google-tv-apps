package com.amine.battleship

import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.platform.app.InstrumentationRegistry
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import com.amine.battleship.ui.BattleshipApp
import com.amine.battleship.ui.BoardPresentationEnvironment
import com.amine.battleship.ui.LocalBoardPresentation
import com.amine.battleship.ui.PresentationClock
import com.amine.battleship.ui.WaterRenderer
import com.amine.battleship.viewmodel.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Presentation fixtures use a frozen scheduler; no change to turn rules or production flags. */
class NativeWaterPresentationTest {
    @get:Rule val screen = createComposeRule()

    @Test fun impactsChangeOnlyTheirTargetBoardAndExpireForBothRenderers() {
        val clock = CaptureClock()
        var renderer by mutableStateOf(WaterRenderer.AUTOMATIC)
        var state by mutableStateOf(activeGame())
        val completed = mutableListOf<Long>()
        screen.setContent {
            CompositionLocalProvider(LocalBoardPresentation provides BoardPresentationEnvironment(clock, renderer)) {
                BattleshipApp(state, { action ->
                    if (action is GameAction.CompleteImpact) {
                        completed += action.id
                        if (state.activeImpact?.id == action.id) state = state.copy(activeImpact = null)
                    }
                }, {})
            }
        }
        var id = 0L
        var shaderWater: ImageBitmap? = null
        for (choice in WaterRenderer.entries) {
            screen.runOnIdle { renderer = choice }
            for (board in ImpactBoard.entries) for (outcome in ShotOutcome.entries)
                for (coordinate in if (choice == WaterRenderer.AUTOMATIC && board == ImpactBoard.ENEMY)
                    listOf(Coordinate(0, 0), Coordinate(9, 9), Coordinate(4, 4))
                    else listOf(Coordinate(0, 0), Coordinate(9, 9))) {
                val targetTag = if (board == ImpactBoard.ENEMY) "targeting_board" else "friendly_board"
                val otherTag = if (board == ImpactBoard.ENEMY) "friendly_board" else "targeting_board"
                screen.runOnIdle { state = state.copy(game = fixtureGame(board, outcome, coordinate), selected = coordinate) }
                val baseline = screen.onNodeWithTag(targetTag).captureToImage()
                if (board == ImpactBoard.ENEMY && outcome == ShotOutcome.MISS && coordinate == Coordinate(0, 0)) {
                    if (choice == WaterRenderer.AUTOMATIC) shaderWater = baseline
                    else if (Build.VERSION.SDK_INT >= 33) assertTrue(
                        "Automatic mode must render AGSL rather than silently use the Canvas fallback",
                        changedPixels(shaderWater!!, baseline) > 12)
                }
                val other = screen.onNodeWithTag(otherTag).captureToImage()
                val acceptedAt = clock.nowNanos()
                val event = ImpactPresentation(++id, board, coordinate, outcome, acceptedAt)
                screen.runOnIdle { state = state.copy(activeImpact = event) }
                capture("${choice.name}-${board.name}-${outcome.name}-${coordinate.label}-start")
                screen.runOnIdle { clock.advance(acceptedAt + 120_000_000L) }
                screen.waitForIdle()
                val peak = screen.onNodeWithTag(targetTag).captureToImage()
                assertTrue("The effect must alter the target board", changedPixels(baseline, peak) > 12)
                // Water is frozen here. A wrong board filter changes this exact comparison.
                assertEquals(0, changedPixels(other, screen.onNodeWithTag(otherTag).captureToImage()))
                assertTrue("The effect must remain active at 120 ms", event.id !in completed)
                capture("${choice.name}-${board.name}-${outcome.name}-${coordinate.label}-peak")
                screen.runOnIdle { clock.advance(acceptedAt + 480_000_000L) }
                screen.waitForIdle()
                assertEquals(1, completed.count { it == event.id })
                assertEquals(0, changedPixels(baseline, screen.onNodeWithTag(targetTag).captureToImage()))
                capture("${choice.name}-${board.name}-${outcome.name}-${coordinate.label}-completed")
            }
        }
    }

    @Test fun lifecycleAndPauseDiscardEffectsAndDirectionalInputKeepsBoardFocus() {
        val clock = CaptureClock()
        var state by mutableStateOf(activeGame())
        val completed = mutableListOf<Long>()
        lateinit var owner: CaptureLifecycleOwner
        screen.runOnIdle { owner = CaptureLifecycleOwner() }
        screen.setContent {
            CompositionLocalProvider(LocalBoardPresentation provides BoardPresentationEnvironment(clock, WaterRenderer.CANVAS),
                LocalLifecycleOwner provides owner) {
                BattleshipApp(state, { action ->
                    when (action) {
                        is GameAction.Move -> state = state.copy(selected = Coordinate(
                            (state.selected.row + action.rowDelta).coerceIn(0, 9),
                            (state.selected.column + action.columnDelta).coerceIn(0, 9)))
                        is GameAction.CompleteImpact -> {
                            completed += action.id
                            if (state.activeImpact?.id == action.id) state = state.copy(activeImpact = null)
                        }
                        GameAction.Resume -> state = state.copy(paused = false)
                        else -> Unit
                    }
                }, {})
            }
        }
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        key(Key.DirectionRight)
        key(Key.DirectionDown)
        screen.onNodeWithTag("targeting_board").assertContentDescriptionContains("Target B2.", substring = true)
        screen.runOnIdle { state = state.copy(activeImpact = ImpactPresentation(71, ImpactBoard.ENEMY,
            Coordinate(1, 1), ShotOutcome.HIT, clock.nowNanos())) }
        screen.runOnIdle { owner.registry.currentState = Lifecycle.State.STARTED }
        screen.waitForIdle()
        assertEquals(listOf(71L), completed)
        screen.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        screen.runOnIdle { state = state.copy(activeImpact = ImpactPresentation(72, ImpactBoard.FRIENDLY,
            Coordinate(9, 9), ShotOutcome.MISS, clock.nowNanos())) }
        screen.runOnIdle { state = state.copy(paused = true) }
        screen.onNodeWithText("Resume").assertIsFocused()
        assertEquals(listOf(71L, 72L), completed)
        key(Key.DirectionCenter)
        screen.onNodeWithTag("targeting_board").assertIsFocused()
        screen.onNodeWithTag("targeting_board").assertContentDescriptionContains("Target B2.", substring = true)
        screen.runOnIdle { clock.advance(clock.nowNanos() + 1_000_000_000L) }
        assertEquals(listOf(71L, 72L), completed)
    }

    @Test fun restoredShotHistoryDoesNotRequestImpactCompletion() {
        val clock = CaptureClock()
        val actions = mutableListOf<GameAction>()
        val state = activeGame().copy(game = fixtureGame(ImpactBoard.ENEMY, ShotOutcome.SUNK, Coordinate(0, 0)))
        screen.setContent {
            CompositionLocalProvider(LocalBoardPresentation provides BoardPresentationEnvironment(clock)) {
                BattleshipApp(state, actions::add, {})
            }
        }
        screen.runOnIdle { clock.advance(clock.nowNanos() + 1_000_000_000L) }
        screen.waitForIdle()
        assertTrue(actions.none { it is GameAction.CompleteImpact })
        screen.onNodeWithTag("targeting_board").assertIsFocused()
    }

    private fun activeGame() = SessionState(loaded = true, screen = Screen.GAME,
        game = GameEngine.newGame(992L), selected = Coordinate(0, 0), showHint = false)

    private fun fixtureGame(target: ImpactBoard, outcome: ShotOutcome, at: Coordinate): GameState {
        val game = GameEngine.newGame(992L)
        val neighbor = Coordinate(at.row, at.column + if (at.column == 9) -1 else 1)
        val ship = Ship(ShipType.DESTROYER, listOf(at, neighbor))
        val priorHits = if (outcome == ShotOutcome.SUNK) listOf(Shot(neighbor, ShotOutcome.HIT, ShipType.DESTROYER))
            else emptyList()
        val marks = priorHits + listOf(Shot(at, outcome, if (outcome == ShotOutcome.MISS) null else ShipType.DESTROYER,
            if (outcome == ShotOutcome.SUNK) ship.cells else emptyList()))
        val dense = (2..8).flatMap { row -> (2..8).map { column -> Shot(Coordinate(row, column),
            if ((row + column) % 3 == 0) ShotOutcome.HIT else ShotOutcome.MISS) } }
            .filter { it.coordinate != at && it.coordinate != neighbor }
            .filter { shot -> marks.none { it.coordinate == shot.coordinate } }
        val board = Board(if (outcome == ShotOutcome.MISS) emptyList() else listOf(ship), dense + marks)
        val fixture = if (target == ImpactBoard.ENEMY) game.copy(computerBoard = board) else game.copy(playerBoard = board)
        if (target == ImpactBoard.ENEMY) {
            assertEquals("Only a sunk fixture can reveal an enemy ship",
                if (outcome == ShotOutcome.SUNK) 1 else 0, GameEngine.playerView(fixture).opponentBoard.ships.size)
        }
        return fixture
    }

    private fun key(key: Key) { screen.onAllNodes(isRoot()).onLast().performKeyInput { pressKey(key) } }

    private fun changedPixels(a: ImageBitmap, b: ImageBitmap): Int {
        assertEquals(a.width, b.width)
        assertEquals(a.height, b.height)
        val left = a.toPixelMap()
        val right = b.toPixelMap()
        var changed = 0
        for (y in 0 until a.height) for (x in 0 until a.width) if (left[x, y] != right[x, y]) changed++
        return changed
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureWater") != "true") return
        require(name.matches(Regex("[A-Za-z0-9-]+")))
        screen.waitForIdle()
        // Gradle can uninstall the APK after testing. Shell-owned captures must survive that cleanup.
        val command = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "screencap -p /data/local/tmp/battleship-water-$name.png")
        ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
    }

    private class CaptureClock : PresentationClock {
        private var now = 12_000_000_000L
        private val impactFrames = Channel<Long>(Channel.CONFLATED)
        // Continuous water intentionally stays frozen for pixel comparisons and repeatable captures.
        override fun nowNanos(): Long = now
        override suspend fun awaitFrame(continuous: Boolean): Long =
            if (continuous) kotlinx.coroutines.awaitCancellation() else impactFrames.receive()
        fun advance(nanos: Long) { now = nanos; impactFrames.trySend(nanos) }
    }

    private class CaptureLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }
}
