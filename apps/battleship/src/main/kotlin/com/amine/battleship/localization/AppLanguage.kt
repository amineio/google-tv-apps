package com.amine.battleship.localization

import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"), DUTCH("nl", "Nederlands"), FRENCH("fr", "Français");
    fun next(): AppLanguage = entries[(ordinal + 1) % entries.size]
}
