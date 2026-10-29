package com.amine.battleship.audio

import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.GamePhase
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState
import com.amine.battleship.viewmodel.SoundCue
import org.junit.Assert.*
import org.junit.Test

class MusicControllerTest {
    private class Player : MusicPlayback {
        var starts = 0
        var released = false
        var gain = 0f
        override fun start() { check(!released); starts++ }
        override fun volume(value: Float) { gain = value }
        override fun release() { released = true }
    }
    private class Backend : MusicBackend {
        data class Request(val track: MusicTrack, val player: Player, val ready: () -> Unit, val done: () -> Unit)
        val requests = mutableListOf<Request>()
        override fun prepare(track: MusicTrack, ready: () -> Unit, done: () -> Unit): MusicPlayback =
            Player().also { requests += Request(track, it, ready, done) }
    }
    private val home = SessionState(loaded = true)
    private val game = home.copy(screen = Screen.GAME, game = GameEngine.newGame(42))
    private val results = game.copy(screen = Screen.RESULTS, game = game.game!!.copy(phase = GamePhase.GAME_OVER))

    @Test fun openingCompletionStartsBattleLoopWithoutReplayingOpening() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.cue(SoundCue.MATCH_START, game)
        val opening = backend.requests.last(); opening.ready(); opening.done()
        val battle = backend.requests.last(); battle.ready()
        assertEquals("BATTLE", battle.track.name)
        assertTrue(battle.track.loop)
        assertTrue(battle.player.gain < opening.player.gain)
        assertTrue(opening.player.released)
        music.update(game)
        assertEquals(1, backend.requests.count { it.track == MusicTrack.START })
        assertEquals(1, battle.player.starts)
    }

    @Test fun continueAndInterruptedOpeningReturnToBattleWithoutAnotherOpening() {
        for (interrupt in 0..3) {
            val backend = Backend(); val music = MusicController(backend)
            music.foreground(true); music.cue(SoundCue.MATCH_START, game)
            val opening = backend.requests.last(); opening.ready()
            when (interrupt) {
                0 -> { music.update(game.copy(paused = true)); music.update(game) }
                1 -> { music.foreground(false); music.foreground(true) }
                2 -> { music.focus(false); music.focus(true) }
                3 -> { music.update(game.copy(soundEnabled = false)); music.update(game) }
            }
            opening.done(); opening.ready()
            val battle = backend.requests.last(); battle.ready()
            assertTrue(opening.player.released)
            assertEquals("BATTLE", battle.track.name)
            assertEquals(1, backend.requests.count { it.track == MusicTrack.START })
            assertEquals(1, battle.player.starts)
        }
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.update(game)
        assertEquals(listOf("BATTLE"), backend.requests.map { it.track.name })
    }

    @Test fun finalShotStopsBattleBeforeResultCueAndResultDoesNotReviveLoop() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.update(game)
        val battle = backend.requests.last(); battle.ready()
        music.update(results.copy(screen = Screen.GAME))
        assertTrue(battle.player.released)
        assertEquals(1, backend.requests.size)
        music.cue(SoundCue.VICTORY, results)
        val victory = backend.requests.last(); victory.ready(); victory.done()
        music.update(results); music.focus(false); music.focus(true)
        assertEquals(listOf("BATTLE", "VICTORY"), backend.requests.map { it.track.name })
    }

    @Test fun finalShotCancelsOpeningAndItsLateCompletionCannotStartBattle() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.cue(SoundCue.MATCH_START, game)
        val opening = backend.requests.single(); opening.ready()
        music.update(results.copy(screen = Screen.GAME))
        opening.done()
        assertTrue(opening.player.released)
        assertEquals(listOf(MusicTrack.START), backend.requests.map { it.track })
    }

    @Test fun battleLoopReleasesOnEveryInterruptionAndReturnsOnlyWhenEligible() {
        for (interrupt in 0..3) {
            val backend = Backend(); val music = MusicController(backend)
            music.foreground(true); music.update(game)
            val first = backend.requests.single(); first.ready()
            when (interrupt) {
                0 -> music.update(game.copy(paused = true))
                1 -> music.foreground(false)
                2 -> music.focus(false)
                3 -> music.update(game.copy(soundEnabled = false))
            }
            first.ready(); first.done()
            assertTrue(first.player.released)
            assertEquals(1, backend.requests.size)
            when (interrupt) {
                0, 3 -> music.update(game)
                1 -> music.foreground(true)
                2 -> music.focus(true)
            }
            assertEquals(listOf(MusicTrack.BATTLE, MusicTrack.BATTLE), backend.requests.map { it.track })
        }
    }

    @Test fun menuRemainsOneLoopAcrossHomeHelpAndSettings() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.update(home)
        music.update(home.copy(screen = Screen.HELP)); music.update(home.copy(screen = Screen.SETTINGS)); music.update(home)
        assertEquals(listOf(MusicTrack.MENU), backend.requests.map { it.track })
        backend.requests.single().ready()
        assertEquals(1, backend.requests.single().player.starts)
    }

    @Test fun loadedMutedAndPausedGamePagesCannotStartMenu() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true)
        music.update(SessionState()); music.update(home.copy(soundEnabled = false))
        music.update(game.copy(paused = true, screen = Screen.SETTINGS))
        assertTrue(backend.requests.isEmpty())
    }

    @Test fun confirmedRestartReplacesAnOpeningEvenIfStateCollectorsSkipThePause() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.cue(SoundCue.MATCH_START, game)
        val first = backend.requests.single()
        music.cue(SoundCue.MATCH_START, game.copy(game = GameEngine.newGame(43)))
        assertTrue(first.player.released)
        assertEquals(2, backend.requests.size)
        first.ready()
        assertEquals(0, first.player.starts)
    }

    @Test fun resultsAcceptEitherStateCollectorOrderAndNeverReplay() {
        for (cueFirst in listOf(false, true)) {
            val backend = Backend(); val music = MusicController(backend)
            music.foreground(true); music.update(game)
            if (!cueFirst) music.update(results)
            music.cue(SoundCue.VICTORY, if (cueFirst) results.copy(screen = Screen.GAME) else results)
            music.update(results)
            backend.requests.last().ready(); backend.requests.last().done()
            music.cue(SoundCue.VICTORY, results); music.update(results)
            music.foreground(false); music.foreground(true)
            assertEquals(listOf(MusicTrack.BATTLE, MusicTrack.VICTORY), backend.requests.map { it.track })
        }
    }

    @Test fun aNewMatchThatStartsMutedCanStillPlayItsLaterResult() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.cue(SoundCue.VICTORY, results)
        music.update(home.copy(soundEnabled = false))
        // Muted GameSession emits no MATCH_START cue.
        music.update(game.copy(soundEnabled = false))
        music.update(game)
        music.cue(SoundCue.DEFEAT, results)
        assertEquals(listOf(MusicTrack.VICTORY, MusicTrack.BATTLE, MusicTrack.DEFEAT), backend.requests.map { it.track })
    }

    @Test fun replacementAndMuteRejectLatePreparedAndCompletionCallbacks() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.update(home)
        val menu = backend.requests.single()
        music.cue(SoundCue.MATCH_START, game)
        val opening = backend.requests.last()
        menu.ready(); menu.done()
        assertEquals(0, menu.player.starts); assertTrue(menu.player.released)
        assertFalse("Stale completion must not release the replacement", opening.player.released)
        music.update(game.copy(soundEnabled = false))
        opening.ready(); opening.done()
        assertEquals(0, opening.player.starts); assertTrue(opening.player.released)
        music.update(game)
        assertEquals(3, backend.requests.size)
        assertEquals(MusicTrack.BATTLE, backend.requests.last().track)
    }

    @Test fun backgroundStopFocusLossAndLeavingResultsDiscardOneShots() {
        for (interrupt in 0..3) {
            val backend = Backend(); val music = MusicController(backend)
            music.foreground(true); music.cue(SoundCue.DEFEAT, results)
            val request = backend.requests.single(); request.ready()
            when (interrupt) {
                0 -> { music.foreground(false); music.cue(SoundCue.VICTORY, results); music.foreground(true) }
                1 -> music.cue(SoundCue.STOP, results)
                2 -> { music.focus(false); music.focus(true) }
                3 -> music.update(home)
            }
            request.ready(); request.done()
            assertTrue(request.player.released)
            assertEquals(1, request.player.starts)
            assertEquals(if (interrupt == 3) 2 else 1, backend.requests.size)
        }
    }

    @Test fun closedControllerAndBackgroundCuesCannotRevivePreparedMusic() {
        val backend = Backend(); val music = MusicController(backend)
        music.cue(SoundCue.MATCH_START, game)
        music.cue(SoundCue.VICTORY, results)
        assertTrue(backend.requests.isEmpty())
        music.foreground(true); music.update(home)
        val request = backend.requests.single()
        music.close()
        request.ready(); request.done()
        music.foreground(true); music.update(home)
        assertTrue(request.player.released)
        assertEquals(0, request.player.starts)
        assertEquals(1, backend.requests.size)
    }

    @Test fun stopCueOnHomeDoesNotSilenceMenuOrRestartItsLoop() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.update(home)
        val menu = backend.requests.single(); menu.ready()
        music.cue(SoundCue.STOP, home)
        assertFalse(menu.player.released)
        assertEquals(1, backend.requests.size)
    }

    @Test fun battleEffectsDuckOpeningAndBattleButLeaveMenuUnchanged() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.cue(SoundCue.MATCH_START, game)
        val opening = backend.requests.single(); opening.ready()
        val normalGain = opening.player.gain
        music.duck(true)
        assertTrue(opening.player.gain < normalGain)
        music.duck(false)
        assertEquals(normalGain, opening.player.gain, .0001f)
        opening.done()
        val battle = backend.requests.last(); battle.ready()
        val battleGain = battle.player.gain
        music.duck(true)
        assertTrue(battle.player.gain < battleGain)
        music.duck(false)
        assertEquals(battleGain, battle.player.gain, .0001f)
        music.update(home); val menu = backend.requests.last(); menu.ready()
        music.duck(true)
        assertEquals(MusicTrack.MENU.gain, menu.player.gain, .0001f)
    }
}
