package com.amine.battleship

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
import com.amine.battleship.viewmodel.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Presentation fixtures use a frozen scheduler; no change to turn rules or production flags. */
class NativeWaterPresentationTest {
    @get:Rule val screen = createComposeRule()

    @Test fun staticSpriteImpactsStayOnTheirBoardAndRunForTheFullOutcomeDuration() {
        val clock = CaptureClock()
        var state by mutableStateOf(activeGame())
        val completed = mutableListOf<Long>()
        screen.setContent {
            CompositionLocalProvider(LocalBoardPresentation provides BoardPresentationEnvironment(clock)) {
                BattleshipApp(state, { action ->
                    if (action is GameAction.CompleteImpact) {
                        completed += action.id
                        if (state.activeImpact?.id == action.id) state = state.copy(activeImpact = null)
                    }
                }, {})
            }
        }
        var id = 0L
        for (board in listOf(ImpactBoard.ENEMY, ImpactBoard.FRIENDLY)) for (outcome in ShotOutcome.entries)
            for (coordinate in if (board == ImpactBoard.ENEMY && outcome == ShotOutcome.MISS)
                listOf(Coordinate(4, 4), Coordinate(0, 0), Coordinate(9, 9)) else listOf(Coordinate(4, 4))) {
            val targetTag = if (board == ImpactBoard.ENEMY) "targeting_board" else "friendly_board"
            val otherTag = if (board == ImpactBoard.ENEMY) "friendly_board" else "targeting_board"
            val game = fixtureGame(board, outcome, coordinate)
            screen.runOnIdle { state = state.copy(game = game, selected = coordinate) }
            val baseline = screen.onNodeWithTag(targetTag).captureToImage()
            val other = screen.onNodeWithTag(otherTag).captureToImage()
            val bounds = screen.onNodeWithTag(targetTag).getUnclippedBoundsInRoot()
            val acceptedAt = clock.nowNanos()
            val targetBoard = if (board == ImpactBoard.ENEMY) game.computerBoard else game.playerBoard
            val shot = targetBoard.shots.last()
            val event = ImpactPresentation(++id, board, coordinate, outcome, acceptedAt,
                sunkCells = shot.sunkCells, shipType = shot.shipType)
            screen.runOnIdle { state = state.copy(activeImpact = event) }
            val stages = when (outcome) {
                ShotOutcome.MISS -> listOf(160L, 500L, 1000L)
                ShotOutcome.HIT -> listOf(200L, 600L, 1200L)
                ShotOutcome.SUNK -> listOf(300L, 1000L, 2000L, 2499L)
            }
            for (elapsed in stages) {
                screen.runOnIdle { clock.advance(acceptedAt + elapsed * 1_000_000L) }
                screen.waitForIdle()
                assertTrue("$outcome must remain active at $elapsed ms", event.id !in completed)
                assertEquals(bounds, screen.onNodeWithTag(targetTag).getUnclippedBoundsInRoot())
                if (elapsed == stages.first()) assertTrue("The effect must alter its board",
                    changedPixels(baseline, screen.onNodeWithTag(targetTag).captureToImage()) > 12)
                if ((outcome == ShotOutcome.HIT && elapsed == 600L) || (outcome == ShotOutcome.SUNK && elapsed == 1000L)) {
                    val burning = screen.onNodeWithTag(targetTag).captureToImage()
                    assertTrue("$outcome needs visible warm fire at $elapsed ms",
                        warmFirePixels(burning) > warmFirePixels(baseline) + 16)
                }
                assertEquals(0, changedPixels(other, screen.onNodeWithTag(otherTag).captureToImage()))
                capture("${board.name}-${outcome.name}-${coordinate.label}-$elapsed")
            }
            val duration = when (outcome) { ShotOutcome.MISS -> 1100L; ShotOutcome.HIT -> 1500L; ShotOutcome.SUNK -> 2500L }
            screen.runOnIdle { clock.advance(acceptedAt + duration * 1_000_000L) }
            screen.waitForIdle()
            assertEquals(1, completed.count { it == event.id })
            assertEquals(0, changedPixels(baseline, screen.onNodeWithTag(targetTag).captureToImage()))
            capture("${board.name}-${outcome.name}-${coordinate.label}-completed")
        }
    }

