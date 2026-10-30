package com.amine.treasureorretreat

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.tv.material3.MaterialTheme

class TreasureModel : ViewModel() {
    var game by mutableStateOf(GameRules.newGame())
    var screen by mutableStateOf("welcome")
    var paused by mutableStateOf(false)
    var active by mutableStateOf(true)
    var selected by mutableIntStateOf(0)
    var menuSelected by mutableIntStateOf(0)
    var sound by mutableStateOf(true)
    var best by mutableIntStateOf(0)
    var discoverySoundEvent = -1
    var finalCelebrated = false
    fun start() { game = GameRules.newGame(); screen = "game"; paused = false; selected = 0; menuSelected = 0; discoverySoundEvent = -1; finalCelebrated = false }
}

class MainActivity : ComponentActivity() {
    private val model: TreasureModel by viewModels()
    private lateinit var audio: GameAudio
    private lateinit var preferences: LocalPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        preferences = LocalPreferences(this)
        audio = GameAudio(this)
        model.sound = preferences.sound
        model.best = preferences.best
        setContent {
            MaterialTheme {
                BackHandler {
                    if (model.screen == "welcome" || model.game.phase == Phase.FINISHED) finish()
                    else { model.paused = !model.paused; model.menuSelected = 0; audio.pause() }
                }
                LaunchedEffect(model.game.phase, model.paused, model.active) {
                    if (model.game.phase == Phase.REVEALING && !model.paused && model.active) {
                        kotlinx.coroutines.delay(180)
                        if (model.game.outcome != Outcome.BANKED && model.discoverySoundEvent != model.game.event) {
                            model.discoverySoundEvent = model.game.event
                            audio.enabled = model.sound
                            audio.play(when (model.game.lastCard) {
                                Card.TRAP -> "trap"
                                Card.GOLD -> "coin"
                                else -> "jewel"
                            })
                        }
                        kotlinx.coroutines.delay(if (model.game.outcome == Outcome.TRAPPED) 920 else 670)
                        model.game = GameRules.settle(model.game)
                    }
                }
                LaunchedEffect(model.game.phase) {
                    if (model.game.phase == Phase.FINISHED) {
                        model.best = preferences.record(model.game.banked)
                        if (!model.finalCelebrated) {
                            model.finalCelebrated = true
                            audio.play("finale")
                        }
                    }
                }
                TreasureScreen(model, ::handleKey)
            }
        }
    }
    private fun handleKey(event: KeyEvent): Boolean {
        val code = event.keyCode
        if (code !in listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER)) return false
        if (event.action != KeyEvent.ACTION_DOWN) return true
        val select = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_NUMPAD_ENTER
        if (select && event.repeatCount > 0) return true
        audio.enabled = model.sound
        val delta = when (code) {
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN -> 1
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_UP -> -1
            else -> 0
        }
        if (model.paused) {
            if (!select) { model.menuSelected = (model.menuSelected + delta).coerceIn(0, 3); audio.play("navigate") }
            else when (model.menuSelected) {
                0 -> { model.paused = false; audio.resume(); audio.play("confirm") }
                1 -> { model.start(); audio.play("confirm") }
                2 -> { model.sound = !model.sound; preferences.sound = model.sound; audio.enabled = model.sound; audio.play("confirm") }
                3 -> finish()
            }
            return true
        }
        if (model.screen == "welcome") {
            if (select) { model.start(); audio.play("chest") }
            return true
        }
        when (model.game.phase) {
            Phase.READY -> {
                if (!select) { model.selected = (model.selected + delta).coerceIn(0, 1); audio.play("navigate") }
                else {
                    model.game = if (model.selected == 0) GameRules.explore(model.game) else GameRules.bank(model.game)
                    audio.play(if (model.game.outcome == Outcome.BANKED) "bank" else "chest")
                }
            }
            Phase.REVEALING -> Unit
            Phase.RESULT -> if (select) { model.game = GameRules.advance(model.game); model.selected = 0; audio.play("expedition") }
            Phase.FINISHED -> {
                if (!select) model.selected = (model.selected + delta).coerceIn(0, 1)
                else if (model.selected == 0) { model.start(); audio.play("confirm") } else finish()
            }
        }
        return true
    }
    override fun onPause() {
        model.active = false
        if (model.screen == "game" && model.game.phase != Phase.FINISHED) { model.paused = true; model.menuSelected = 0 }
        audio.pause()
        super.onPause()
    }
    override fun onResume() { super.onResume(); model.active = true; if (::audio.isInitialized) audio.resume() }
    override fun onDestroy() { audio.release(); super.onDestroy() }
}
