package com.amine.memoryparade

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen { WELCOME, GAME, PAUSE, RESULTS }
class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("memory_parade", 0)
    var game by mutableStateOf(GameRules.newGame()); private set
    var screen by mutableStateOf(Screen.WELCOME); private set
    var selected by mutableStateOf(0); private set
    var menuIndex by mutableStateOf(0); private set
    var best by mutableStateOf(preferences.getInt("best_moves", 0)); private set
    var sound by mutableStateOf(preferences.getBoolean("sound", true)); private set
    var event by mutableStateOf(0); private set
    var eventKind by mutableStateOf("confirm"); private set
    private var resolution: Job? = null
    private var lockedUntil = 0L
    var active by mutableStateOf(true); private set
    private fun emit(kind: String) { eventKind = kind; event++ }
    fun start() {
        resolution?.cancel(); game = GameRules.newGame(); selected = 0; menuIndex = 0
        screen = Screen.GAME; lockedUntil = 0; emit("confirm")
    }
    fun direction(direction: Direction) {
        if (!active) return
        if (screen == Screen.GAME) {
            val next = GameRules.move(selected, direction)
            if (next != selected) { selected = next; emit("navigate") }
        } else {
            val count = if (screen == Screen.PAUSE) 4 else if (screen == Screen.RESULTS) 2 else 1
            val step = if (direction == Direction.UP || direction == Direction.LEFT) -1 else 1
            val next = (menuIndex + step).coerceIn(0, count - 1)
            if (next != menuIndex) { menuIndex = next; emit("navigate") }
        }
    }
    fun select(exit: () -> Unit) {
        if (!active) return
        when (screen) {
            Screen.WELCOME -> start()
            Screen.RESULTS -> if (menuIndex == 0) start() else exit()
            Screen.PAUSE -> when (menuIndex) {
                0 -> resume()
                1 -> start()
                2 -> { sound = !sound; preferences.edit().putBoolean("sound", sound).apply(); emit("confirm") }
                3 -> exit()
            }
            Screen.GAME -> {
                if (android.os.SystemClock.uptimeMillis() < lockedUntil) return
                val next = GameRules.flip(game, selected)
                if (next == game) return
                game = next; lockedUntil = android.os.SystemClock.uptimeMillis() + 330
                emit("flip")
                if (game.comparing) scheduleResolution()
            }
        }
    }
    private fun scheduleResolution() {
        resolution?.cancel()
        resolution = viewModelScope.launch {
            val matched = game.isMatch
            delay(380)
            emit(if (matched) "match" else "mismatch")
            delay(if (matched) 480 else 520)
            game = GameRules.resolve(game)
            if (game.completed) {
                best = GameRules.bestScore(best, game.moves)
                preferences.edit().putInt("best_moves", best).apply()
                emit("complete")
                delay(1200); screen = Screen.RESULTS; menuIndex = 0
            } else if (matched) emit("arrival")
        }
    }
    fun back(exit: () -> Unit) {
        when (screen) {
            Screen.WELCOME, Screen.RESULTS -> exit()
            Screen.GAME -> pause()
            Screen.PAUSE -> resume()
        }
    }
    fun pause() { if (screen == Screen.GAME) { resolution?.cancel(); screen = Screen.PAUSE; menuIndex = 0 } }
    private fun resume() { screen = Screen.GAME; emit("confirm"); if (game.comparing) scheduleResolution() else if (game.completed) screen = Screen.RESULTS }
    fun onActivityActive(value: Boolean) {
        active = value
        if (!value) pause()
    }
}
