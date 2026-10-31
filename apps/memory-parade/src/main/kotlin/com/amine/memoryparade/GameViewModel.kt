package com.amine.memoryparade

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen { WELCOME, GAME, PAUSE, RESULTS }
class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = MemoryPreferences(application)
    var chosenBoard by mutableStateOf(preferences.board); private set
    var records by mutableStateOf(BoardSize.entries.associateWith(preferences::best)); private set
    var game by mutableStateOf(GameRules.newGame(size = chosenBoard)); private set
    var screen by mutableStateOf(Screen.WELCOME); private set
    var selected by mutableIntStateOf(0); private set
    var menuIndex by mutableIntStateOf(chosenBoard.ordinal); private set
    val best get() = bestFor(game.size)
    fun bestFor(size: BoardSize) = records[size] ?: 0
    var sound by mutableStateOf(preferences.sound); private set
    var event by mutableIntStateOf(0); private set
    var eventKind by mutableStateOf("confirm"); private set
    private var consumedAudioEvent = 0
    fun consumeAudioEvent(): String? {
        if (event <= consumedAudioEvent) return null
        consumedAudioEvent = event
        return eventKind.takeIf { active && sound }
    }
    private var resolution: Job? = null
    private var lockedUntil = 0L
    var active by mutableStateOf(true); private set
    private fun emit(kind: String) { eventKind = kind; event++ }
    fun start(size: BoardSize = chosenBoard) {
        resolution?.cancel(); chosenBoard = size; preferences.board = size
        game = GameRules.newGame(size = size); selected = 0; menuIndex = 0
        screen = Screen.GAME; lockedUntil = 0; emit("confirm")
    }
    fun chooseBoard() {
        resolution?.cancel(); screen = Screen.WELCOME; menuIndex = chosenBoard.ordinal; emit("confirm")
    }
    fun direction(direction: Direction) {
        if (!active) return
        if (screen == Screen.GAME) {
            val next = GameRules.move(selected, direction, game.size)
            if (next != selected) { selected = next; emit("navigate") }
        } else {
            val count = when (screen) { Screen.WELCOME -> 3; Screen.PAUSE -> 5; Screen.RESULTS -> 3; else -> 1 }
            val step = if (direction == Direction.UP || direction == Direction.LEFT) -1 else 1
            val next = (menuIndex + step).coerceIn(0, count - 1)
            if (next != menuIndex) { menuIndex = next; emit("navigate") }
        }
    }
    fun select(exit: () -> Unit) {
        if (!active) return
        when (screen) {
            Screen.WELCOME -> start(BoardSize.entries[menuIndex])
            Screen.RESULTS -> when (menuIndex) { 0 -> start(); 1 -> chooseBoard(); else -> exit() }
            Screen.PAUSE -> when (menuIndex) {
                0 -> resume()
                1 -> start()
                2 -> { sound = !sound; preferences.sound = sound; emit("confirm") }
                3 -> chooseBoard()
                4 -> exit()
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
                val record = preferences.record(game.size, game.moves)
                records = records + (game.size to record)
                emit("complete")
                delay(2200); screen = Screen.RESULTS; menuIndex = 0
            } else if (matched) emit("arrival")
        }
    }
    fun back(exit: () -> Unit) {
        when (screen) {
            Screen.WELCOME -> exit()
            Screen.RESULTS -> chooseBoard()
            Screen.GAME -> pause()
            Screen.PAUSE -> resume()
        }
    }
    fun pause() { if (screen == Screen.GAME) { consumedAudioEvent = event; resolution?.cancel(); screen = Screen.PAUSE; menuIndex = 0 } }
    private fun resume() { screen = Screen.GAME; emit("confirm"); if (game.comparing) scheduleResolution() else if (game.completed) { screen = Screen.RESULTS; menuIndex = 0 } }
    fun onActivityActive(value: Boolean) { active = value; if (!value) { consumedAudioEvent = event; pause() } }
}
