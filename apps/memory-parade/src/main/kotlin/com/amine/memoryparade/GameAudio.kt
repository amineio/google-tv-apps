package com.amine.memoryparade

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/** Short local effects. The app has no continuous music or network audio. */
class GameAudio(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val loaded = mutableSetOf<Int>()
    private val sounds = mapOf("navigate" to R.raw.navigate, "flip" to R.raw.flip, "match" to R.raw.match,
        "mismatch" to R.raw.mismatch, "arrival" to R.raw.arrival, "complete" to R.raw.complete, "confirm" to R.raw.confirm)
        .mapValues { pool.load(context, it.value, 1) }
    private val streams = ArrayDeque<Int>()
    init { pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) loaded.add(id) } }
    fun play(name: String) {
        val id = sounds[name] ?: return
        if (id !in loaded) return
        while (streams.size >= 3) pool.stop(streams.removeFirst())
        streams.addLast(pool.play(id, .6f, .6f, 1, 0, 1f))
    }
    fun stop() { streams.forEach(pool::stop); streams.clear() }
    fun release() { stop(); pool.release() }
}
