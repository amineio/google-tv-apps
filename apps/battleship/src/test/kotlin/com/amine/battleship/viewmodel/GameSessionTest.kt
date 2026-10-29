package com.amine.battleship.viewmodel

import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.GamePhase
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.Player
import com.amine.battleship.game.model.ShipType
import com.amine.battleship.game.ai.AttackKnowledge
import com.amine.battleship.game.ai.HuntTargetStrategy
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
class GameSessionTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun openingPlaysOnlyForAcceptedNewMatches() = runTest {
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), ioDispatcher = StandardTestDispatcher(testScheduler))
        val cues = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.sounds.collect { cues += it.name } }
        runCurrent()
        session.action(GameAction.Play)
        assertEquals(1, cues.count { it == "MATCH_START" })
        session.action(GameAction.Pause)
        session.action(GameAction.Resume)
        session.action(GameAction.Restart)
        session.action(GameAction.Cancel)
        session.action(GameAction.Home)
        session.action(GameAction.Confirm)
        session.action(GameAction.Continue)
        assertEquals(1, cues.count { it == "MATCH_START" })
        session.action(GameAction.Restart)
        session.action(GameAction.Confirm)
        assertEquals(2, cues.count { it == "MATCH_START" })
    }

    @Test fun restoredMatchDoesNotEmitOpeningWhenContinued() = runTest {
        val repository = GameSaveRepository(folder.newFile())
        repository.save(SavedSession(game = GameEngine.newGame(42)))
        val session = GameSession(backgroundScope, repository, ioDispatcher = StandardTestDispatcher(testScheduler))
        val cues = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.sounds.collect { cues += it.name } }
        runCurrent()
        session.action(GameAction.Continue)
        assertFalse(cues.contains("MATCH_START"))
    }

    @Test fun hitFeedbackIsConciseAndDoesNotRepeatTheCoordinate() = runTest {
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), seedSource = { 42L }, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        session.action(GameAction.Play)
        val target = session.state.value.game!!.computerBoard.ships.first().cells.first()
        session.action(GameAction.Move(target.row, target.column))
        session.action(GameAction.Fire)
        assertEquals("Hit", session.state.value.message)
    }

    @Test fun repeatedShotWarningClearsWhenThePlayerAimsElsewhere() = runTest {
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), seedSource = { 42L }, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        advanceTimeBy(6_000)
        runCurrent()
        session.action(GameAction.Fire)
        assertEquals("Already targeted", session.state.value.message)
        session.action(GameAction.Move(0, 1))
        assertEquals("", session.state.value.message)
    }

    @Test fun namedSunkFeedbackSurvivesAimingDuringComputerTurn() = runTest {
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), seedSource = { 42L }, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        session.action(GameAction.Play)
        val targets = session.state.value.game!!.computerBoard.ships.first { it.type == ShipType.DESTROYER }.cells
        for ((index, target) in targets.withIndex()) {
            val selected = session.state.value.selected
            session.action(GameAction.Move(target.row - selected.row, target.column - selected.column))
            session.action(GameAction.Fire)
            if (index != targets.lastIndex) {
                advanceTimeBy(6_000)
                runCurrent()
            }
        }
        assertEquals(GamePhase.COMPUTER_TURN, session.state.value.game!!.phase)
        assertEquals("Destroyer sunk", session.state.value.message)
        session.action(GameAction.Move(0, 1))
        assertEquals("Destroyer sunk", session.state.value.message)
        session.action(GameAction.Move(-20, -20))
        session.action(GameAction.Move(-1, 0))
        assertEquals("Destroyer sunk", session.state.value.message)
    }

    @Test fun clampedAimKeepsRepeatedShotWarning() = runTest {
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), seedSource = { 42L }, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        advanceTimeBy(6_000)
        runCurrent()
        session.action(GameAction.Fire)
        session.action(GameAction.Move(-1, 0))
        assertEquals(Coordinate(0, 0), session.state.value.selected)
        assertEquals("Already targeted", session.state.value.message)
    }

    @Test fun computerHitUsesTheFriendlyDamageSound() = runTest {
        val seed = (0L..100L).first { candidate ->
            val game = GameEngine.newGame(candidate)
            val target = HuntTargetStrategy(candidate).chooseShot(AttackKnowledge.fromShots(emptyList()))
            game.playerBoard.ships.any { target in it.cells }
        }
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), seedSource = { seed }, ioDispatcher = StandardTestDispatcher(testScheduler))
        val cues = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.sounds.collect { cues += it.name } }
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals("DAMAGE", cues.last())
    }

    @Test fun victoryPlaysExactlyOnceAndMuteSuppressesTheWholeMatch() = runTest {
        for (muted in listOf(false, true)) {
            val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), seedSource = { 42L }, ioDispatcher = StandardTestDispatcher(testScheduler))
            val cues = mutableListOf<String>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.sounds.collect { cues += it.name } }
            runCurrent()
            session.action(GameAction.Play)
            if (muted) session.action(GameAction.ToggleSound)
            cues.clear()
            val targets = session.state.value.game!!.computerBoard.ships.flatMap { it.cells }
            for (target in targets) {
                val current = session.state.value.selected
                session.action(GameAction.Move(target.row - current.row, target.column - current.column))
                session.action(GameAction.Fire)
                advanceTimeBy(6_000)
                runCurrent()
            }
            assertEquals(Player.HUMAN, session.state.value.game!!.winner)
            assertEquals(Screen.RESULTS, session.state.value.screen)
            if (muted) assertTrue(cues.isEmpty()) else assertEquals(1, cues.count { it == "VICTORY" })
            assertFalse(cues.contains("DEFEAT"))
        }
    }

    @Test fun defeatPlaysExactlyOnceWhenTheComputerWins() = runTest {
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), seedSource = { 42L }, ioDispatcher = StandardTestDispatcher(testScheduler))
        val cues = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.sounds.collect { cues += it.name } }
        runCurrent()
        session.action(GameAction.Play)
        val enemyCells = session.state.value.game!!.computerBoard.ships.flatMap { it.cells }.toSet()
        val water = (0..9).flatMap { row -> (0..9).map { column -> Coordinate(row, column) } }.filter { it !in enemyCells }
        for (target in water) {
            if (session.state.value.game!!.phase == GamePhase.GAME_OVER) break
            val current = session.state.value.selected
            session.action(GameAction.Move(target.row - current.row, target.column - current.column))
            session.action(GameAction.Fire)
            advanceTimeBy(6_000)
            runCurrent()
        }
        assertEquals(Player.COMPUTER, session.state.value.game!!.winner)
        assertEquals(1, cues.count { it == "DEFEAT" })
        assertFalse(cues.contains("VICTORY"))
    }

    @Test fun pauseCancelsThePendingComputerShotAndResumeAppliesItOnce() = runTest {
        val session = GameSession(backgroundScope, GameSaveRepository(folder.newFile()), aiDelayMs = 500, seedSource = { 12L }, ioDispatcher = StandardTestDispatcher(testScheduler))
        runCurrent()
        session.action(GameAction.Play)
        session.action(GameAction.Fire)
        assertEquals(GamePhase.COMPUTER_TURN, session.state.value.game!!.phase)
        session.action(GameAction.Pause)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(0, session.state.value.game!!.playerBoard.shots.size)
        session.action(GameAction.Resume)
        advanceTimeBy(6_000)
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
        advanceTimeBy(6_000)
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
        advanceTimeBy(6_000)
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
        advanceTimeBy(6_000)
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
        advanceTimeBy(6_000)
        runCurrent()
        assertTrue(session.state.value.game!!.playerBoard.shots.isEmpty())
        session.action(GameAction.Continue)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(1, session.state.value.game!!.playerBoard.shots.size)
    }
}
