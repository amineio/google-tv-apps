package com.amine.battleship.viewmodel

import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import com.amine.battleship.persistence.GameSaveRepository
import com.amine.battleship.persistence.SavedSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class OutcomeTimingTest {
    @get:Rule val folder = TemporaryFolder()

    private fun TestScope.fixture(outcome: ShotOutcome, final: Boolean = false): Pair<GameSession, Coordinate> {
        val game = GameEngine.newGame(42)
        val hull = game.computerBoard.ships.first { it.type == ShipType.DESTROYER }
        val target = if (outcome == ShotOutcome.MISS) (0..9).flatMap { r -> (0..9).map { Coordinate(r, it) } }
            .first { cell -> game.computerBoard.ships.none { cell in it.cells } } else hull.cells.last()
        val damagedCells = when {
            final -> game.computerBoard.ships.flatMap { it.cells }.filter { it != target }
            outcome == ShotOutcome.SUNK -> hull.cells.dropLast(1)
            else -> emptyList()
        }
        var prepared = game
        val friendlyWater = (0..9).flatMap { r -> (0..9).map { Coordinate(r, it) } }
            .filter { cell -> game.playerBoard.ships.none { cell in it.cells } }
        for ((index, cell) in damagedCells.withIndex()) {
            prepared = GameEngine.attack(prepared, Player.HUMAN, cell).state
            prepared = GameEngine.attack(prepared, Player.COMPUTER, friendlyWater[index]).state
        }
        val repository = GameSaveRepository(folder.newFile())
        repository.save(SavedSession(game = prepared))
        val session = GameSession(backgroundScope, repository, seedSource = { 42 },
            ioDispatcher = StandardTestDispatcher(testScheduler), nanoTimeSource = { testScheduler.currentTime * 1_000_000L })
        runCurrent()
        session.action(GameAction.Continue)
        session.action(GameAction.Move(target.row, target.column))
        return session to target
    }

    @Test fun everyOutcomeFinishesBeforeTheComputerCanReplaceIt() = runTest {
        for ((outcome, duration) in listOf(ShotOutcome.MISS to 1100L, ShotOutcome.HIT to 1500L, ShotOutcome.SUNK to 2500L)) {
            val (session, _) = fixture(outcome)
            session.action(GameAction.Fire)
            val incomingBefore = session.state.value.game!!.playerBoard.shots.size
            val impact = requireNotNull(session.state.value.activeImpact)
            assertEquals(outcome, impact.outcome)
            advanceTimeBy(duration - 1)
            runCurrent()
            assertEquals("$outcome must retain its own effect", impact, session.state.value.activeImpact)
            assertEquals(incomingBefore, session.state.value.game!!.playerBoard.shots.size)
            session.action(GameAction.Fire)
            assertEquals(1, session.state.value.game!!.computerBoard.shots.count { it.coordinate == impact.coordinate })
            advanceTimeBy(1)
            runCurrent()
            assertEquals(incomingBefore + 1, session.state.value.game!!.playerBoard.shots.size)
            session.action(GameAction.Pause)
        }
    }

    @Test fun finalSinkingHoldsResultsAndPlaysSunkBeforeVictory() = runTest {
        val (session, _) = fixture(ShotOutcome.SUNK, final = true)
        val sounds = mutableListOf<SoundCue>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.sounds.collect { sounds += it } }
        // Continue's buffered selection cue predates the final-shot sequence under test.
        sounds.clear()
        session.action(GameAction.Fire)
        runCurrent()
        assertEquals(SoundCue.SUNK, sounds.last())
        assertFalse(sounds.contains(SoundCue.VICTORY))
        advanceTimeBy(2499)
        runCurrent()
        assertEquals(Screen.GAME, session.state.value.screen)
        assertEquals(ShotOutcome.SUNK, session.state.value.activeImpact!!.outcome)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(Screen.RESULTS, session.state.value.screen)
        assertEquals(listOf(SoundCue.FIRE, SoundCue.SUNK, SoundCue.VICTORY), sounds)
    }

    @Test fun pauseAndMuteStopAnExtendedCueWithoutReplayingIt() = runTest {
        val (session, _) = fixture(ShotOutcome.SUNK)
        val sounds = mutableListOf<SoundCue>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.sounds.collect { sounds += it } }
        sounds.clear()
        session.action(GameAction.Fire)
        assertEquals(SoundCue.SUNK, sounds.last())
        session.action(GameAction.Pause)
        assertEquals(SoundCue.STOP, sounds.last())
        session.action(GameAction.Resume)
        sounds.clear()
        session.action(GameAction.ToggleSound)
        assertEquals(listOf(SoundCue.STOP), sounds)
        advanceTimeBy(6000)
        runCurrent()
        assertEquals(listOf(SoundCue.STOP), sounds)
    }

    @Test fun pauseAndResumeDoNotReplayTheCanceledSinking() = runTest {
        val (session, _) = fixture(ShotOutcome.SUNK)
        session.action(GameAction.Fire)
        val old = session.state.value.activeImpact!!
        val incomingBefore = session.state.value.game!!.playerBoard.shots.size
        advanceTimeBy(300)
        session.background()
        advanceTimeBy(4000)
        runCurrent()
        assertNull(session.state.value.activeImpact)
        assertEquals(incomingBefore, session.state.value.game!!.playerBoard.shots.size)
        session.action(GameAction.Resume)
        advanceTimeBy(850)
        runCurrent()
        val incoming = requireNotNull(session.state.value.activeImpact)
        assertEquals(ImpactBoard.FRIENDLY, incoming.board)
        assertNotEquals(old.id, incoming.id)
        session.action(GameAction.CompleteImpact(old.id))
        assertEquals(incoming, session.state.value.activeImpact)
    }
}
