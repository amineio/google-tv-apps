package com.amine.memoryparade

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.SystemClock

/** Independent short-effect channels keep navigation from interrupting a reward. */
class GameAudio(context: Context) {
    private enum class Channel { UI, CARD, REWARD, ARRIVAL }
    private data class Cue(val resources: List<Int>, val channel: Channel, val gain: Float, val priority: Int)

    private val lock = Any()
    private val pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    private val loaded = mutableSetOf<Int>()
    private val streams = mutableMapOf<Channel, Int>()
    private val nextVariant = mutableMapOf<String, Int>()
    private var released = false
    private var lastNavigation = -80L
    private var finaleUntil = 0L
    private val cues = mapOf(
        "navigate" to Cue(listOf(R.raw.navigate, R.raw.navigate_2), Channel.UI, .30f, 1),
        "flip" to Cue(listOf(R.raw.flip, R.raw.flip_2, R.raw.flip_3), Channel.CARD, .50f, 2),
        "match" to Cue(listOf(R.raw.match, R.raw.match_2, R.raw.match_3), Channel.REWARD, .58f, 3),
        "mismatch" to Cue(listOf(R.raw.mismatch), Channel.REWARD, .42f, 2),
        "arrival" to Cue(listOf(R.raw.arrival), Channel.ARRIVAL, .46f, 2),
        "complete" to Cue(listOf(R.raw.complete), Channel.REWARD, .65f, 4),
        "confirm" to Cue(listOf(R.raw.confirm), Channel.UI, .50f, 2),
    )
    private val sounds: Map<String, List<Int>>

    init {
        // Readiness must be observed before the first asynchronous load begins.
        pool.setOnLoadCompleteListener { _, id, status ->
            synchronized(lock) { if (!released && status == 0) loaded.add(id) }
        }
        sounds = cues.mapValues { (_, cue) -> cue.resources.map { pool.load(context, it, 1) } }
    }

    fun play(name: String) = synchronized(lock) {
        if (released) return@synchronized
        val cue = cues[name] ?: return@synchronized
        val now = SystemClock.uptimeMillis()
        if (now < finaleUntil) return@synchronized
        if (name == "navigate" && now - lastNavigation < 80L) return@synchronized
        val variants = sounds.getValue(name)
        val start = nextVariant[name] ?: 0
        val choice = variants.indices.map { (start + it) % variants.size }
            .firstOrNull { variants[it] in loaded } ?: return@synchronized
        nextVariant[name] = (choice + 1) % variants.size
        if (name == "navigate") lastNavigation = now
        if (name == "complete") {
            stopLocked()
            finaleUntil = now + 1350L
        } else streams.remove(cue.channel)?.let(pool::stop)
        val stream = pool.play(variants[choice], cue.gain, cue.gain, cue.priority, 0, 1f)
        if (stream != 0) streams[cue.channel] = stream
    }

    fun stop() = synchronized(lock) { if (!released) stopLocked() }

    private fun stopLocked() {
        streams.values.forEach(pool::stop)
        streams.clear()
        finaleUntil = 0L
    }

    fun release() = synchronized(lock) {
        if (released) return@synchronized
        stopLocked()
        released = true
        pool.setOnLoadCompleteListener(null)
        pool.release()
        loaded.clear()
    }
}
