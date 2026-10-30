package com.amine.treasureorretreat

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class GameAudio(context: Context) {
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val loaded = mutableSetOf<Int>()
    private val sounds: Map<String, Int>
    var enabled = true
    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(loaded) { loaded.add(id) } }
        sounds = mapOf(
            "navigate" to R.raw.navigate, "confirm" to R.raw.confirm, "chest" to R.raw.chest_open,
            "coin" to R.raw.coin, "jewel" to R.raw.jewel, "bank" to R.raw.bank,
            "trap" to R.raw.trap, "expedition" to R.raw.expedition, "finale" to R.raw.finale
        ).mapValues { pool.load(context, it.value, 1) }
    }
    fun play(name: String) {
        if (!enabled) return
        val id = sounds[name] ?: return
        if (synchronized(loaded) { id in loaded }) pool.play(id, .7f, .7f, 1, 0, 1f)
    }
    fun pause() = pool.autoPause()
    fun resume() = pool.autoResume()
    fun release() = pool.release()
}

class LocalPreferences(context: Context, name: String = "treasure_records") {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    var sound: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) { prefs.edit().putBoolean("sound", value).apply() }
    val best: Int get() = prefs.getInt("best", 0)
    fun record(score: Int): Int {
        val next = maxOf(best, score)
        prefs.edit().putInt("best", next).apply()
        return next
    }
}
