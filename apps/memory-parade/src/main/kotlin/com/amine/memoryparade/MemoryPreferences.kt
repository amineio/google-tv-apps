package com.amine.memoryparade

import android.content.Context

/** Small local record store. The original six-pair score belongs only to Classic. */
class MemoryPreferences(context: Context, name: String = "memory_parade") {
    private val values = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    var sound: Boolean
        get() = values.getBoolean("sound", true)
        set(value) { values.edit().putBoolean("sound", value).apply() }
    var board: BoardSize
        get() = BoardSize.entries.firstOrNull { it.key == values.getString("last_board", null) } ?: BoardSize.CLASSIC
        set(value) { values.edit().putString("last_board", value.key).apply() }
    fun best(size: BoardSize): Int {
        val key = "best_${size.key}"
        if (!values.contains(key) && size == BoardSize.CLASSIC) {
            val legacy = values.getInt("best_moves", 0).takeIf { it >= size.pairs } ?: 0
            values.edit().putInt(key, legacy).apply()
        }
        return values.getInt(key, 0).takeIf { it >= size.pairs } ?: 0
    }
    fun record(size: BoardSize, moves: Int): Int {
        val next = GameRules.bestScore(best(size), moves, size)
        values.edit().putInt("best_${size.key}", next).apply()
        return next
    }
}
