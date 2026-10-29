package com.amine.battleship

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.amine.battleship.audio.GameAudio
import com.amine.battleship.ui.BattleshipApp
import com.amine.battleship.viewmodel.GameViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    val game: GameViewModel by viewModels()
    private lateinit var audio: GameAudio
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        audio = GameAudio(this)
        lifecycleScope.launch {
            game.sounds.collect { audio.play(it, game.state.value) }
        }
        lifecycleScope.launch {
            game.state.collect { audio.update(it) }
        }
        setContent {
            val state = game.state.collectAsStateWithLifecycle().value
            BattleshipApp(state, game::action, ::finish)
        }
    }
    override fun onResume() {
        super.onResume()
        audio.foreground(true, game.state.value)
    }
    override fun onPause() {
        audio.foreground(false, game.state.value)
        game.background()
        super.onPause()
    }
    override fun onDestroy() {
        audio.close()
        super.onDestroy()
    }
}
