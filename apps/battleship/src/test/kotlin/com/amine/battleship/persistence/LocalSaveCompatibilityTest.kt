package com.amine.battleship.persistence

import com.amine.battleship.game.engine.GameEngine
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalSaveCompatibilityTest {
    @get:Rule val folder = TemporaryFolder()
    private val json = Json { encodeDefaults = true }
    private fun fixture(schema: Int, mode: String?, left: String = "fox", right: String = "owl", name: String? = null): java.io.File {
        val encoded = json.encodeToJsonElement(SavedSession(game = GameEngine.newGame(42))).jsonObject
        val game = encoded.getValue("game").jsonObject.toMutableMap()
        if (mode != null) game["mode"] = JsonPrimitive(mode)
        game["leftProfile"] = buildJsonObject { put("portraitId", left); if (name != null) put("customName", name) }
        game["rightProfile"] = buildJsonObject { put("portraitId", right) }
        return folder.newFile().also { it.writeText(JsonObject(encoded + mapOf("schemaVersion" to JsonPrimitive(schema), "game" to JsonObject(game))).toString()) }
    }
    private fun property(value: Any, name: String): Any? {
        val method = value.javaClass.methods.firstOrNull { it.name == "get${name.replaceFirstChar(Char::uppercaseChar)}" }
        assertNotNull("Missing metadata $name", method); return method!!.invoke(value)
    }
    @Test fun schemaOneRemainsComputerModeWithoutAReplayablePreview() {
        val file = folder.newFile(); file.writeText(json.encodeToString(SavedSession(game = GameEngine.newGame(42))).replace("\"schemaVersion\":2", "\"schemaVersion\":1"))
        val loaded = GameSaveRepository(file).load()!!
        assertEquals("COMPUTER", property(loaded.game!!, "mode").toString())
        assertEquals(true, property(loaded.game, "previewConsumed"))
    }
    @Test fun schemaTwoRoundTripsLocalModeNamesAndBothBoards() {
        val repository = GameSaveRepository(fixture(2, "LOCAL", name = "Ada")); val loaded = repository.load()
        assertNotNull("Schema 2 must preserve local games", loaded)
        val match = requireNotNull(loaded).game!!
        assertEquals("LOCAL", property(match, "mode").toString())
        assertEquals("Ada", property(property(match, "leftProfile")!!, "customName"))
        repository.save(loaded); assertEquals(loaded, repository.load()); assertEquals(GameEngine.newGame(42).playerBoard, match.playerBoard)
    }
    @Test fun unknownModeUsesExplicitInvalidSaveRecovery() {
        val repository = GameSaveRepository(fixture(2, "UNKNOWN")); assertNull(repository.load()); assertTrue(repository.recoveredInvalidSave)
    }
    @Test fun unknownOrDuplicatePortraitsGetDistinctDeterministicFallbacks() {
        for ((left, right) in listOf("unknown" to "unknown", "fox" to "fox")) {
            val repository = GameSaveRepository(fixture(2, "LOCAL", left, right)); val loaded = repository.load()
            assertNotNull("Bad portrait metadata must not discard the board", loaded)
            val match = requireNotNull(loaded).game!!
            val first = property(property(match, "leftProfile")!!, "portraitId")
            val second = property(property(match, "rightProfile")!!, "portraitId")
            assertEquals("fox", first); assertEquals("owl", second)
        }
    }
    @Test fun invalidSavedNameFallsBackWithoutLosingBoardOrMode() {
        val repository = GameSaveRepository(fixture(2, "LOCAL", name = "bad\nname")); val loaded = repository.load()
        assertNotNull("Invalid name must not erase a valid match", loaded)
        val match = requireNotNull(loaded).game!!
        assertNull(property(property(match, "leftProfile")!!, "customName"))
        assertEquals(GameEngine.newGame(42).computerBoard, match.computerBoard)
    }
}