    @Test fun localFixedBoardImpactsUsePublicProjectionsAndStayOnTheirOwnerForTheFullDuration() {
        val clock = CaptureClock()
        var state by mutableStateOf(activeGame())
        val completed = mutableListOf<Long>()
        screen.setContent {
            CompositionLocalProvider(LocalBoardPresentation provides BoardPresentationEnvironment(clock)) {
                BattleshipApp(state, { action ->
                    if (action is GameAction.CompleteImpact) {
                        completed += action.id
                        if (state.activeImpact?.id == action.id) state = state.copy(activeImpact = null)
                    }
                }, {})
            }
        }
        var id = 100L
        for (board in listOf(ImpactBoard.LEFT, ImpactBoard.RIGHT)) for (outcome in ShotOutcome.entries) {
            val coordinate = Coordinate(4, 4)
            val game = fixtureGame(board, outcome, coordinate)
            val target = if (board == ImpactBoard.LEFT) game.playerBoard else game.computerBoard
            assertEquals(if (outcome == ShotOutcome.SUNK) 1 else 0, GameEngine.publicBoard(target).ships.size)
            val targetTag = if (board == ImpactBoard.LEFT) "left_fleet_board" else "right_fleet_board"
            val otherTag = if (board == ImpactBoard.LEFT) "right_fleet_board" else "left_fleet_board"
            screen.runOnIdle { state = state.copy(game = game, selected = coordinate, resolving = true, activeImpact = null) }
            val baseline = screen.onNodeWithTag(targetTag).captureToImage()
            val other = screen.onNodeWithTag(otherTag).captureToImage()
            val bounds = screen.onNodeWithTag(targetTag).getUnclippedBoundsInRoot()
            val acceptedAt = clock.nowNanos()
            val shot = target.shots.last()
            val event = ImpactPresentation(++id, board, coordinate, outcome, acceptedAt,
                sunkCells = shot.sunkCells, shipType = shot.shipType)
            screen.runOnIdle { state = state.copy(activeImpact = event) }
            val stages = when (outcome) {
                ShotOutcome.MISS -> listOf(160L, 500L, 1000L)
                ShotOutcome.HIT -> listOf(200L, 600L, 1200L)
                ShotOutcome.SUNK -> listOf(300L, 1000L, 2000L, 2499L)
            }
            for (elapsed in stages) {
                screen.runOnIdle { clock.advance(acceptedAt + elapsed * 1_000_000L) }
                screen.waitForIdle()
                assertTrue("$board $outcome must remain active at $elapsed ms", event.id !in completed)
                assertEquals(bounds, screen.onNodeWithTag(targetTag).getUnclippedBoundsInRoot())
                if (elapsed == stages.first()) assertTrue("The local effect must alter its fixed board",
                    changedPixels(baseline, screen.onNodeWithTag(targetTag).captureToImage()) > 12)
                if ((outcome == ShotOutcome.HIT && elapsed == 600L) || (outcome == ShotOutcome.SUNK && elapsed == 1000L)) {
                    assertTrue("$board $outcome needs visible warm fire at $elapsed ms",
                        warmFirePixels(screen.onNodeWithTag(targetTag).captureToImage()) > warmFirePixels(baseline) + 16)
                }
                assertEquals(0, changedPixels(other, screen.onNodeWithTag(otherTag).captureToImage()))
            }
            val duration = when (outcome) { ShotOutcome.MISS -> 1100L; ShotOutcome.HIT -> 1500L; ShotOutcome.SUNK -> 2500L }
            screen.runOnIdle { clock.advance(acceptedAt + duration * 1_000_000L) }
            screen.waitForIdle()
            assertEquals(1, completed.count { it == event.id })
            assertEquals(0, changedPixels(baseline, screen.onNodeWithTag(targetTag).captureToImage()))
        }
    }

    @Test fun lifecycleAndPauseDiscardEffectsAndDirectionalInputKeepsBoardFocus() {
        val clock = CaptureClock()
        var state by mutableStateOf(activeGame())
        val completed = mutableListOf<Long>()
        lateinit var owner: CaptureLifecycleOwner
        screen.runOnIdle { owner = CaptureLifecycleOwner() }
        screen.setContent {
            CompositionLocalProvider(LocalBoardPresentation provides BoardPresentationEnvironment(clock),
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
        val initialEnemy = screen.onNodeWithTag("targeting_board").getUnclippedBoundsInRoot()
        val initialFriendly = screen.onNodeWithTag("friendly_board").getUnclippedBoundsInRoot()
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
        assertEquals(initialEnemy, screen.onNodeWithTag("targeting_board").getUnclippedBoundsInRoot())
        assertEquals(initialFriendly, screen.onNodeWithTag("friendly_board").getUnclippedBoundsInRoot())
        capture("pause-resume")
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
        assertEquals("A restored static board must not schedule animation frames", 0, clock.frameRequests)
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
        val enemy = target == ImpactBoard.ENEMY || target == ImpactBoard.RIGHT
        val fixture = if (enemy) game.copy(computerBoard = board) else game.copy(playerBoard = board)
        if (target == ImpactBoard.LEFT || target == ImpactBoard.RIGHT) {
            return fixture.copy(mode = GameMode.LOCAL,
                phase = if (target == ImpactBoard.RIGHT) GamePhase.COMPUTER_TURN else GamePhase.PLAYER_TURN)
        }
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

    /** Bright orange/yellow fire is distinct from the navy sea, grey hulls and persistent red marks. */
    private fun warmFirePixels(image: ImageBitmap): Int {
        val pixels = image.toPixelMap()
        var count = 0
        for (y in 0 until image.height) for (x in 0 until image.width) {
            val color = pixels[x, y]
            if (color.red > .85f && color.green > .32f && color.blue < .65f &&
                color.red > color.green * 1.2f && color.green > color.blue * 1.2f) count++
        }
        return count
    }

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("captureSprites") != "true") return
        require(name.matches(Regex("[A-Za-z0-9-]+")))
        screen.waitForIdle()
        // Gradle can uninstall the APK after testing. Shell-owned captures must survive that cleanup.
        val command = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "screencap -p /data/local/tmp/battleship-sprites-$name.png")
        ParcelFileDescriptor.AutoCloseInputStream(command).use { it.readBytes() }
    }

    private class CaptureClock : PresentationClock {
        private var now = 12_000_000_000L
        private val impactFrames = Channel<Long>(Channel.CONFLATED)
        var frameRequests = 0
            private set
        // Only finite accepted impacts may request frames.
        override fun nowNanos(): Long = now
        override suspend fun awaitFrame(continuous: Boolean): Long {
            check(!continuous) { "Static water must not request continuous frames" }
            frameRequests++
            return impactFrames.receive()
        }
        fun advance(nanos: Long) { now = nanos; impactFrames.trySend(nanos) }
    }

    private class CaptureLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }
}
