package com.amine.battleship.game.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameMode { COMPUTER, LOCAL }

/** Stable app-owned identity. A null custom name follows the selected language. */
@Serializable
data class PlayerProfile(val portraitId: String = "fox", val customName: String? = null) {
    companion object {
        val portraitIds = listOf("fox", "owl", "bear", "cat", "storm", "reed", "kai", "mira")
        fun normalizeName(value: String): String? {
            require(value.codePoints().noneMatch { Character.isISOControl(it) })
            val trimmed = value.trim()
            require(trimmed.codePointCount(0, trimmed.length) <= 24)
            return trimmed.ifEmpty { null }
        }
    }
    fun restored(excluded: Set<String> = emptySet()): PlayerProfile {
        val id = portraitId.takeIf { it in portraitIds && it !in excluded }
            ?: portraitIds.first { it !in excluded }
        val name = customName?.let { runCatching { normalizeName(it) }.getOrNull() }
        return PlayerProfile(id, name)
    }
}
