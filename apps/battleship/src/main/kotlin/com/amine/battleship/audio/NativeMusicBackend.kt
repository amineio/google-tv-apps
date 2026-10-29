package com.amine.battleship.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.amine.battleship.R

/** Streams compressed local music. Preparation and all player callbacks run on the main thread. */
class NativeMusicBackend(private val context: Context) : MusicBackend {
    override fun prepare(track: MusicTrack, ready: () -> Unit, done: () -> Unit): MusicPlayback {
        val handler = Handler(Looper.getMainLooper())
        val player = MediaPlayer()
        val playback = NativeMusicPlayback(player, handler, done)
        player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        player.setOnPreparedListener { if (!playback.released) ready() }
        player.setOnCompletionListener { if (!playback.released) done() }
        player.setOnErrorListener { _, _, _ -> if (!playback.released) done(); true }
        try {
            val resource = when (track) {
                MusicTrack.MENU -> R.raw.music_menu
                MusicTrack.START -> R.raw.music_start
                MusicTrack.BATTLE -> R.raw.music_battle
                MusicTrack.VICTORY -> R.raw.music_victory
                MusicTrack.DEFEAT -> R.raw.music_defeat
            }
            context.resources.openRawResourceFd(resource).use { source ->
                player.setDataSource(source.fileDescriptor, source.startOffset, source.length)
            }
            player.isLooping = track.loop
            player.setVolume(0f, 0f)
            player.prepareAsync()
        } catch (_: Exception) {
            // Defer failures until the controller owns this returned handle.
            handler.post { if (!playback.released) done() }
        }
        return playback
    }
}

internal class NativeMusicPlayback(
    internal val player: MediaPlayer,
    private val handler: Handler,
    private val failed: () -> Unit,
) : MusicPlayback {
    internal var released = false
        private set
    private var started = false
    private var gain = 0f
    private var targetGain = 0f
    private var ramp: Runnable? = null

    override fun start() {
        if (released || started) return
        try {
            player.start()
            started = true
            fadeTo(targetGain, 500L)
        } catch (_: IllegalStateException) { failed() }
    }

    override fun volume(value: Float) {
        if (released) return
        targetGain = value.coerceIn(0f, 1f)
        if (started) fadeTo(targetGain, 120L)
    }

    private fun fadeTo(destination: Float, duration: Long) {
        ramp?.let(handler::removeCallbacks)
        val initialGain = gain
        val began = SystemClock.uptimeMillis()
        val step = object : Runnable {
            override fun run() {
                if (released) return
                val fraction = ((SystemClock.uptimeMillis() - began).toFloat() / duration).coerceIn(0f, 1f)
                gain = initialGain + (destination - initialGain) * fraction
                try { player.setVolume(gain, gain) } catch (_: IllegalStateException) { failed(); return }
                if (fraction < 1f) handler.postDelayed(this, 16L)
            }
        }
        ramp = step
        handler.post(step)
    }

    override fun release() {
        if (released) return
        released = true
        ramp?.let(handler::removeCallbacks)
        player.setOnPreparedListener(null)
        player.setOnCompletionListener(null)
        player.setOnErrorListener(null)
        player.release()
    }
}
