package com.amine.memoryparade

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class BoardSizeRulesTest {
    @Test fun everyBoardHasItsExactPairDistribution() {
        for (size in BoardSize.entries) repeat(40) { seed ->
            val state = GameRules.newGame(Random(seed), size)
            assertEquals(size.cards, state.cards.size)
            assertEquals(size.pairs, state.cards.toSet().size)
            assertTrue(state.cards.groupingBy { it }.eachCount().values.all { it == 2 })
            assertEquals(size, state.size)
        }
    }
    @Test fun smallerBoardsChooseFreshSubsetsAndSeedsAreReproducible() {
        assertEquals(8, Animal.entries.size)
        for (size in BoardSize.entries) {
            assertEquals(GameRules.newGame(Random(8), size), GameRules.newGame(Random(8), size))
            val subsets = (0..30).map { GameRules.newGame(Random(it), size).cards.toSet() }.toSet()
            if (size != BoardSize.CHALLENGE) assertTrue(subsets.size > 1)
        }
    }
    @Test fun navigationReachesEveryPositionAndClampsEdges() {
        for (size in BoardSize.entries) {
            assertEquals(0, GameRules.move(0, Direction.UP, size))
            assertEquals(0, GameRules.move(0, Direction.LEFT, size))
            assertEquals(size.cards - 1, GameRules.move(size.cards - 1, Direction.RIGHT, size))
            assertEquals(size.cards - 1, GameRules.move(size.cards - 1, Direction.DOWN, size))
            for (i in 0 until size.cards) {
                var position = 0
                repeat(i / size.columns) { position = GameRules.move(position, Direction.DOWN, size) }
                repeat(i % size.columns) { position = GameRules.move(position, Direction.RIGHT, size) }
                assertEquals(i, position)
            }
        }
    }
    @Test fun allBoardsCompleteOnlyAfterAllPairsAndRejectDuplicateScoring() {
        for (size in BoardSize.entries) {
            var state = GameRules.newGame(Random(12), size)
            state.cards.distinct().forEachIndexed { count, animal ->
                val pair = state.cards.indices.filter { state.cards[it] == animal }
                state = GameRules.flip(GameRules.flip(state, pair[0]), pair[1])
                state = GameRules.resolve(state)
                assertEquals(count + 1, state.moves)
                assertEquals(count + 1, state.pairs)
                assertEquals(count + 1 == size.pairs, state.completed)
                assertEquals(state, GameRules.flip(state, pair[0]))
                assertEquals(state, GameRules.resolve(state))
            }
        }
    }
    @Test fun minimumRecordDependsOnBoard() {
        for (size in BoardSize.entries) {
            assertEquals(size.pairs, GameRules.bestScore(0, size.pairs, size))
            assertEquals(0, GameRules.bestScore(0, size.pairs - 1, size))
            assertEquals(size.pairs, GameRules.bestScore(size.pairs, size.pairs + 3, size))
        }
    }
}
