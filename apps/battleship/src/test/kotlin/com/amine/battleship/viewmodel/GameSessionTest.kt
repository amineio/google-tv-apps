package com.amine.battleship.viewmodel

import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.GamePhase
import com.amine.battleship.persistence.GameSaveRepository
import com.amine.battleship.persistence.SavedSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class GameSessionTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun pauseCancelsThePendingComputerShotAndResumeAppliesItOnce() = runTest {
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), aiDelayMs = 500, seedSource = { 12L }, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        assertEquals(GamePhase.COMPUTER_TURN, session.state.value.game!!.phase)
        session.action(GameAction.Pause)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(0, session.state.value.game!!.playerBoard.shots.size)
        session.action(GameAction.Resume)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(1, session.state.value.game!!.playerBoard.shots.size)
        assertEquals(GamePhase.PLAYER_TURN, session.state.value.game!!.phase)
        assertEquals(1, session.state.value.game!!.computerBoard.shots.size)
    }

    @Test fun repeatedFireCannotApplyAnotherShotDuringResolution() = runTest {
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), aiDelayMs = 500, seedSource = { 13L }, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        session.action(GameAction.Play)
        repeat(30) { session.action(GameAction.Fire) }
        assertEquals(1, session.state.value.game!!.computerBoard.shots.size)
        advanceTimeBy(1_000)
        runCurrent()
        session.action(GameAction.Fire)
        assertEquals(1, session.state.value.game!!.computerBoard.shots.size)
        assertEquals(GamePhase.PLAYER_TURN, session.state.value.game!!.phase)
    }

    @Test fun restartCannotReceiveTheOldComputerShot() = runTest {
        var seed = 15L
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), aiDelayMs = 500, seedSource = { seed++ }, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        session.action(GameAction.Pause)
        session.action(GameAction.Restart)
        assertEquals(Confirmation.RESTART, session.state.value.confirmation)
        session.action(GameAction.Confirm)
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(session.state.value.game!!.playerBoard.shots.isEmpty())
        assertTrue(session.state.value.game!!.computerBoard.shots.isEmpty())
    }

    @Test fun backgroundStopsTheMatchUntilThePlayerResumes() = runTest {
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), aiDelayMs = 500, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        session.background()
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(session.state.value.paused)
        assertTrue(session.state.value.game!!.playerBoard.shots.isEmpty())
    }

    @Test fun restoredComputerTurnWaitsForContinueThenFiresOnce() = runTest {
        val repository = GameSaveRepository(folder.newFile())
        val game = GameEngine.newGame(19)
        val pending = GameEngine.attack(game, com.amine.battleship.game.model.Player.HUMAN,
            com.amine.battleship.game.model.Coordinate(0, 0)).state
        repository.save(SavedSession(game = pending))
        val session = GameSession(backgroundScope, repository, aiDelayMs = 500, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        assertEquals(Screen.HOME, session.state.value.screen)
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(session.state.value.game!!.playerBoard.shots.isEmpty())
        session.action(GameAction.Continue)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(1, session.state.value.game!!.playerBoard.shots.size)
    }
}
