package com.amine.battleship.viewmodel

import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.*
import com.amine.battleship.persistence.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.coroutines.CoroutineContext

/** Session contracts use reflection until the new API exists, so RED is an assertion failure. */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalGameSessionTest {
    @get:Rule val folder = TemporaryFolder()

    private fun command(session: GameSession, name: String) {
        val type = runCatching { Class.forName("com.amine.battleship.viewmodel.GameAction\$$name") }.getOrNull()
        assertNotNull("Missing local action $name", type)
        session.action(type!!.getField("INSTANCE").get(null) as GameAction)
    }
    private fun property(value: Any, name: String): Any? {
        val getter = value.javaClass.methods.firstOrNull { it.name == "get${name.replaceFirstChar(Char::uppercaseChar)}" }
        assertNotNull("Missing ${value.javaClass.simpleName}.$name", getter)
        return getter!!.invoke(value)
    }
    private fun local(session: GameSession) {
        command(session, "LocalSetup")
        command(session, "NextParticipant")
        command(session, "StartLocal")
        if (session.state.value.confirmation != null) session.action(GameAction.Confirm)
    }
    private fun readyBoth(session: GameSession) {
        command(session, "Ready")
        command(session, "ReleaseConfirm")
        command(session, "Ready")
        command(session, "ReleaseConfirm")
    }
    private fun TestScope.session(repository: GameSaveRepository = GameSaveRepository(folder.newFile()), io: CoroutineDispatcher = StandardTestDispatcher(testScheduler)) =
        GameSession(backgroundScope, repository, seedSource = { 42L }, ioDispatcher = io,
            nanoTimeSource = { testScheduler.currentTime * 1_000_000 })
    private fun reveal(state: SessionState): Long? = property(state, "revealRemainingMs") as Long?
    private fun ready(state: SessionState): Player? = property(state, "readySeat") as Player?
    private fun TestScope.startAndHide(session: GameSession) {
        local(session); readyBoth(session); runCurrent(); advanceTimeBy(10_000); runCurrent()
    }

    @Test fun bothReadyStepsPrecedeTheFullTenSecondReveal() = runTest {
        val session = session(); runCurrent(); local(session)
        assertEquals(Player.HUMAN, ready(session.state.value)); assertNull(reveal(session.state.value))
        command(session, "Ready"); command(session, "ReleaseConfirm")
        assertEquals(Player.COMPUTER, ready(session.state.value)); assertNull(reveal(session.state.value))
        command(session, "Ready"); runCurrent()
        assertEquals(10_000L, reveal(session.state.value))
        advanceTimeBy(9_999); runCurrent(); assertTrue(reveal(session.state.value)!! > 0)
        advanceTimeBy(1); runCurrent(); assertNull(reveal(session.state.value))
        assertEquals(GamePhase.PLAYER_TURN, session.state.value.game!!.phase)
    }

    @Test fun previewBlocksMovementAndFire() = runTest {
        val session = session(); runCurrent(); local(session); readyBoth(session); runCurrent()
        val before = session.state.value
        session.action(GameAction.Move(3, 4)); session.action(GameAction.Fire)
        assertEquals(before.selected, session.state.value.selected)
        assertEquals(before.game, session.state.value.game)
    }

    @Test fun saveMustAcknowledgeBeforeAnyFleetIsRevealed() = runTest {
        val io = QueuedDispatcher(); val repository = GameSaveRepository(folder.newFile())
        val session = session(repository, io); runCurrent(); io.drain(); runCurrent()
        local(session); readyBoth(session); runCurrent()
        assertNull(reveal(session.state.value)); assertNull(session.state.value.game)
        io.drain(); runCurrent()
        assertEquals(10_000L, reveal(session.state.value))
        assertEquals(true, property(repository.load()!!.game!!, "previewConsumed"))
        assertEquals("LOCAL", property(repository.load()!!.game!!, "mode").toString())
    }

    @Test fun failedPreviewSaveKeepsCommittedMatchAndRetryAcknowledgesBeforeReveal() = runTest {
        val file = folder.newFile(); val repository = GameSaveRepository(file)
        val old = GameEngine.newGame(21); repository.save(SavedSession(game = old))
        val session = session(repository); runCurrent()
        val pending = java.io.File(file.parentFile, "${file.name}.pending"); pending.mkdir()
        local(session); readyBoth(session); runCurrent()
        assertNull(reveal(session.state.value)); assertEquals(old, session.state.value.game)
        assertEquals(old, repository.load()!!.game); assertNotNull(session.state.value.saveWarning)
        pending.delete(); command(session, "RetrySave"); runCurrent()
        assertEquals(10_000L, reveal(session.state.value)); assertNotEquals(old, repository.load()!!.game)
    }

    @Test fun cancelledSetupPreservesTheExistingSaveAndProfiles() = runTest {
        val repository = GameSaveRepository(folder.newFile()); val old = GameEngine.newGame(19)
        repository.save(SavedSession(game = old)); val session = session(repository); runCurrent()
        command(session, "LocalSetup"); session.action(GameAction.Back); runCurrent()
        assertEquals(old, session.state.value.game); assertEquals(old, repository.load()!!.game)
        assertNull(property(session.state.value, "setup"))
    }

    @Test fun bothSeatsFireWithoutAnyComputerTaskAndKeepFixedImpactBoards() = runTest {
        val session = session(); runCurrent(); startAndHide(session)
        session.action(GameAction.Fire); runCurrent()
        assertEquals("RIGHT", session.state.value.activeImpact!!.board.name)
        val firstId = session.state.value.activeImpact!!.id
        advanceTimeBy(3_000); runCurrent()
        assertEquals(Player.COMPUTER, ready(session.state.value))
        assertEquals(0, session.state.value.game!!.playerBoard.shots.size)
        command(session, "Ready"); command(session, "ReleaseConfirm")
        session.action(GameAction.Fire); runCurrent()
        assertEquals("LEFT", session.state.value.activeImpact!!.board.name)
        assertTrue(session.state.value.activeImpact!!.id > firstId)
        session.action(GameAction.CompleteImpact(firstId))
        assertNotNull(session.state.value.activeImpact)
        advanceTimeBy(20_000); runCurrent()
        assertEquals(1, session.state.value.game!!.playerBoard.shots.size)
        assertEquals(1, session.state.value.game!!.computerBoard.shots.size)
        assertEquals(Player.HUMAN, ready(session.state.value))
    }

    @Test fun readyNeedsReleaseBeforeTheNextSeatCanFire() = runTest {
        val session = session(); runCurrent(); startAndHide(session)
        session.action(GameAction.Fire); advanceTimeBy(3_000); runCurrent()
        command(session, "Ready")
        repeat(6) { session.action(GameAction.Fire) }
        assertTrue(session.state.value.game!!.playerBoard.shots.isEmpty())
        command(session, "ReleaseConfirm"); session.action(GameAction.Fire)
        assertEquals(1, session.state.value.game!!.playerBoard.shots.size)
    }

    @Test fun shotHandoffWaitsForTheSaveAndTheWholeEffect() = runTest {
        val io = QueuedDispatcher(); val repository = GameSaveRepository(folder.newFile())
        val session = session(repository, io); runCurrent(); io.drain(); runCurrent()
        local(session); readyBoth(session); runCurrent(); io.drain(); runCurrent()
        advanceTimeBy(10_000); runCurrent()
        session.action(GameAction.Fire); runCurrent()
        advanceTimeBy(3_000); runCurrent()
        assertNull(ready(session.state.value)); assertTrue(session.state.value.resolving)
        io.drain(); runCurrent()
        assertEquals(Player.COMPUTER, ready(session.state.value))
        assertEquals(1, repository.load()!!.game!!.computerBoard.shots.size)
    }

    @Test fun failedShotSaveBlocksHandoffUntilRetry() = runTest {
        val file = folder.newFile(); val session = session(GameSaveRepository(file)); runCurrent(); startAndHide(session)
        val pending = java.io.File(file.parentFile, "${file.name}.pending"); pending.mkdir()
        session.action(GameAction.Fire); advanceTimeBy(3_000); runCurrent()
        assertNull(ready(session.state.value)); assertNotNull(session.state.value.saveWarning)
        assertTrue(GameSaveRepository(file).load()!!.game!!.computerBoard.shots.isEmpty())
        pending.delete(); command(session, "RetrySave"); runCurrent()
        assertEquals(Player.COMPUTER, ready(session.state.value))
    }

    @Test fun repeatAttackPreservesSeatWithoutEffectOrHandoff() = runTest {
        val session = session(); runCurrent(); startAndHide(session)
        repeat(2) { session.action(GameAction.Fire); advanceTimeBy(3_000); runCurrent(); command(session, "Ready"); command(session, "ReleaseConfirm") }
        val before = session.state.value.game
        session.action(GameAction.Fire); runCurrent()
        assertEquals(before, session.state.value.game); assertNull(session.state.value.activeImpact)
        assertNull(ready(session.state.value)); assertEquals("Already targeted", session.state.value.message)
    }

    @Test fun backgroundConsumesRevealAndResumeNeverRevealsAgain() = runTest {
        val session = session(); runCurrent(); local(session); readyBoth(session); runCurrent()
        advanceTimeBy(2_000); session.background(); runCurrent()
        assertNull(reveal(session.state.value)); assertTrue(session.state.value.paused)
        session.action(GameAction.Resume)
        assertNull(reveal(session.state.value)); assertEquals(Player.HUMAN, ready(session.state.value))
        advanceTimeBy(20_000); runCurrent(); assertTrue(session.state.value.game!!.computerBoard.shots.isEmpty())
    }

    @Test fun interruptionWhilePreviewWriteIsPendingCannotRevealOnAcknowledgement() = runTest {
        val io = QueuedDispatcher(); val session = session(io = io)
        runCurrent(); io.drain(); runCurrent(); local(session); readyBoth(session); runCurrent()
        session.background(); io.drain(); runCurrent()
        assertNull(reveal(session.state.value)); assertTrue(session.state.value.paused)
        assertEquals("LOCAL", property(session.state.value.game!!, "mode").toString())
        session.action(GameAction.Resume); assertNull(reveal(session.state.value))
        assertEquals(Player.HUMAN, ready(session.state.value))
    }

    @Test fun restorationAndContinueArePausedHiddenAndNeverScheduleAi() = runTest {
        val repository = GameSaveRepository(folder.newFile()); val first = session(repository)
        runCurrent(); startAndHide(first); first.action(GameAction.Fire); advanceTimeBy(3_000); runCurrent()
        val restored = session(repository); runCurrent(); restored.action(GameAction.Continue)
        assertTrue(restored.state.value.paused); assertNull(reveal(restored.state.value))
        restored.action(GameAction.Resume); assertEquals(Player.COMPUTER, ready(restored.state.value))
        advanceTimeBy(20_000); runCurrent(); assertTrue(restored.state.value.game!!.playerBoard.shots.isEmpty())
    }

    @Test fun eitherLocalSeatWinsWithCelebrationAndRematchKeepsItsProfiles() = runTest {
        for (winner in Player.entries) {
            val session = session(); val cues = mutableListOf<SoundCue>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { session.sounds.collect { cues += it } }
            runCurrent(); startAndHide(session)
            val initial = session.state.value.game!!
            val target = if (winner == Player.HUMAN) initial.computerBoard else initial.playerBoard
            val hits = target.ships.flatMap { it.cells }.iterator()
            val other = if (winner == Player.HUMAN) initial.playerBoard else initial.computerBoard
            val occupied = other.ships.flatMap { it.cells }.toSet()
            val misses = (0..9).flatMap { row -> (0..9).map { col -> Coordinate(row, col) } }.filter { it !in occupied }.iterator()
            repeat(40) {
                val game = session.state.value.game!!
                if (game.phase == GamePhase.GAME_OVER) return@repeat
                val seat = if (game.phase == GamePhase.PLAYER_TURN) Player.HUMAN else Player.COMPUTER
                val cell = if (seat == winner) hits.next() else misses.next()
                val current = session.state.value.selected
                session.action(GameAction.Move(cell.row - current.row, cell.column - current.column))
                session.action(GameAction.Fire); advanceTimeBy(3_000); runCurrent()
                if (session.state.value.game!!.phase != GamePhase.GAME_OVER) { command(session, "Ready"); command(session, "ReleaseConfirm") }
            }
            assertEquals(winner, session.state.value.game!!.winner)
            assertEquals(Screen.RESULTS, session.state.value.screen)
            assertEquals(1, cues.count { it == SoundCue.VICTORY }); assertFalse(cues.contains(SoundCue.DEFEAT))
            val profiles = listOf(property(initial, "leftProfile"), property(initial, "rightProfile"))
            session.action(GameAction.Restart); session.action(GameAction.Confirm)
            assertEquals(Player.HUMAN, ready(session.state.value)); readyBoth(session); runCurrent()
            assertEquals(10_000L, reveal(session.state.value))
            assertEquals(profiles, listOf(property(session.state.value.game!!, "leftProfile"), property(session.state.value.game!!, "rightProfile")))
            assertTrue(session.state.value.game!!.playerBoard.shots.isEmpty())
        }
    }

    @Test fun firstCaptainCanChooseAnyPortraitAndCustomNameSurvivesPortraitAndLanguageChanges() = runTest {
        val session = session(); runCurrent(); command(session, "LocalSetup")
        session.action(GameAction.SaveName("  Harbor  "))
        for (id in listOf("fox", "owl", "bear", "cat", "storm", "reed", "kai", "mira")) {
            session.action(GameAction.SelectPortrait(id))
            val draft = session.state.value.setup!!
            assertEquals(id, draft.left.portraitId); assertEquals("Harbor", draft.left.customName)
            assertNotEquals(id, draft.right.portraitId)
        }
        session.action(GameAction.CycleLanguage)
        assertEquals("Harbor", session.state.value.setup!!.left.customName)
        session.action(GameAction.SaveName("")); assertNull(session.state.value.setup!!.left.customName)
    }

    @Test fun backgroundBeforeBothReadyConsumesThePendingPreviewWithoutGettingStuck() = runTest {
        val session = session(); runCurrent(); local(session)
        session.background(); runCurrent()
        assertTrue(session.state.value.paused); assertNull(reveal(session.state.value))
        assertNotNull(session.state.value.game)
        session.action(GameAction.Resume)
        assertEquals(Player.HUMAN, ready(session.state.value)); assertNull(reveal(session.state.value))
        command(session, "Ready"); command(session, "ReleaseConfirm")
        session.action(GameAction.Fire); runCurrent()
        assertEquals(1, session.state.value.game!!.computerBoard.shots.size)
    }

    private class QueuedDispatcher : CoroutineDispatcher() {
        private val tasks = ArrayDeque<Runnable>()
        override fun dispatch(context: CoroutineContext, block: Runnable) { tasks.add(block) }
        fun drain() { while (tasks.isNotEmpty()) tasks.removeFirst().run() }
    }
}
