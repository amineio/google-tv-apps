package com.amine.battleship.audio

import com.amine.battleship.game.model.GamePhase
import com.amine.battleship.viewmodel.Screen
import com.amine.battleship.viewmodel.SessionState
import com.amine.battleship.viewmodel.SoundCue

enum class MusicTrack(val gain: Float, val loop: Boolean = false) {
    MENU(.40f, true), START(.45f), VICTORY(.50f), DEFEAT(.40f),
}

interface MusicPlayback {
    fun start()
    fun volume(value: Float)
    fun release()
}

interface MusicBackend {
    fun prepare(track: MusicTrack, ready: () -> Unit, done: () -> Unit): MusicPlayback
}

/** Main-thread music policy. Each replacement releases its predecessor before preparing another. */
class MusicController(private val backend: MusicBackend) : AutoCloseable {
    private var state = SessionState()
    private var foreground = false
    private var focused = true
    private var closed = false
    private var resultConsumed = false
    private var effectDucked = false
    private var focusDucked = false
    private var generation = 0L
    private var track: MusicTrack? = null
    private var playback: MusicPlayback? = null
    private var started = false

    fun update(value: SessionState) {
        state = value
        // Muted matches emit no opening cue. Active gameplay still begins a fresh result lifetime.
        if (value.screen == Screen.GAME && value.game?.phase != GamePhase.GAME_OVER) resultConsumed = false
        reconcile()
    }

    fun foreground(value: Boolean) { foreground = value; reconcile() }
    fun focus(value: Boolean) { focused = value; reconcile() }
    fun duck(value: Boolean) { effectDucked = value; applyVolume() }
    fun focusDuck(value: Boolean) { focusDucked = value; applyVolume() }

    fun cue(cue: SoundCue, value: SessionState) {
        update(value)
        when (cue) {
            SoundCue.MATCH_START -> {
                resultConsumed = false
                if (available() && state.screen == Screen.GAME && !state.paused) {
                    // A confirmed restart can arrive before collectors observe its intermediate pause.
                    replace(null)
                    replace(MusicTrack.START)
                }
            }
            SoundCue.VICTORY, SoundCue.DEFEAT -> {
                val resultContext = state.screen == Screen.RESULTS ||
                    (state.screen == Screen.GAME && state.game?.phase == GamePhase.GAME_OVER)
                if (resultContext && !resultConsumed) {
                    resultConsumed = true
                    if (available() && !state.paused) replace(
                        if (cue == SoundCue.VICTORY) MusicTrack.VICTORY else MusicTrack.DEFEAT)
                }
            }
            // STOP also accompanies returning Home and toggling sound. Preserve an eligible menu loop.
            SoundCue.STOP -> if (track != MusicTrack.MENU) replace(null)
            else -> Unit
        }
    }

    private fun available() = !closed && foreground && focused && state.loaded && state.soundEnabled
    private fun menu() = state.screen in setOf(Screen.HOME, Screen.HELP, Screen.SETTINGS) && !state.paused

    private fun reconcile() {
        when {
            !available() -> replace(null)
            menu() -> replace(MusicTrack.MENU)
            track == MusicTrack.MENU -> replace(null)
            track == MusicTrack.START && (state.screen != Screen.GAME || state.paused) -> replace(null)
            track in setOf(MusicTrack.VICTORY, MusicTrack.DEFEAT) &&
                state.screen != Screen.RESULTS &&
                !(state.screen == Screen.GAME && state.game?.phase == GamePhase.GAME_OVER && !state.paused) -> replace(null)
        }
    }

    private fun replace(next: MusicTrack?) {
        if (track == next) return
        val token = ++generation
        playback?.release()
        playback = null
        started = false
        track = next
        if (next == null) return
        playback = backend.prepare(next, ready = {
            if (generation == token && !started && available()) {
                started = true
                applyVolume()
                playback?.start()
            }
        }, done = {
            if (generation == token) replace(null)
        })
    }

    private fun applyVolume() {
        val current = track ?: return
        val effectGain = if (current == MusicTrack.START && effectDucked) .25f else 1f
        playback?.volume(current.gain * effectGain * if (focusDucked) .25f else 1f)
    }

    override fun close() { closed = true; replace(null) }
}
