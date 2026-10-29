package com.amine.battleship.viewmodel

import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.GamePhase
import com.amine.battleship.game.model.ShotOutcome
import com.amine.battleship.game.model.ShipType
import com.amine.battleship.persistence.GameSaveRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GameSessionImpactTest {
    @get:Rule val folder = TemporaryFolder()

    private fun TestScope.session(file: File = folder.newFile(), clock: () -> Long = { 123_456L }) =
        GameSession(backgroundScope, GameSaveRepository(file), aiDelayMs = 850,
            seedSource = { 42L }, ioDispatcher = StandardTestDispatcher(testScheduler), nanoTimeSource = clock)

    private fun GameSession.aim(target: Coordinate) {
        val selected = state.value.selected
        action(GameAction.Move(target.row - selected.row, target.column - selected.column))
    }

    @Test fun acceptedHumanMissUsesEnemyBoardAndInjectedStartTime() = runTest {
        val session = session(clock = { 987_654L })
        runCurrent()
        session.action(GameAction.Play)
        val shipCells = session.state.value.game!!.computerBoard.ships.flatMap { it.cells }.toSet()
        val water = (0..9).flatMap { row -> (0..9).map { Coordinate(row, it) } }.first { it !in shipCells }
        session.aim(water)
        session.action(GameAction.Fire)
        val impact = requireNotNull(session.state.value.activeImpact)
        assertEquals(ImpactBoard.ENEMY, impact.board)
        assertEquals(water, impact.coordinate)
        assertEquals(ShotOutcome.MISS, impact.outcome)
        assertEquals(987_654L, impact.startedAtNanos)
        assertEquals(1, session.state.value.game!!.computerBoard.shots.size)
    }

    @Test fun acceptedHitsAndSinkingKeepTheirDistinctOutcomes() = runTest {
        val session = session()
        runCurrent()
        session.action(GameAction.Play)
        val cells = session.state.value.game!!.computerBoard.ships.first { it.type == ShipType.DESTROYER }.cells
        session.aim(cells.first())
        session.action(GameAction.Fire)
        assertEquals(ShotOutcome.HIT, requireNotNull(session.state.value.activeImpact).outcome)
        advanceTimeBy(1_351)
        runCurrent()
        session.aim(cells.last())
        session.action(GameAction.Fire)
        assertEquals(ShotOutcome.SUNK, requireNotNull(session.state.value.activeImpact).outcome)
    }

    @Test fun acceptedComputerShotUsesFriendlyBoardAndDoesNotChangeTurnTiming() = runTest {
        var now = 100L
        val session = session(clock = { now })
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        val human = requireNotNull(session.state.value.activeImpact)
        now = 200L
        advanceTimeBy(849)
        runCurrent()
        assertTrue(session.state.value.game!!.playerBoard.shots.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        val computer = requireNotNull(session.state.value.activeImpact)
        val shot = session.state.value.game!!.playerBoard.shots.single()
        assertEquals(ImpactBoard.FRIENDLY, computer.board)
        assertEquals(shot.coordinate, computer.coordinate)
        assertEquals(shot.outcome, computer.outcome)
        assertEquals(200L, computer.startedAtNanos)
        assertTrue(computer.id > human.id)
        assertTrue(session.state.value.resolving)
        advanceTimeBy(499)
        runCurrent()
        assertTrue(session.state.value.resolving)
        advanceTimeBy(1)
        runCurrent()
        assertFalse(session.state.value.resolving)
    }

    @Test fun rejectedFireDoesNotCreateAnImpactOrReadTheClock() = runTest {
        var clockReads = 0
        val session = session(clock = { (++clockReads).toLong() })
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        val first = requireNotNull(session.state.value.activeImpact)
        session.action(GameAction.Fire)
        assertEquals(first, session.state.value.activeImpact)
        assertEquals(1, clockReads)
        advanceTimeBy(1_351)
        runCurrent()
        session.action(GameAction.CompleteImpact(requireNotNull(session.state.value.activeImpact).id))
        session.action(GameAction.Fire)
        assertNull(session.state.value.activeImpact)
        assertEquals(2, clockReads)
        assertEquals(1, session.state.value.game!!.computerBoard.shots.size)
    }

    @Test fun matchingCompletionClearsOnlyTheImpactWithoutSoundSaveOrMove() = runTest {
        val file = folder.newFile()
        val session = session(file)
        val sounds = mutableListOf<SoundCue>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.sounds.collect { sounds += it } }
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        runCurrent()
        val impact = requireNotNull(session.state.value.activeImpact)
        val before = session.state.value
        sounds.clear()
        // A real write would replace this marker, even if its snapshot bytes were unchanged.
        file.writeText("no save expected")
        session.action(GameAction.CompleteImpact(impact.id))
        runCurrent()
        assertEquals(before.copy(activeImpact = null), session.state.value)
        assertTrue(sounds.isEmpty())
        assertEquals("no save expected", file.readText())
    }

    @Test fun staleCompletionCannotClearANewerComputerImpact() = runTest {
        val session = session()
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        val old = requireNotNull(session.state.value.activeImpact)
        advanceTimeBy(850)
        runCurrent()
        val current = requireNotNull(session.state.value.activeImpact)
        session.action(GameAction.CompleteImpact(old.id))
        assertEquals(current, session.state.value.activeImpact)
        session.action(GameAction.CompleteImpact(current.id))
        assertNull(session.state.value.activeImpact)
    }

    @Test fun restartDoesNotReuseIdsOrAcceptOldCompletion() = runTest {
        val session = session()
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        val old = requireNotNull(session.state.value.activeImpact)
        session.action(GameAction.Restart)
        assertNull(session.state.value.activeImpact)
        session.action(GameAction.Confirm)
        assertNull(session.state.value.activeImpact)
        session.action(GameAction.Fire)
        val current = requireNotNull(session.state.value.activeImpact)
        assertTrue(current.id > old.id)
        session.action(GameAction.CompleteImpact(old.id))
        assertEquals(current, session.state.value.activeImpact)
    }

    @Test fun gameplayInterruptionsDiscardImpactsAndCancelDoesNotReplayThem() = runTest {
        val interruptions: List<(GameSession) -> Unit> = listOf(
            { it.action(GameAction.Pause) },
            { it.background() },
            { it.action(GameAction.Restart) },
            { it.action(GameAction.Settings) },
            { it.action(GameAction.Home) },
            { it.action(GameAction.Play) },
        )
        for (interrupt in interruptions) {
            val session = session()
            runCurrent()
            session.action(GameAction.Play)
            session.action(GameAction.Fire)
            assertNotNull(session.state.value.activeImpact)
            interrupt(session)
            assertNull(session.state.value.activeImpact)
            session.action(GameAction.Cancel)
            assertNull(session.state.value.activeImpact)
        }
    }

    @Test fun homeContinueAndSaveRestorationNeverReplayShotHistory() = runTest {
        val file = folder.newFile()
        val session = session(file)
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        assertNotNull(session.state.value.activeImpact)
        session.action(GameAction.Home)
        session.action(GameAction.Confirm)
        runCurrent()
        assertEquals(Screen.HOME, session.state.value.screen)
        assertNull(session.state.value.activeImpact)
        assertFalse(file.readText().contains("activeImpact"))
        assertFalse(file.readText().contains("startedAtNanos"))
        val restored = session(file)
        runCurrent()
        assertNull(restored.state.value.activeImpact)
        restored.action(GameAction.Continue)
        assertEquals(Screen.GAME, restored.state.value.screen)
        assertNull(restored.state.value.activeImpact)
        advanceTimeBy(850)
        runCurrent()
        assertEquals(ImpactBoard.FRIENDLY, requireNotNull(restored.state.value.activeImpact).board)
    }

    @Test fun resultsDiscardTheFinalImpact() = runTest {
        val session = session()
        runCurrent()
        session.action(GameAction.Play)
        val targets = session.state.value.game!!.computerBoard.ships.flatMap { it.cells }
        for (target in targets) {
            session.aim(target)
            session.action(GameAction.Fire)
            assertNotNull(session.state.value.activeImpact)
            advanceTimeBy(2_000)
            runCurrent()
        }
        assertEquals(GamePhase.GAME_OVER, session.state.value.game!!.phase)
        assertEquals(Screen.RESULTS, session.state.value.screen)
        assertNull(session.state.value.activeImpact)
    }
}
