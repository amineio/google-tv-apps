package com.amine.battleship.audio

import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState
import com.amine.battleship.viewmodel.SoundCue
import org.junit.Assert.*
import org.junit.Test

class AudioFocusControllerTest {
    private class Lease(override val granted: Boolean) : AudioFocusLease {
        var abandons = 0
        override fun abandon() { abandons++ }
    }
    private class Backend(var grant: Boolean = true) : AudioFocusBackend {
        data class Request(val lease: Lease, val changed: (AudioFocusChange) -> Unit)
        val requests = mutableListOf<Request>()
        override fun request(changed: (AudioFocusChange) -> Unit): AudioFocusLease =
            Lease(grant).also { requests += Request(it, changed) }
    }

    @Test fun denialCanRecoverOnLaterExplicitActionWithoutAutomaticRetry() {
        val backend = Backend(grant = false)
        val focus = AudioFocusController(backend) { _, _ -> }
        focus.update(eligible = true)
        val denied = backend.requests.single()
        assertFalse(focus.focused)
        backend.grant = true
        repeat(5) { focus.update(eligible = true) }
        assertEquals("State updates must not fight the current focus owner", 1, backend.requests.size)
        focus.update(eligible = true, explicit = true)
        assertEquals("A later explicit action must retry", 2, backend.requests.size)
        assertTrue(focus.focused)
        assertEquals(1, denied.lease.abandons)
    }

    @Test fun permanentLossReleasesTheLeaseAndWaitsForExplicitAction() {
        val backend = Backend()
        val focus = AudioFocusController(backend) { _, _ -> }
        focus.update(true)
        val lost = backend.requests.single()
        lost.changed(AudioFocusChange.LOSS)
        assertFalse(focus.focused)
        repeat(5) { focus.update(true) }
        assertEquals(1, backend.requests.size)
        focus.update(true, explicit = true)
        assertEquals(2, backend.requests.size)
        assertTrue(focus.focused)
        assertEquals(1, lost.lease.abandons)
    }

    @Test fun staleGainAfterDenialOrPermanentLossCannotRestoreOrCancelTheNewLease() {
        for (denied in listOf(false, true)) {
            val backend = Backend(grant = !denied)
            val focus = AudioFocusController(backend) { _, _ -> }
            focus.update(true)
            val old = backend.requests.single()
            if (!denied) old.changed(AudioFocusChange.LOSS)
            old.changed(AudioFocusChange.GAIN)
            assertFalse("Abandoned callbacks must stay invalid", focus.focused)
            backend.grant = true
            focus.update(true, explicit = true)
            val fresh = backend.requests.last()
            old.changed(AudioFocusChange.LOSS)
            assertTrue("An old loss must not silence newly acquired focus", focus.focused)
            assertEquals(0, fresh.lease.abandons)
        }
    }

    @Test fun transientLossRetainsItsLeaseAndDoesNotFightForFocus() {
        val backend = Backend()
        val focus = AudioFocusController(backend) { _, _ -> }
        focus.update(true)
        val request = backend.requests.single()
        request.changed(AudioFocusChange.TRANSIENT_LOSS)
        focus.update(true, explicit = true)
        assertFalse(focus.focused)
        assertEquals(1, backend.requests.size)
        assertEquals(0, request.lease.abandons)
        request.changed(AudioFocusChange.GAIN)
        assertTrue(focus.focused)
        request.changed(AudioFocusChange.DUCK)
        assertTrue(focus.focused)
        assertTrue(focus.ducked)
        request.changed(AudioFocusChange.GAIN)
        assertFalse(focus.ducked)
    }

    @Test fun foregroundOrSoundEnableAllowsOneNewAttemptAndCloseRejectsCallbacks() {
        val backend = Backend(grant = false)
        val focus = AudioFocusController(backend) { _, _ -> }
        focus.update(true)
        focus.update(false)
        backend.grant = true
        focus.update(true)
        assertEquals(2, backend.requests.size)
        val active = backend.requests.last()
        focus.close()
        active.changed(AudioFocusChange.GAIN)
        focus.update(true, explicit = true)
        assertFalse(focus.focused)
        assertEquals(2, backend.requests.size)
        assertEquals(1, active.lease.abandons)
    }

    @Test fun aFreshFocusRequestNeverReplaysTheDiscardedOpening() {
        val backend = Backend()
        val tracks = mutableListOf<MusicTrack>()
        val musicBackend = object : MusicBackend {
            override fun prepare(track: MusicTrack, ready: () -> Unit, done: () -> Unit): MusicPlayback {
                tracks += track
                return object : MusicPlayback {
                    override fun start() = Unit
                    override fun volume(value: Float) = Unit
                    override fun release() = Unit
                }
            }
        }
        val music = MusicController(musicBackend)
        music.focus(false)
        val focus = AudioFocusController(backend) { granted, ducked ->
            music.focusDuck(ducked)
            music.focus(granted)
        }
        val game = SessionState(loaded = true, screen = Screen.GAME, game = GameEngine.newGame(42))
        music.update(game); music.foreground(true)
        focus.update(true)
        music.cue(SoundCue.MATCH_START, game)
        backend.requests.single().changed(AudioFocusChange.LOSS)
        focus.update(true, explicit = true)
        assertEquals(2, backend.requests.size)
        assertEquals(listOf(MusicTrack.START), tracks)
        music.close(); focus.close()
    }
}
