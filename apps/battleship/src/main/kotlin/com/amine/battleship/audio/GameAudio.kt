package com.amine.battleship.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.amine.battleship.R
import com.amine.battleship.viewmodel.SoundCue

class GameAudio(context: Context) : AutoCloseable {
    private val loaded = mutableSetOf<Int>()
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val sounds: Map<SoundCue, Int>
    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) }
        sounds = mapOf(
            SoundCue.SELECT to R.raw.ui_select, SoundCue.FIRE to R.raw.fire,
            SoundCue.MISS to R.raw.splash, SoundCue.HIT to R.raw.impact,
            SoundCue.SUNK to R.raw.sunk, SoundCue.VICTORY to R.raw.victory, SoundCue.DEFEAT to R.raw.defeat,
        ).mapValues { pool.load(context, it.value, 1) }
    }
    fun play(cue: SoundCue) {
        val id = sounds[cue] ?: return
        if (id in loaded) pool.play(id, .32f, .32f, 1, 0, 1f)
    }
    fun pause() = pool.autoPause()
    override fun close() = pool.release()
}
