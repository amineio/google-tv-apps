package com.amine.battleship.game

import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.InvocationTargetException

class PlayerProfileTest {
    private fun normalized(value: String): String? {
        val type = runCatching { Class.forName("com.amine.battleship.game.model.PlayerProfile") }.getOrNull()
        assertNotNull("Missing stable PlayerProfile metadata and Unicode name validation", type)
        val companion = type!!.getField("Companion").get(null)
        return companion.javaClass.getMethod("normalizeName", String::class.java).invoke(companion, value) as String?
    }
    @Test fun emptyNameUsesTheLocalizedCharacterDefault() { assertNull(normalized("   ")); assertEquals("Captain", normalized("  Captain  ")) }
    @Test fun twentyFourUnicodeCodePointsFitWithoutSplittingSurrogates() { val name = "🚢".repeat(24); assertEquals(name, normalized(name)) }
    @Test fun namesLongerThanTwentyFourCodePointsAreRejected() { rejects("a".repeat(25)) }
    @Test fun controlCharactersAreRejectedRatherThanSaved() { for (value in listOf("a\nb", "a\tb", "a\u0000b")) rejects(value) }
    private fun rejects(value: String) {
        try { normalized(value); fail("Invalid custom name accepted") }
        catch (error: InvocationTargetException) { assertTrue(error.cause is IllegalArgumentException) }
    }
}
