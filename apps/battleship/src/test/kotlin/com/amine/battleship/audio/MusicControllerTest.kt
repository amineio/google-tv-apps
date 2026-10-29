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

    @Test fun openingRequiresCueAndDoesNotReplayAfterCompletionOrReturn() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.update(game)
        assertTrue(backend.requests.isEmpty()) // Continue/Resume only update state.
        music.cue(SoundCue.MATCH_START, game)
        backend.requests.single().ready(); backend.requests.single().done()
        music.update(game); music.foreground(false); music.foreground(true); music.update(game)
        assertEquals(listOf(MusicTrack.START), backend.requests.map { it.track })
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
            backend.requests.single().ready(); backend.requests.single().done()
            music.cue(SoundCue.VICTORY, results); music.update(results)
            music.foreground(false); music.foreground(true)
            assertEquals(listOf(MusicTrack.VICTORY), backend.requests.map { it.track })
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
        assertEquals(listOf(MusicTrack.VICTORY, MusicTrack.DEFEAT), backend.requests.map { it.track })
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
        music.update(game); assertEquals(2, backend.requests.size)
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

    @Test fun battleEffectsDuckOnlyTheOpening() {
        val backend = Backend(); val music = MusicController(backend)
        music.foreground(true); music.cue(SoundCue.MATCH_START, game)
        val opening = backend.requests.single(); opening.ready()
        val normalGain = opening.player.gain
        music.duck(true)
        assertTrue(opening.player.gain < normalGain)
        music.duck(false)
        assertEquals(normalGain, opening.player.gain, .0001f)
        music.update(home); val menu = backend.requests.last(); menu.ready()
        music.duck(true)
        assertEquals(MusicTrack.MENU.gain, menu.player.gain, .0001f)
    }
}
