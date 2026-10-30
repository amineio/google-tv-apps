package com.amine.memoryparade

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameRulesTest {
    @Test fun boardContainsExactlySixPairs() {
        repeat(30) { seed ->
            val state = GameRules.newGame(Random(seed))
            assertEquals(12, state.cards.size)
            assertEquals(Animal.entries.toSet(), state.cards.toSet())
            state.cards.groupingBy { it }.eachCount().values.forEach { assertEquals(2, it) }
        }
        assertNotEquals(GameRules.newGame(Random(1)).cards, GameRules.newGame(Random(2)).cards)
    }
    @Test fun sameCardAndThirdFlipDoNotCount() {
        var s = GameRules.newGame(Random(3))
        s = GameRules.flip(s, 0)
        assertEquals(s, GameRules.flip(s, 0))
        assertEquals(0, s.moves)
        val other = s.cards.indices.first { s.cards[it] != s.cards[0] }
        s = GameRules.flip(s, other)
        assertEquals(1, s.moves)
        assertEquals(s, GameRules.flip(s, (other + 1) % 12))
        val resolved = GameRules.resolve(s)
        assertTrue(resolved.visible.isEmpty())
        assertEquals(0, resolved.pairs)
    }
    @Test fun matchCountsOnceAndCompletionRequiresSix() {
        var s = GameRules.newGame(Random(4))
        Animal.entries.forEachIndexed { number, animal ->
            val pair = s.cards.indices.filter { s.cards[it] == animal }
            s = GameRules.flip(s, pair[0]); s = GameRules.flip(s, pair[1])
            s = GameRules.resolve(s)
            assertEquals(number + 1, s.pairs)
            assertEquals(number + 1, s.moves)
            assertEquals(s, GameRules.flip(s, pair[0]))
            assertEquals(number == 5, s.completed)
        }
        assertEquals(6, s.parade.distinct().size)
        assertEquals(s, GameRules.resolve(s))
    }
    @Test fun navigationClampsAtGridEdges() {
        assertEquals(0, GameRules.move(0, Direction.UP))
        assertEquals(0, GameRules.move(0, Direction.LEFT))
        assertEquals(11, GameRules.move(11, Direction.DOWN))
        assertEquals(11, GameRules.move(11, Direction.RIGHT))
        assertEquals(7, GameRules.move(3, Direction.DOWN))
        assertEquals(2, GameRules.move(3, Direction.LEFT))
    }
    @Test fun invalidPositionsAreIgnored() {
        val s = GameRules.newGame(Random(4))
        assertEquals(s, GameRules.flip(s, -1))
        assertEquals(s, GameRules.flip(s, 12))
    }
    @Test fun bestRecordIsLowestCompletedMoveCount() {
        assertEquals(10, GameRules.bestScore(0, 10))
        assertEquals(8, GameRules.bestScore(10, 8))
        assertEquals(8, GameRules.bestScore(8, 12))
        assertEquals(0, GameRules.bestScore(0, 0))
    }
}
