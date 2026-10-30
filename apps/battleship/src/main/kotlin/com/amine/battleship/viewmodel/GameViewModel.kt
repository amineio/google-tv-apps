package com.amine.battleship.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amine.battleship.persistence.GameSaveRepository
import java.io.File

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val session = GameSession(viewModelScope,
        GameSaveRepository(File(application.filesDir, "battleship-session.json")))
    val state = session.state
    val sounds = session.sounds
    fun action(action: GameAction) = session.action(action)
    fun background() = session.background()
    fun focusLost() = session.focusLost()
}
