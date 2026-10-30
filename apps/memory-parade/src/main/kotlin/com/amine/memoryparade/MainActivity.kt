package com.amine.memoryparade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    val gameModel: GameViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { MemoryApp(gameModel, ::finish) }
    }
    override fun onResume() { super.onResume(); gameModel.onActivityActive(true) }
    override fun onPause() { gameModel.onActivityActive(false); super.onPause() }
}
