package com.amine.battleship.persistence

import com.amine.battleship.game.engine.GameEngine
import com.amine.battleship.game.model.Coordinate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GameSaveRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun oldSaveDefaultsToEnglishAndLanguageRoundTrips() {
        val file = folder.newFile()
        file.writeText("{\"schemaVersion\":1,\"soundEnabled\":false}")
        val repository = GameSaveRepository(file)
        val old = repository.load()!!
        assertEquals("ENGLISH", old.language.name)
        assertFalse(old.soundEnabled)
        com.amine.battleship.localization.AppLanguage.entries.forEach { language ->
            val value = old.copy(language = language, game = GameEngine.newGame(42))
            repository.save(value)
            assertEquals(value, repository.load())
        }
    }

    @Test fun fullMatchAndPreferencesRoundTrip() = runTest {
        val repository = GameSaveRepository(folder.newFile())
        val value = SavedSession(game = GameEngine.newGame(42), selected = Coordinate(9, 7), soundEnabled = false, showHint = false)
        repository.save(value)
        assertEquals(value, repository.load())
    }

    @Test fun corruptAndUnknownSavesReturnNoMatch() = runTest {
        val file = folder.newFile()
        file.writeText("{broken")
        assertNull(GameSaveRepository(file).load())
        file.writeText("{\"schemaVersion\":99}")
        assertNull(GameSaveRepository(file).load())
    }

    @Test fun invalidSelectedCoordinateRejectsTheSave() = runTest {
        val repository = GameSaveRepository(folder.newFile())
        repository.save(SavedSession(game = GameEngine.newGame(3), selected = Coordinate(20, -1)))
        assertNull(repository.load())
    }
}
