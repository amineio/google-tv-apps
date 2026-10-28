package com.amine.battleship

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
            repeatOnLifecycle(Lifecycle.State.STARTED) { game.sounds.collect { audio.play(it) } }
        }
        setContent {
            val state = game.state.collectAsStateWithLifecycle().value
            BattleshipApp(state, game::action, ::finish)
        }
    }
    override fun onPause() {
        game.background()
        audio.pause()
        super.onPause()
    }
    override fun onDestroy() {
        audio.close()
        super.onDestroy()
    }
}
