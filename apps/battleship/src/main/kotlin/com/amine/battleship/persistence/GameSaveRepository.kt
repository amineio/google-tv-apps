package com.amine.battleship.persistence

import com.amine.battleship.localization.AppLanguage
import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.Coordinate
import com.amine.battleship.game.model.GameState
import com.amine.battleship.game.model.GameMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@Serializable
data class SavedSession(
    val schemaVersion: Int = 2,
    val game: GameState? = null,
    val selected: Coordinate = Coordinate(0, 0),
    val soundEnabled: Boolean = true,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val showHint: Boolean = true,
)

class GameSaveRepository(private val file: File) {
    private val json = Json { encodeDefaults = true }
    var recoveredInvalidSave: Boolean = false
        private set

    fun load(): SavedSession? {
        if (!file.exists() || file.length() == 0L) return null
        return try {
            require(file.length() <= 1_048_576)
            json.decodeFromString<SavedSession>(file.readText()).let {
                require(it.schemaVersion in 1..2 && it.selected.isValid)
                require(it.game == null || GameEngine.isValid(it.game))
                val match = it.game?.let { game ->
                    val left = game.leftProfile.restored()
                    game.copy(mode = if (it.schemaVersion == 1) GameMode.COMPUTER else game.mode,
                        leftProfile = left, rightProfile = game.rightProfile.restored(setOf(left.portraitId)), previewConsumed = true)
                }
                it.copy(game = match)
            }
        } catch (_: Exception) {
            recoveredInvalidSave = true
            null
        }
    }

    fun save(value: SavedSession) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.pending")
        FileOutputStream(temporary).use { output ->
            output.write(json.encodeToString(value).toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
}
