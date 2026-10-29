package com.amine.battleship.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import com.amine.battleship.R
import com.amine.battleship.viewmodel.SessionState
import com.amine.battleship.viewmodel.SoundCue

class GameAudio(context: Context) : AutoCloseable {
    private val handler = Handler(Looper.getMainLooper())
    private val music = MusicController(NativeMusicBackend(context))
    private var foreground = false
    private var closed = false
    private val loaded = java.util.Collections.synchronizedSet(mutableSetOf<Int>())
    private val streams = ArrayDeque<Int>()
    private val restoreOpening = Runnable { music.duck(false) }
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val sounds: Map<SoundCue, Int>

    private val focus = AudioFocusController(NativeAudioFocusBackend(context, handler)) { focused, ducked ->
        music.focusDuck(ducked)
        music.focus(focused)
        if (!focused || ducked) stopEffects()
    }

    init {
        music.focus(false)
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) }
        sounds = mapOf(
            SoundCue.SELECT to R.raw.ui_select, SoundCue.FIRE to R.raw.fire,
            SoundCue.MISS to R.raw.splash, SoundCue.HIT to R.raw.impact,
            SoundCue.DAMAGE to R.raw.damage, SoundCue.SUNK to R.raw.sunk,
        ).mapValues { pool.load(context, it.value, 1) }
    }

    fun update(value: SessionState) = update(value, explicit = false)

    private fun update(value: SessionState, explicit: Boolean) {
        if (closed) return
        music.update(value)
        focus.update(foreground && value.loaded && value.soundEnabled, explicit)
        if (!value.soundEnabled) stopEffects()
    }

    fun foreground(value: Boolean, current: SessionState) {
        if (closed) return
        foreground = value
        if (!value) {
            music.foreground(false)
            stopEffects()
        }
        update(current)
        if (value) music.foreground(true)
    }

    fun play(cue: SoundCue, current: SessionState) {
        if (closed) return
        // SELECT and FIRE originate from user input. Incoming impacts and results must not retry focus.
        update(current, explicit = cue == SoundCue.SELECT || cue == SoundCue.FIRE)
        music.cue(cue, current)
        if (cue == SoundCue.STOP) { stopEffects(); return }
        if (!foreground || !focus.focused || !current.loaded || !current.soundEnabled) return
        val id = sounds[cue] ?: return
        val gain = when (cue) {
            SoundCue.SELECT, SoundCue.FIRE, SoundCue.MISS -> .32f
            SoundCue.HIT, SoundCue.DAMAGE -> .60f
            SoundCue.SUNK -> .55f
            else -> 0f
        } * if (focus.ducked) .25f else 1f
        if (id in loaded) {
            val stream = pool.play(id, gain, gain, 1, 0, 1f)
            if (stream != 0) {
                if (streams.size >= 16) streams.removeFirst()
                streams.addLast(stream)
                if (cue != SoundCue.SELECT) {
                    music.duck(true)
                    handler.removeCallbacks(restoreOpening)
                    handler.postDelayed(restoreOpening, when (cue) {
                        SoundCue.SUNK -> 2500L
                        SoundCue.HIT, SoundCue.DAMAGE -> 1500L
                        else -> 1100L
                    })
                }
            }
        }
    }

    private fun stopEffects() {
        streams.forEach(pool::stop)
        streams.clear()
        handler.removeCallbacks(restoreOpening)
        music.duck(false)
    }

    override fun close() {
        if (closed) return
        closed = true
        foreground = false
        stopEffects()
        music.close()
        focus.close()
        pool.release()
    }
}
